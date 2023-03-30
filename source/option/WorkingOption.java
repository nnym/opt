package option;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.List;

final class WorkingOption<T> extends BaseOption<T> {
	final Field field;
	final OptionParser<T> parser;
	String string;
	boolean set;
	Object value;

	WorkingOption(Field field, OptionParser<?> parser, String name, char character, String fallback) {
		super((Class<T>) field.getType(), name, character, fallback);

		this.field = field;
		this.parser = (OptionParser<T>) parser;
	}

	void fallBack(List<String> problems) {
		try {
			this.value = this.fallback.equals("\0") ? MethodHandles.zero(this.type).invoke() : this.parser.parse(this.string, this.fallback, problems);
		} catch (Throwable trouble) {
			throw new AssertionError(trouble);
		}
	}
}
