package option;

public final class Option<T> extends BaseOption<T> {
	public final String string;
	public final T value;

	public Option(Class<T> type, String name, char character, String fallback, String string, T value) {
		super(null, null, '\0', null);

		this.string = string;
		this.value = value;
	}

	@Override public String toString() {
		return this.string;
	}
}
