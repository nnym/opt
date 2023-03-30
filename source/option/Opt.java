package option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 An option record's option's options. Slap it on an option record's component in order to configure its default value, names and behavior.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Opt {
	/**
	 Returns the option's default value which is parsed according to the option's type.
	 An empty array means no default value and that this option is required.
	 An array of 1 element means its element unless it is {@code "\0"} which means the option's type's null value.
	 An array of more elements is invalid.

	 @return the option's default value
	 */
	String[] value() default {};

	/**
	 Returns the option's names. An option must have at least 1 name and at most 1 long name and 1 short name.
	 A name of length 1 is treated as  a short name; in order to make it long, prefix it by {@code "--"}.
	 By default the record component's name is an implicit name of the option unless the special name "-" is among its names.
	 An explicit name overrides the default name if both are short or long.

	 @return the option's names
	 */
	String[] name() default {};

	/**
	 Controls whether the Boolean option must have an explicit {@code false} or {@code true} value.
	 If {@code false}, then the value is by default {@code false} and {@code -o} makes it {@code true} and does not accept arguments.

	 @return whether the Boolean option's value must be specified explicitly
	 */
	boolean explicit() default false;

	Repeat repeat() default Repeat.AGGREGATE_FLAT;

	enum Repeat {
		REPLACE,
		AGGREGATE,
		AGGREGATE_FLAT
	}
}
