package option;

import java.util.List;

@FunctionalInterface
public interface OptionParser<T> {
	T parse(String option, String value, List<String> problems);
}
