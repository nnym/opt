import misc.MyOptions;
import misc.MyOptions.Digit;
import option.Options;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import java.nio.file.Path;
import java.util.Arrays;

@Testable
public class Tests {
	@Test void test() {
		var options = Options.parse(MyOptions.class, "--port 0xFFFF -h example.net --path /bin/bash --flag".split(" ")).value();

		assert options.port() == 0xFFFF
			&& options.hostname().equals("example.net")
			&& options.path().equals(Path.of("/bin/bash"))
			&& options.flag()
			&& Arrays.equals(options.digits(), new Digit[]{Digit.ZERO, Digit.TWO})
			&& options.query() == null;

		var bp = true;
	}
}
