package option;

sealed class BaseOption<T> permits Option, WorkingOption {
	public final Class<T> type;
	public final String name;
	public final char character;
	public final String fallback;

	BaseOption(Class<T> type, String name, char character, String fallback) {
		this.type = type;
		this.name = name;
		this.character = character;
		this.fallback = fallback;
	}

	public String formatLong() {
		return "--" + this.name;
	}

	public String formatShort() {
		return "-" + this.character;
	}
}
