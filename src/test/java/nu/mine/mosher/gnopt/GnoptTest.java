package nu.mine.mosher.gnopt;

import org.junit.jupiter.api.Test;
import org.slf4j.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GnoptTest {
    private static final Logger LOG = LoggerFactory.getLogger(GnoptTest.class);

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class MyOpts {
        private int verbose = 0;
        private String item = "unchanged item";
        private String inputFileName = "unchanged inputFileName";

        public void verbose() {
            ++this.verbose;
        }

        public static class MyException extends Exception {
        }

        public void item(final Optional<String> value) throws MyException {
            this.item = value.orElseThrow(MyException::new);
        }

        public void __(final String value) {
            this.inputFileName = value;
        }
    }

    @Test
    void nominal() throws Throwable {
        final String[] args = {"--item=something", "input.txt", "--verbose"};
        final MyOpts opts = Gnopt.process(MyOpts.class, args);
//        final String bad = Gnopt.process(MyOpts.class); // wrong datatype shouldn't compile

        assertAll(
            () -> assertEquals("something", opts.item),
            () -> assertEquals("input.txt", opts.inputFileName),
            () -> assertEquals(1, opts.verbose)
        );
    }

    @Test
    void nominalVarArgsCall() throws Throwable {
        // varargs-style call is convenient for unit testing
        final MyOpts opts = Gnopt.process(MyOpts.class, "--item=something", "input.txt", "--verbose");

        assertAll(
            () -> assertEquals("something", opts.item),
            () -> assertEquals("input.txt", opts.inputFileName),
            () -> assertEquals(1, opts.verbose)
        );
    }

    @Test
    void dashdash() throws Throwable {
        final MyOpts opts = Gnopt.process(MyOpts.class, "--", "--verbose");
        assertAll(
            () -> assertEquals("--verbose", opts.inputFileName),
            () -> assertEquals(0, opts.verbose)
        );
    }

    @Test
    void dashdashdashdash() throws Throwable {
        final MyOpts opts = Gnopt.process(MyOpts.class, "--", "--");
        assertEquals("--", opts.inputFileName);
    }

    @Test
    void userCanThrowException() {
        final InvocationTargetException invocationTargetException = assertThrows(InvocationTargetException.class, () -> Gnopt.process(MyOpts.class, "--item"));
        LOG.trace("wrapping exception", invocationTargetException);

        final Throwable userException = invocationTargetException.getTargetException();
        LOG.trace("user exception", userException);
        assertEquals(MyOpts.MyException.class, userException.getClass());
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType", "WeakerAccess"})
    public interface Interf {
    }

    @Test
    void interFaceTest() {
        assertThrows(InstantiationException.class, () -> Gnopt.process(Interf.class));
    }
}
