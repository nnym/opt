package misc;

import option.Opt;
import option.Opt.Repeat;
import option.Options;
import option.Parse;

import java.nio.file.Path;
import java.util.List;

public record MyOptions(
	@Opt(name = {"hostname", "h"}) String hostname,
	@Opt(value = "80") int port,
	@Opt("\0") String query,
	boolean flag,
	@Opt(name = "p") List<Path> paths,
	@Opt(value = "zero,two", name = "a") Digit[] digitsA,
	@Opt(value = "one,two", name = "b", repeat = Repeat.ERROR) Digit[] digitsB,
	@Opt(value = "two,two", name = "c", repeat = Repeat.IGNORE) Digit[] digitsC,
	@Opt(value = {"zero", "three"}, name = "d", repeat = Repeat.AGGREGATE) Digit[] digitsD,
	@Opt(value = {"zero,two", "three"}, name = "e", repeat = Repeat.AGGREGATE_FLAT) Digit[] digitsE
) {
	@Parse static List<Path> paths(String option, String value, List<String> problems) {
		return List.of((Path[]) Options.parseArray(Path.class, option, value, problems));
	}

	public enum Digit {
		ZERO,
		ONE,
		TWO,
		THREE
	}
}
