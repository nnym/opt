package option;

public @interface Repeat {
	Mode value() default Mode.AGGREGATE_FLAT;

	enum Mode {
		REPLACE,
		AGGREGATE,
		AGGREGATE_FLAT
	}
}
