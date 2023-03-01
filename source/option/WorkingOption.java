package option;

import java.lang.reflect.Field;
import java.util.List;

final class WorkingOption<T> extends BaseOption<T> {
	final Field field;
	final OptionParser<T> parser;
	String string;
	boolean set;
	Object value;

	WorkingOption(Field field, OptionParser<?> parser, String name, char character, Default fallback) {
		super((Class<T>) field.getType(), name, character, fallback);

		this.field = field;
		this.parser = (OptionParser<T>) parser;
	}

	void fallBack(List<String> problems) {
		var fallback = this.fallback == null ? "\u0000" : this.fallback.value();
		this.value = !fallback.equals("\u0000") ? this.parser.parse(this.string, fallback, problems)
			: this.type == boolean.class ? false
			: this.type == byte.class ? (byte) 0
			: this.type == char.class ? '\u0000'
			: this.type == short.class ? (short) 0
			: this.type == int.class ? 0
			: this.type == long.class ? 0L
			: this.type == float.class ? 0F
			// Need to cast the `0D` to `Object` in order to change the expression's (boolean ? double : null) type from `double` to `Object`;
			// otherwise the compiler will try to invoke `Double::doubleValue` on the `null`.
			: this.type == double.class ? (Object) 0D
			: null;
	}
}
