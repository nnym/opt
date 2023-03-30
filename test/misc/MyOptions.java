package misc;

import option.Opt;
import option.Parse;

import java.nio.file.Path;
import java.util.List;

public record MyOptions(
	@Opt(name = {"hostname", "h"}) String hostname,
	@Opt(value = "80", name = "p") int port,
	@Opt("\0") String query,
	Path path,
	boolean flag,
	@Opt("zero,two") Digit[] digits
) {
	@Parse static Path path(String option, String value, List<String> problems) {
		return Path.of(value);
	}

	public enum Digit {
		ZERO,
		ONE,
		TWO,
		THREE
	}
}
