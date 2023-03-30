package option;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

final class WorkingOption<T> extends BaseOption<T> {
	final Field field;
	final Opt opt;
	final OptionParser<T> parser;
	String string;
	boolean set;
	Object value;

	WorkingOption(Field field, Opt opt, OptionParser<?> parser, String name, char character, String[] fallback) {
		super((Class<T>) field.getType(), name, character, fallback);

		this.field = field;
		this.opt = opt;
		this.parser = (OptionParser<T>) parser;
	}

	void set(String argument, List<String> problems) {
		if (this.opt.repeat() == Opt.Repeat.ERROR && this.value != null) {
			problems.add("duplicate assignment of option %s (to \"%s\")".formatted(argument, this.string));
		} else {
			this.set(this.parser.parse(this.string, argument, problems), problems);
		}
	}

	void set(Object value, List<String> problems) {
		switch (this.opt.repeat()) {
			case AGGREGATE -> {
				if (this.value == null) {
					this.value = Array.newInstance(this.field.getType().componentType(), 1);
					Array.set(this.value, 0, value);
				} else {
					var previousLength = Array.getLength(this.value);
					var joined = Array.newInstance(this.field.getType().componentType(), previousLength + 1);
					System.arraycopy(this.value, 0, joined, 0, previousLength);
					Array.set(joined, previousLength, value);
					this.value = joined;
				}
			}
			case AGGREGATE_FLAT -> {
				if (this.value == null) {
					this.value = value;
				} else {
					var previousLength = Array.getLength(this.value);
					var length = Array.getLength(value);
					var joined = Array.newInstance(this.field.getType().componentType(), previousLength + length);
					System.arraycopy(this.value, 0, joined, 0, previousLength);
					System.arraycopy(value, 0, joined, previousLength, length);
					this.value = joined;
				}
			}
			default -> {
				if (this.value == null || this.opt.repeat() != Opt.Repeat.IGNORE) {
					this.value = value;
				}
			}
		}
	}

	void fallBack(List<String> problems) {
		try {
			for (var value : this.fallback) {
				this.set(value.equals("\0") ? MethodHandles.zero(this.type).invoke() : this.parser.parse(this.string, value, problems), problems);
			}
		} catch (Throwable trouble) {
			throw new AssertionError(trouble);
		}
	}
}
