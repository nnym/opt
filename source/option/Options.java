package option;

import net.auoeke.reflect.*;

import java.io.File;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.auoeke.reflect.Classes.cast;

public class Options {
	private static final @Opt Opt defaultOpt = Fields.of(Options.class, "defaultOpt").getAnnotation(Opt.class);

	public static <T extends Record> Result<T> parse(Class<T> type, String... line) {
		if (!type.isRecord()) throw new IllegalArgumentException("%s is not a record type".formatted(type));

		Methods.of(type).forEach(method -> {
			if (method.isAnnotationPresent(Parse.class)) {
				var reference = lazyString(() -> "@Parse %s::%s%s".formatted(type.getName(), method.getName(), Methods.type(method)));

				if (Flags.isInstance(method)) throw new WrongMethodTypeException(reference + " is not static");

				if (!Types.canCast(0L, method.getParameterTypes(), String.class, String.class, List.class)) {
					throw new WrongMethodTypeException("parameters of " + reference + " do not match (String, String, List<String>)");
				}

				var field = Fields.of(type, method.getName());

				if (field == null || Flags.isStatic(field)) throw new WrongMethodTypeException(reference + " does not match a component");

				if (!field.getType().isAssignableFrom(method.getReturnType())) {
					throw new WrongMethodTypeException("return type of %s does not match %s %s".formatted(reference, field.getType().getName(), field.getName()));
				}
			}
		});

		var problems = new ArrayList<String>();
		var all = Stream.of(type.getRecordComponents())
			.map(component -> Fields.of(type, component.getName()))
			.map(field -> {
				var component = lazyString(() -> "%s#%s".formatted(type.getName(), field.getName()));
				var opt = Objects.requireNonNullElse(field.getAnnotation(Opt.class), defaultOpt);

				if (opt.value().length > 1 && !opt.repeat().aggregate()) {
					throw new IllegalArgumentException("%s: value.length > 1 but repeat == %s != AGGREGATE(_FLAT)?: %s".formatted(component, opt.repeat(), Arrays.toString(opt.value())));
				}

				var names = new ArrayList<>(Arrays.asList(opt.name()));

				if (!names.remove("-") && names.stream().noneMatch(name -> (name.length() == 1) == (field.getName().length() == 1))) {
					names.add(field.getName());
				}

				if (names.isEmpty()) throw new IllegalArgumentException("0 names specified for " + component);

				if (opt.repeat().aggregate() && !field.getType().isArray()) {
					throw new IllegalArgumentException("%s is not an array but repeat == %s".formatted(component, opt.repeat()));
				}

				String ln = null;
				var sn = '\0';

				for (var iterator = names.listIterator(); iterator.hasNext();) {
					var name = iterator.next();

					if (iterator.previousIndex() != names.lastIndexOf(name)) {
						throw new IllegalArgumentException("%s: duplicate name \"%s\"".formatted(component, name));
					}

					switch (name.length()) {
						case 0 -> throw new IllegalArgumentException("empty name specified for " + component);
						case 1 -> {
							if (sn != 0) throw new IllegalArgumentException(component + " has more than 1 short name");
							if (0 == (sn = name.charAt(0))) throw new IllegalArgumentException("illegal short name '\\0' (null)");
						}
						default -> {
							if (ln != null) throw new IllegalArgumentException(component + " has more than 1 long name");
							ln = name;
						}
					}
				}

				var parser = Optional.ofNullable(Methods.of(type, field.getName(), String.class, String.class, List.class))
					.filter(method -> method.isAnnotationPresent(Parse.class))
					.map(method -> {
						var handle = Invoker.unreflect(method);
						return (OptionParser<?>) (option, value, problems1) -> Invoker.invoke(handle, option, value, problems1);
					})
					.orElseGet(() -> cast(parser(field.getType(), opt)));
				return new WorkingOption(field, opt, parser, ln, sn, opt.value());
			}).toList();

		var byLong = all.stream().filter(option -> Objects.nonNull(option.name)).collect(Collectors.toMap(o -> o.name, Function.identity()));
		var byShort = all.stream().filter(o -> o.character != 0).collect(Collectors.toMap(o -> o.character, Function.identity()));
		var options = new LinkedHashSet<WorkingOption>();
		var notFound = new LinkedHashSet<String>();
		var operands = new ArrayList<String>();
		WorkingOption lastOption = null;

		for (var iterator = Arrays.asList(line).iterator(); iterator.hasNext();) {
			var argument = iterator.next();

			if (argument.equals("--")) {
				iterator.forEachRemaining(operands::add);
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
				operands.add(argument);
			} else {
				lastOption.set(argument, problems);
				lastOption = null;
			}
		}

		for (var name : notFound) {
			problems.add("option " + name + " does not exist");
		}

		all.forEach(option -> {
			if (options.contains(option)) {
				option.set = option.parser == null || option.value != null;

				if (!option.set) {
					problems.add("option " + option.string + " must be followed immediately by an argument but none was found");
				} else if (option.type == boolean.class && option.value == null) {
					option.value = true;
				}
			} else if (option.fallback.length == 0) {
				problems.add("option " + option.format() + " is required but not set");
			}
		});

		all.stream()
			.filter(option -> !options.contains(option) && option.fallback.length != 0 || option.type.isPrimitive() && option.value == null)
			.forEach(option -> option.fallBack(problems));

		return new Result<>(Invoker.invoke(Invoker.unreflectConstructor(Constructors.canonical(type)), all.stream().map(option -> option.value).toArray()), Collections.unmodifiableList(problems));
	}

	private static OptionParser<?> parser(Class<?> type, Opt opt) {
		if (opt != null && opt.repeat() == Opt.Repeat.AGGREGATE) {
			type = type.componentType();
		}

		return type == String.class ? (option, value, problems) -> value
			: type == boolean.class && (opt == null || opt.explicit()) ? Options::parseBoolean
			: type == byte.class ? Options::parseByte
			: type == char.class ? Options::parseCharacter
			: type == short.class ? Options::parseShort
			: type == int.class ? Options::parseInteger
			: type == long.class ? Options::parseLong
			: type == float.class ? Options::parseFloat
			: type == double.class ? Options::parseDouble
			: type.isArray() ? (option, value, problems) -> parseArray(type.componentType(), option, value, problems)
			: Enum.class.isAssignableFrom(type) ? (option, value, problems) -> parseEnum(cast(type), option, value, problems)
			: type == File.class ? Options::parseFile
			: type == Path.class ? Options::parsePath
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
		var parser = parser(type, null);
		var array = Stream.of(value.split(","))
			.map(element -> parser.parse(option, element, problems))
			.toArray(length -> cast(Array.newInstance(Types.box(type), length)));
		return type.isPrimitive() ? Types.unbox(array) : array;
	}

	public static File parseFile(String option, String value, List<String> problems) {
		return new File(value);
	}

	public static Path parsePath(String option, String value, List<String> problems) {
		try {
			return Path.of(value);
		} catch (Exception exception) {
			problems.add("invalid path \"%s\" for %s".formatted(value, option));
			return null;
		}
	}

	private static Object lazyString(Supplier<String> supplier) {
		return new Object() {
			@Override public String toString() {
				return supplier.get();
			}
		};
	}
}
