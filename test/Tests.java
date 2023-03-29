import misc.MyOptions;
import option.Options;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

@Testable
public class Tests {
	@Test void test() {
		var options = Options.parse(MyOptions.class, "--port 0xFFFF -h example.net --path /bin/bash --flag".split(" "));
		var bp = true;
	}
}
