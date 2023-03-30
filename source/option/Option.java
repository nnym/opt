package option;

public final class Option<T> extends BaseOption<T> {
	public final String string;
	public final T value;

	public Option(Class<T> type, String name, char character, String fallback, String string, T value) {
		super(type, name, character, fallback);

		this.string = string;
		this.value = value;
	}

	@Override public String toString() {
		return this.string;
	}
}
