package option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Result<T> {
	public final List<String> problems;
	private final T value;

	Result(T value, List<String> problems) {
		this.value = value;
		this.problems = problems;
	}

	public T value() {
		if (this.problems.isEmpty()) {
			return this.value;
		}

		throw new NoSuchElementException(this.problems.stream().map("- "::concat).collect(Collectors.joining("\n", "", ".")));
	}

	static <T> Result<T> build(Function<List<String>, T> function) {
		var problems = new ArrayList<String>();
		return new Result<>(function.apply(problems), Collections.unmodifiableList(problems));
	}
}
