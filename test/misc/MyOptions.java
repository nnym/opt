package misc;

import option.Default;
import option.Name;
import option.Parse;

import java.nio.file.Path;
import java.util.List;

public record MyOptions(
	@Name({"hostname", "h"}) String hostname,
	@Default("80") @Name(character = 'p') int port,
	@Default String query,
	Path path,
	boolean flag,
	E e
) {
	@Parse static Path path(String option, String value, List<String> problems) {
		return Path.of(value);
	}
}
