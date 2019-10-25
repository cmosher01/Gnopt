package nu.mine.mosher.gnopt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GnoptValuesTest {
    private static final Logger LOG = LoggerFactory.getLogger(GnoptValuesTest.class);

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
    public static class Nominal {
        int valueCalled = 0;
        List<Optional<String>> values = new ArrayList<>();
        {
            this.values.add(Optional.of("default_value"));
        }

        int argCalled = 0;
        Optional<String> arg = Optional.of("default_arg");

        // value being tested
        public void value(final Optional<String> value) {
            this.valueCalled++;
            if (this.values.size() == 1 && this.values.get(0).isPresent() && this.values.get(0).get().equals("default_value")) {
                this.values = new ArrayList<>();
            }
            this.values.add(Objects.requireNonNull(value));
        }

        public void __(final Optional<String> value) {
            ++this.argCalled;
            this.arg = value;
        }

        public void o(final Optional<String> v) {
        }
    }

    @Test
    void fixtures() {
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
            V option being tested
            O arbitrary other option (not being tested)
            A arbitrary argument (not being tested)
     */

    @Test
    void V() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value=foo");
        assertEquals(1, fixture.valueCalled);
        assertEquals(1, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("foo", fixture.values.get(0).get());
        assertUnchangedArgument(fixture);
    }

    @Test
    void VV() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value=foo", "--value=bar");
        assertEquals(2, fixture.valueCalled);
        assertEquals(2, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("foo", fixture.values.get(0).get());
        assertTrue(fixture.values.get(1).isPresent());
        assertEquals("bar", fixture.values.get(1).get());
        assertUnchangedArgument(fixture);
    }

    @Test
    void VA() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value=foo", "a");
        assertEquals(1, fixture.valueCalled);
        assertEquals(1, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("foo", fixture.values.get(0).get());
    }

    @Test
    void VAV() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value=foo", "a", "--value=bar");
        assertEquals(2, fixture.valueCalled);
        assertEquals(2, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("foo", fixture.values.get(0).get());
        assertTrue(fixture.values.get(1).isPresent());
        assertEquals("bar", fixture.values.get(1).get());
    }

    @Test
    void VO() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value=foo", "--o=bar");
        assertEquals(1, fixture.valueCalled);
        assertEquals(1, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("foo", fixture.values.get(0).get());
        assertUnchangedArgument(fixture);
    }

    @Test
    void VOV() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value=foo", "--o=other", "--value=bar");
        assertEquals(2, fixture.valueCalled);
        assertEquals(2, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("foo", fixture.values.get(0).get());
        assertTrue(fixture.values.get(1).isPresent());
        assertEquals("bar", fixture.values.get(1).get());
        assertUnchangedArgument(fixture);
    }

    @Test
    void equalsWithNoValueShouldPassStringWithLengthZero() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value=");
        assertEquals(1, fixture.valueCalled);
        assertEquals(1, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("", fixture.values.get(0).get());
        assertUnchangedArgument(fixture);
    }

    @Test
    void noEqualsAndNoValueShouldPassOptionalEmpty() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value");
        assertEquals(1, fixture.valueCalled);
        assertEquals(1, fixture.values.size());
        assertFalse(fixture.values.get(0).isPresent()); // NOT present
        assertUnchangedArgument(fixture);
    }

    @Test
    void negValueWithoutOptionName() {
        bad(() -> Gnopt.process(Nominal.class, "--=foo"));
    }

    @Test
    void negDashDashEquals() {
        bad(() -> Gnopt.process(Nominal.class, "--="));
    }

    @Test
    void negUnderscoreUnderscore() {
        bad(() -> Gnopt.process(Nominal.class, "--__"));
    }

    @Test
    void negUnderscoreUnderscoreEqualsNothing() {
        bad(() -> Gnopt.process(Nominal.class, "--__="));
    }

    @Test
    void negUnderscoreUnderscoreEqualsSomething() {
        bad(() -> Gnopt.process(Nominal.class, "--__=foo"));
    }

    @Test
    void equalsSignAsTheValue() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value==");
        assertEquals(1, fixture.valueCalled);
        assertEquals(1, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("=", fixture.values.get(0).get());
        assertUnchangedArgument(fixture);
    }

    @Test
    void hyphenSignAsTheValue() throws Throwable {
        final Nominal fixture = Gnopt.process(Nominal.class, "--value=-");
        assertEquals(1, fixture.valueCalled);
        assertEquals(1, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("-", fixture.values.get(0).get());
        assertUnchangedArgument(fixture);
    }



    private static void assertUnchangedValue(final Nominal fixture) {
        assertEquals(0, fixture.valueCalled);
        assertEquals(1, fixture.values.size());
        assertTrue(fixture.values.get(0).isPresent());
        assertEquals("default_value", fixture.values.get(0).get());
    }

    private static void assertUnchangedArgument(final Nominal fixture) {
        assertEquals(0, fixture.argCalled);
        assertEquals("default_arg", fixture.arg.get());
    }

    private static void bad(final Executable executable) {
        final Gnopt.InvalidOption e = assertThrows(Gnopt.InvalidOption.class, executable);
        LOG.trace("The testing framework caught the following exception:", e);
    }
}
