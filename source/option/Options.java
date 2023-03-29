package option;

import net.auoeke.reflect.*;

import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.auoeke.reflect.Classes.cast;

public class Options {

	public static <T extends Record> Result<T> parse(Class<T> type, String... line) {
		if (!type.isRecord()) throw new IllegalArgumentException("%s is not a record type".formatted(type));

		Methods.of(type).forEach(method -> {
			if (method.isAnnotationPresent(Parse.class)) {
				Supplier<String> format = () -> "@Parse %s::%s%s".formatted(type.getName(), method.getName(), Methods.type(method));

				if (Flags.isInstance(method)) throw new WrongMethodTypeException(format.get() + " is not static");

				if (!Types.canCast(0L, method.getParameterTypes(), String.class, String.class, List.class)) {
					throw new WrongMethodTypeException("parameters of " + format.get() + " do not match (String, String, List<String>)");
				}

				var field = Fields.of(type, method.getName());

				if (field == null || Flags.isStatic(field)) throw new WrongMethodTypeException(format.get() + " does not match a component");

				if (!field.getType().isAssignableFrom(method.getReturnType())) {
					throw new WrongMethodTypeException("return type of %s does not match %s %s".formatted(format.get(), field.getType().getName(), field.getName()));
				}
			}
		});

		return Result.build(problems -> {
			var all = Stream.of(type.getRecordComponents())
				.map(component -> Fields.of(type, component.getName()))
				.map(field -> {
					var name = field.getAnnotation(Name.class);
					var names = name == null || name.value().length == 0 && name.character() != 0 ? new String[]{field.getName()} : name.value();

					if (names.length == 0 && name.character() == 0) throw new IllegalArgumentException("0 names specified for %s#%s".formatted(type.getName(), field.getName()));

					String ln = null;
					var sn = name == null ? 0 : name.character();

					for (var value : names) {
						switch (value.length()) {
							case 0 -> throw new IllegalArgumentException("empty name specified for %s#%s".formatted(type.getName(), field.getName()));
							case 1 -> {
								if (sn != 0) throw new IllegalArgumentException("%s#%s has more than 1 short name".formatted(type.getName(), field.getName()));
								sn = value.charAt(0);
							}
							default -> {
								if (ln != null) throw new IllegalArgumentException("%s#%s has more than 1 long name".formatted(type.getName(), field.getName()));
								ln = value;
							}
						}
					}

					var parser = Optional.ofNullable(Methods.of(type, field.getName(), String.class, String.class, List.class))
						.filter(method -> method.isAnnotationPresent(Parse.class))
						.map(method -> {
							var handle = Invoker.unreflect(method);
							return (OptionParser<?>) (option, value, problems1) -> Invoker.invoke(handle, option, value, problems1);
						})
						.orElseGet(() -> cast(parser(field, field.getType())));
					return new WorkingOption<>(field, parser, ln, sn, field.getAnnotation(Default.class));
				}).toList();

			var byLong = all.stream().filter(option -> Objects.nonNull(option.name)).collect(Collectors.toMap(o -> o.name, Function.identity()));
			var byShort = all.stream().filter(o -> o.character != 0).collect(Collectors.toMap(o -> o.character, Function.identity()));
			var options = new LinkedHashSet<WorkingOption>();
			var notFound = new LinkedHashSet<String>();
			var arguments = new ArrayList<String>();
			WorkingOption lastOption = null;

			for (var iterator = Arrays.asList(line).iterator(); iterator.hasNext();) {
				var argument = iterator.next();

				if (argument.equals("--")) {
					iterator.forEachRemaining(arguments::add);
				} else if (argument.startsWith("--")) {
					lastOption = byLong.get(argument.substring(2));

					if (lastOption == null) {
						notFound.add(argument);
					} else {
						lastOption.string = argument;
						options.add(lastOption);
					}
				} else if (argument.startsWith("-") && argument.length() > 1) {
					var names = argument.substring(1).toCharArray();
					lastOption = byShort.get(names[names.length - 1]);

					for (var name : names) {
						var option = byShort.get(name);

						if (option == null) {
							notFound.add("-" + name);
						} else {
							option.string = option.formatShort();
							options.add(option);
						}
					}
				} else if (lastOption == null || lastOption.parser == null) {
					arguments.add(argument);
				} else {
					lastOption.value = cast(lastOption.parser.parse(lastOption.string, argument, problems));
					lastOption = null;
				}
			}

			for (var name : notFound) {
				problems.add("option " + name + " does not exist");
			}

			options.forEach(option -> {
				option.set = option.parser == null || option.value != null;

				if (!option.set) {
					problems.add("option " + option.string + " must be followed immediately by an argument but none was found");
				} else if (option.type == boolean.class) {
					option.value = true;
				}
			});

			all.stream()
				.filter(option -> !options.contains(option) && option.fallback != null || option.type.isPrimitive() && option.value == null)
				.forEach(option -> option.fallBack(problems));

			return Invoker.invoke(Invoker.unreflectConstructor(Constructors.canonical(type)), all.stream().map(option -> option.value).toArray());
		});
	}

	private static OptionParser<?> parser(Field field, Class<?> type) {
		return type == String.class ? (option, value, problems) -> value
			: type == boolean.class && field != null && field.isAnnotationPresent(Explicit.class) ? Options::parseBoolean
			: type == byte.class ? Options::parseByte
			: type == char.class ? Options::parseCharacter
			: type == short.class ? Options::parseShort
			: type == int.class ? Options::parseInteger
			: type == long.class ? Options::parseLong
			: type == float.class ? Options::parseFloat
			: type == double.class ? Options::parseDouble
			: type.isArray() ? (option, value, problems) -> parseArray(type.componentType(), option, value, problems)
			: Enum.class.isAssignableFrom(type) ? (option, value, problems) -> parseEnum(cast(type), option, value, problems)
			: null;
	}

	public static Boolean parseBoolean(String option, String value, List<String> problems) {
		if (value.equalsIgnoreCase("true")) return true;
		if (value.equalsIgnoreCase("false")) return false;

		problems.add("invalid boolean value \"%s\" for %s".formatted(value, option));
		return null;
	}

	public static Byte parseByte(String option, String value, List<String> problems) {
		try {
			return Byte.decode(value);
		} catch (NumberFormatException exception) {
			problems.add("invalid byte value \"%s\" for %s".formatted(value, option));
			return null;
		}
	}

	public static Character parseCharacter(String option, String value, List<String> problems) {
		var codePoints = option.chars().limit(2).toArray();

		if (codePoints.length == 2) {
			problems.add("invalid character value \"%s\" for option %s".formatted(value, option));
			return null;
		}

		return (char) codePoints[0];
	}

	public static Short parseShort(String option, String value, List<String> problems) {
		try {
			return Short.decode(value);
		} catch (NumberFormatException exception) {
			problems.add("invalid short value \"%s\" for %s".formatted(value, option));
			return null;
		}
	}

	public static Integer parseInteger(String option, String value, List<String> problems) {
		try {
			return Integer.decode(value);
		} catch (NumberFormatException exception) {
			problems.add("invalid integer value \"%s\" for %s".formatted(value, option));
			return null;
		}
	}

	public static Long parseLong(String option, String value, List<String> problems) {
		try {
			return Long.decode(value);
		} catch (NumberFormatException exception) {
			problems.add("invalid long value \"%s\" for %s".formatted(value, option));
			return null;
		}
	}

	public static Float parseFloat(String option, String value, List<String> problems) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException exception) {
			problems.add("invalid float value \"%s\" for %s".formatted(value, option));
			return null;
		}
	}

	public static Double parseDouble(String option, String value, List<String> problems) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException exception) {
			problems.add("invalid double value \"%s\" for %s".formatted(value, option));
			return null;
		}
	}

	public static <T extends Enum<T>> T parseEnum(Class<T> type, String option, String value, List<String> problems) {
		for (var constant : type.getEnumConstants()) {
			if (constant.name().equalsIgnoreCase(value)) {
				return constant;
			}
		}

		problems.add("invalid enum value \"%s\" for %s".formatted(value, option));
		return null;
	}

	public static Object parseArray(Class<?> type, String option, String value, List<String> problems) {
		var parser = parser(null, type);
		var array = Stream.of(value.split(","))
			.map(element -> parser.parse(option, element, problems))
			.toArray(length -> cast(Array.newInstance(Types.box(type), length)));
		return type.isPrimitive() ? Types.unbox(array) : array;
	}
}
