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
        final GnoptCompiler uut = GnoptCompiler.compile(Nominal.class);
        assertAll(
            () -> uut.hasProcessorFor("flag"),
            () -> uut.hasProcessorFor("value"),
            () -> uut.hasProcessorFor(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS)
        );
    }


    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class Inherited extends Nominal {
        // 4
        public void more(final Optional<String> value) {
        }
    }

    @Test
    void inherited() {
        final GnoptCompiler uut = GnoptCompiler.compile(Inherited.class);
        assertAll(
            () -> uut.hasProcessorFor("flag"),
            () -> uut.hasProcessorFor("value"),
            () -> uut.hasProcessorFor(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS),
        () -> uut.hasProcessorFor("more")
        );
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
    public abstract static class AllMessages {
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

        abstract public void abstractMethod(Optional<String> v);
    }

    @Test
    void negAllMessages() {
        bad(() -> GnoptCompiler.compile(AllMessages.class));
    }

    public interface IFoo {
        void bar(Optional<String> value);
    }
    public static class Foo implements IFoo {
        boolean ok;
        @Override
        public void bar(Optional<String> value) {
            this.ok = true;
        }
    }

    @Test
    void inheritedInterface() {
        final GnoptCompiler uut = GnoptCompiler.compile(Foo.class);
        assertTrue(uut.hasProcessorFor("bar"));
    }

    public static abstract class Abs {
        abstract public void abs(Optional<String> value);
        public void bar(Optional<String> value) {}
    }
    public static class Conc extends Abs {
        @Override
        public void abs(Optional<String> value) {
        }
    }

    @Test
    void concreteSubclass() {
        final GnoptCompiler uut = GnoptCompiler.compile(Conc.class);
        assertTrue(uut.hasProcessorFor("abs"));
        assertTrue(uut.hasProcessorFor("bar"));
    }

    @Test
    void negAbstractMethod() {
        bad(() -> GnoptCompiler.compile(Abs.class));
    }

    private static void bad(final Executable executable) {
        final GnoptCompiler.InvalidOptionProcessorException e = assertThrows(GnoptCompiler.InvalidOptionProcessorException.class, executable);
        LOG.trace("The testing framework caught the following exception:", e);
    }
}
