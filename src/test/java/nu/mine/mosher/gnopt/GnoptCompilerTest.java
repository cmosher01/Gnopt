package nu.mine.mosher.gnopt;

import nu.mine.mosher.gnopt.compiler.GnoptCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GnoptCompilerTest {
    private static final Logger LOG = LoggerFactory.getLogger(GnoptCompilerTest.class);

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class Nominal {
        // 1
        public void flag(final Optional<String> value) {
        }

        // 2
        public void value(final Optional<String> value) {
        }

        // 3
        public void __(final Optional<String> value) {
        }

        private void privateMethodNotSeenByCompiler() {
        }

        protected void protectedMethodNotSeenByCompiler() {
        }

        void packageMethodNotSeenByCompiler() {
        }
    }

    @Test
    void nominal() {
        final GnoptCompiler<Nominal> uut = GnoptCompiler.compile(Nominal.class);
        assertEquals(3, uut.countProcessors());
    }


    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class Inherited extends Nominal {
        // 4
        public void more(final Optional<String> value) {
        }
    }

    @Test
    void inherited() {
        final GnoptCompiler<Inherited> uut = GnoptCompiler.compile(Inherited.class);
        assertEquals(4, uut.countProcessors());
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class NonVoidReturn {
        public String foo() {
            return "junk";
        }
    }

    @Test
    void negNonVoidReturn() {
        bad(() -> GnoptCompiler.compile(NonVoidReturn.class));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class NonVoidReturnGoodParam {
        public String foo(Optional<String> value) {
            return "junk";
        }
    }

    @Test
    void negNonVoidReturnGoodParam() {
        bad(() -> GnoptCompiler.compile(NonVoidReturnGoodParam.class));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class TwoParameters {
        public void foo(Optional<String> one, Optional<String> two) {
        }
    }

    @Test
    void negTwoParameters() {
        bad(() -> GnoptCompiler.compile(TwoParameters.class));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class StringParameter {
        public void foo(String one) {
        }
    }

    @Test
    void negStringParameter() {
        bad(() -> GnoptCompiler.compile(StringParameter.class));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class IntegerParameter {
        public void foo(Integer one) {
        }
    }

    @Test
    void negIntegerParameter() {
        bad(() -> GnoptCompiler.compile(IntegerParameter.class));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class IntParameter {
        public void foo(int one) {
        }
    }

    @Test
    void negIntParameters() {
        bad(() -> GnoptCompiler.compile(IntParameter.class));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class ArgProcWithNoParams {
        public void __() {
        }
    }

    @Test
    void negArgProcWithNoParams() {
        bad(() -> GnoptCompiler.compile(ArgProcWithNoParams.class));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class ArgProcWithInteger {
        public void __(Integer value) {
        }
    }

    @Test
    void negArgProcWithInteger() {
        bad(() -> GnoptCompiler.compile(ArgProcWithInteger.class));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class ArgProcWithInt {
        public void __(int value) {
        }
    }

    @Test
    void negArgProcWithInt() {
        bad(() -> GnoptCompiler.compile(ArgProcWithInt.class));
    }

    @Test
    void negNullClass() {
        assertThrows(NullPointerException.class, () -> GnoptCompiler.compile(null));
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class AllMessages {
        public void __() {
        }

        public String a() {
            return "";
        }

        public void b(Random r) {
        }

        public void c(Random r, Random s) {
        }

        public void dup() {
        }

        public void dup(Optional<String> v) {
        }

        public void __(String ok) {
        }

        public void flag() {
        }

        public void value(Optional<String> ok) {
        }

        public String twoProblems(Integer badArg) {
            return "";
        }
    }

    @Test
    void negAllMessages() {
        bad(() -> GnoptCompiler.compile(AllMessages.class));
    }

    private static void bad(final Executable executable) {
        final GnoptCompiler.InvalidOptionProcessorException e = assertThrows(GnoptCompiler.InvalidOptionProcessorException.class, executable);
        LOG.trace("The testing framework caught the following exception:", e);
    }
}
