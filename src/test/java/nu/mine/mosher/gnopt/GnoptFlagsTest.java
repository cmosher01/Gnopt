package nu.mine.mosher.gnopt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GnoptFlagsTest {
    private static final Logger LOG = LoggerFactory.getLogger(GnoptFlagsTest.class);

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
    public static class Nominal {
        // flag being tested
        int flag;
        Optional<String> value = Optional.of("default_value");
        String arg = "default_arg";

        // flag being tested
        public void flag() {
            ++this.flag;
        }

        public void value(final Optional<String> value) {
            this.value = value;
        }

        public void __(final String value) {
            this.arg = value;
        }

        public void x() {
        }

        public void v(Optional<String> v) {
        }
    }

    @Test
    void fixtures() {
        final Nominal fixture = new Nominal();
        assertAll(
            () -> assertUnchangedFlag(fixture),
            () -> assertUnchangedValue(fixture),
            () -> assertUnchangedArgument(fixture)
        );
    }

    private static void assertUnchangedFlag(final Nominal fixture) {
        assertEquals(0, fixture.flag);
    }

    private static void assertUnchangedValue(final Nominal fixture) {
        assertTrue(fixture.value.isPresent());
        assertEquals("default_value", fixture.value.get());
    }

    private static void assertUnchangedArgument(final Nominal fixture) {
        assertEquals("default_arg", fixture.arg);
    }

    @Test
    void nil() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class);
        assertAll(
            () -> assertUnchangedFlag(fixture),
            () -> assertUnchangedValue(fixture),
            () -> assertUnchangedArgument(fixture)
        );
    }

    /*
    Abbreviations in test names:
    F flag being tested
    A arbitrary argument (not being tested)
    X arbitrary other flag (not being tested)
    V arbitrary other value-option (not being tested)
     */

    @Test
    void F() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--flag");
        assertEquals(1, fixture.flag);
        assertUnchangedValue(fixture);
        assertUnchangedArgument(fixture);
    }

    @Test
    void FF() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--flag", "--flag");
        assertEquals(2, fixture.flag);
        assertUnchangedValue(fixture);
        assertUnchangedArgument(fixture);
    }

    @Test
    void FA() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--flag", "a");
        assertEquals(1, fixture.flag);
        assertUnchangedValue(fixture);
    }

    @Test
    void FAF() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--flag", "a", "--flag");
        assertEquals(2, fixture.flag);
        assertUnchangedValue(fixture);
    }

    @Test
    void FX() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--flag", "--x");
        assertEquals(1, fixture.flag);
        assertUnchangedArgument(fixture);
        assertUnchangedValue(fixture);
    }

    @Test
    void FXF() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--flag", "--x", "--flag");
        assertEquals(2, fixture.flag);
        assertUnchangedArgument(fixture);
        assertUnchangedValue(fixture);
    }

    @Test
    void FV() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--flag", "--v=v");
        assertEquals(1, fixture.flag);
    }

    @Test
    void FVF() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--flag", "--v=v", "--flag");
        assertEquals(2, fixture.flag);
    }

    @Test
    void negInvalidFlagName() {
        bad(() -> Gnopt.process(Nominal.class, "--foobar"));
    }

    @Test
    void negFlagWithValue() {
        bad(() -> Gnopt.process(Nominal.class, "--flag=xyz"));
    }

    @Test
    void negFlagWithEmptyValue() {
        bad(() -> Gnopt.process(Nominal.class, "--flag="));
    }

    @Test
    void negUnderscoreUnderscore() {
        bad(() -> Gnopt.process(Nominal.class, "--__"));
    }

    private static void bad(final Executable executable) {
        final Gnopt.InvalidOption e = assertThrows(Gnopt.InvalidOption.class, executable);
        LOG.trace("exception", e);
    }
}
