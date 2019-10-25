package nu.mine.mosher.gnopt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GnoptArgsTest {
    private static final Logger LOG = LoggerFactory.getLogger(GnoptArgsTest.class);

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
    public static class Nominal {
        int argCalled = 0;
        List<String> args = new ArrayList<>();
        {
            this.args.add("default_arg");
        }

        int valueCalled = 0;

        // argument processing method being tested
        public void __(final Optional<String> value) {
            assert Objects.requireNonNull(value).isPresent();
            ++this.argCalled;
            if (this.args.size() == 1 && this.args.get(0).equals("default_arg")) {
                this.args = new ArrayList<>();
            }
            this.args.add(value.get());
        }

        public void value(final Optional<String> value) {
            ++this.valueCalled;
        }
    }

    @Test
    void fixture() {
        final Nominal fixture = new Nominal();
        assertAll(
            () -> assertUnchangedValue(fixture),
            () -> assertUnchangedArgument(fixture)
        );
    }

    @Test
    void nil() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class);
        assertAll(
            () -> assertUnchangedValue(fixture),
            () -> assertUnchangedArgument(fixture)
        );
    }

    /*
    Abbreviations in test names:
    A non-option argument being tested
    V arbitrary option (not being tested)
     */

    @Test
    void A() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "foo");
        assertEquals(1, fixture.argCalled);
        assertEquals(1, fixture.args.size());
        assertEquals("foo", fixture.args.get(0));
        assertUnchangedValue(fixture);
    }

    @Test
    void AA() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "foo", "bar");
        assertEquals(2, fixture.argCalled);
        assertEquals(2, fixture.args.size());
        assertEquals("foo", fixture.args.get(0));
        assertEquals("bar", fixture.args.get(1));
        assertUnchangedValue(fixture);
    }

    @Test
    void AV() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "foo", "--value=x");
        assertEquals(1, fixture.argCalled);
        assertEquals(1, fixture.args.size());
        assertEquals("foo", fixture.args.get(0));
    }

    @Test
    void AVA() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "foo", "--value=x", "bar");
        assertEquals(2, fixture.argCalled);
        assertEquals(2, fixture.args.size());
        assertEquals("foo", fixture.args.get(0));
        assertEquals("bar", fixture.args.get(1));
    }

    @Test
    void dash() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "-");
        assertEquals(1, fixture.argCalled);
        assertEquals(1, fixture.args.size());
        assertEquals("-", fixture.args.get(0));
        assertUnchangedValue(fixture);
    }

    @Test
    void equalsSign() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "=");
        assertEquals(1, fixture.argCalled);
        assertEquals(1, fixture.args.size());
        assertEquals("=", fixture.args.get(0));
        assertUnchangedValue(fixture);
    }

    @Test
    void emptyString() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "");
        assertEquals(1, fixture.argCalled);
        assertEquals(1, fixture.args.size());
        assertEquals("", fixture.args.get(0));
        assertUnchangedValue(fixture);
    }

    @Test
    void nullForArray() throws Throwable {
        final String[] nullArray = null;
        final Nominal fixture = Gnopt.process(Nominal.class, nullArray);
        assertAll(
            () -> assertUnchangedValue(fixture),
            () -> assertUnchangedArgument(fixture)
        );
    }

    @Test
    void oneNullArgument() throws Throwable {
        final String[] oneNullArg = new String[] { null };
        final Nominal fixture = Gnopt.process(Nominal.class, oneNullArg);
        assertEquals(1, fixture.argCalled);
        assertEquals(1, fixture.args.size());
        assertEquals("", fixture.args.get(0));
        assertUnchangedValue(fixture);
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
    public static class NoArgs {
    }

    @Test
    void negNoArgs() {
        bad(() -> Gnopt.process(NoArgs.class, "no-args-allowed"));
    }

    private static void assertUnchangedValue(final Nominal fixture) {
        assertEquals(0, fixture.valueCalled);
    }

    private static void assertUnchangedArgument(final Nominal fixture) {
        assertEquals(1, fixture.args.size());
        assertEquals("default_arg", fixture.args.get(0));
    }

    private static void bad(final Executable executable) {
        final Gnopt.InvalidOption e = assertThrows(Gnopt.InvalidOption.class, executable);
        LOG.trace("The testing framework caught the following exception:", e);
    }
}
