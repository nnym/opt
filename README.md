```java
public class Main {
	record MyOptions(
		String string,
		@Opt(name = "i") int integer,
		@Opt(name = "f") boolean flag,
		E e,
		@Opt("default,value") Vector<String> strings
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
		assert options.string().equals("foo");
		assert options.integer() == 1729;
		assert options.flag();
		assert options.e() == E.SMITH;
		assert options.strings().equals(List.of("default", "value"));
	}
}
```
```sh
java Main.java -fi 1729 -e smith --string foo
```