```java
public class Main {
	record MyOptions(
		@Default("default string") String string,
		@Name("i") int integer,
		@Name({"flag", "f"}) boolean flag,
		E e,
		Vector<String> strings
	) {
		@Parse static Vector<String> strings(String option, String value, List<String> problems) {
			return new Vector<>(List.of(value.split(",")));
		}
	}

	enum E {
		JOHN,
		SMITH
	}

	public static void main(String[] a) {
		var options = Options.parse(MyOptions.class, a).value();
		assert options.string().equals("default string");
		assert options.integer() == 1729;
		assert options.flag();
		assert options.e() == E.SMITH;
		assert options.strings().equals(List.of("a", "b"));
	}
}
```
```sh
java Main.java -fi 1729 -e smith --strings a,b
```