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
	 The value {@code "\0"} means the option's type's null value.
	 An empty array means no default value and that this option is required.
	 An array of 2 or more elements is valid only if {@link #repeat} is {@link Repeat#AGGREGATE} or {@link Repeat#AGGREGATE_FLAT}.

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

	/**
	 Determines the method of handling repeated assignments.

	 @return the method of handling repeated assignments
	 @see Repeat
	 */
	Repeat repeat() default Repeat.REPLACE;

	/**
	 A method of handling repeated option assignments.
	 */
	enum Repeat {
		/**
		 Last value wins.
		 */
		REPLACE,

		/**
		 First value wins.
		 */
		IGNORE,

		/**
		 Report an error.
		 */
		ERROR,

		/**
		 Parse the values into objects of the option's component type and aggregate them into an array.
		 */
		AGGREGATE,

		/**
		 Parse the values into arrays of the option's type and join their elements into a single array.
		 */
		AGGREGATE_FLAT;

		boolean aggregate() {
			return this == AGGREGATE || this == AGGREGATE_FLAT;
		}
	}
}
