package nu.mine.mosher.gnopt;

import nu.mine.mosher.gnopt.compiler.GnoptCompiler;
import org.slf4j.*;

import java.lang.reflect.*;
import java.util.*;

public class Gnopt<OptProc> {
    /**
     * Prefix string of characters to indicate that an argument is an option.
     * GNU standard is two hyphen-minus characters.
     */
    public static final String OPT_PREFIX = "\u002D\u002D";

    public static class InvalidOption extends Throwable {
        private InvalidOption(String message) {
            super(message);
        }
    }

    /**
     * The main entry-point for the option processor.
     *
     * @param classProcessor {@link Class} of option processor to create
     * @param args array of command-line arguments to analyze for options
     * @param <OptProc> class of classProcessor
     * @return new instance of classProcessor, after processing args
     * @throws InvalidOption
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static <OptProc> OptProc process(final Class<OptProc> classProcessor, final String... args) throws InvocationTargetException, IllegalAccessException, InstantiationException, InvalidOption {
        final GnoptCompiler<OptProc> compiler = GnoptCompiler.compile(classProcessor);
        final OptProc instanceProcessor = classProcessor.newInstance();

        new Gnopt<>(compiler, instanceProcessor).process(args);

        return instanceProcessor;
    }



    private static final Logger LOG = LoggerFactory.getLogger(Gnopt.class);
    private final GnoptCompiler<OptProc> compiler;
    private final OptProc instanceProcessor;
    private boolean optionProcessingIsEnabled = true;

    private Gnopt(final GnoptCompiler<OptProc> compiler, final OptProc instanceProcessor) {
        this.compiler = compiler;
        this.instanceProcessor = instanceProcessor;
    }

    private void process(final String[] args) throws InvocationTargetException, IllegalAccessException, InvalidOption {
        for (final String arg : args) {
            processArg(arg);
        }
    }

    private void processArg(final String arg) throws InvalidOption, IllegalAccessException, InvocationTargetException {
        LOG.trace("processing argument/option: {}", arg.isEmpty() ? "(empty argument string)" : arg);
        if (this.optionProcessingIsEnabled && arg.startsWith(OPT_PREFIX)) {
            if (arg.equals(OPT_PREFIX)) {
                this.optionProcessingIsEnabled = false;
            } else {
                option(arg.substring(OPT_PREFIX.length()));
            }
        } else {
            nonoption(arg);
        }
    }

    private static class NameValue {
        final String name;
        final String value;
        private NameValue(final String[] r) {
            assert r.length == 2;
            this.name = r[0];
            this.value = r[1];
        }
    }

    private void option(final String arg) throws InvalidOption, IllegalAccessException, InvocationTargetException {
        if (arg.contains("=")) {
            value(new NameValue(arg.split("=", 2)));
        } else {
            flag(arg);
        }
    }

    private void nonoption(final String arg) throws InvalidOption, IllegalAccessException, InvocationTargetException {
        processor(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS).invoke(this.instanceProcessor, arg);
    }

    private void value(final NameValue kv) throws InvalidOption, IllegalAccessException, InvocationTargetException {
        if (kv.name.equals(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS)) {
            throwInvalid(kv.name);
        }
        final Method method = processor(kv.name);
        if (method.getParameterCount() == 0) {
            throwInvalid(kv.name, "a value is not allowed for option");
        } else {
            method.invoke(this.instanceProcessor, Optional.of(kv.value));
        }
    }

    private void flag(final String name) throws InvalidOption, IllegalAccessException, InvocationTargetException {
        if (name.equals(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS)) {
            throwInvalid(name);
        }
        final Method method = processor(name);
        if (method.getParameterCount() == 0) {
            method.invoke(this.instanceProcessor);
        } else {
            method.invoke(this.instanceProcessor, Optional.<String>empty());
        }
    }

    private Method processor(final String name) throws InvalidOption {
        if (!this.compiler.hasProcessorFor(name)) {
            throwInvalid(name);
        }
        return this.compiler.processor(name);
    }

    private static void throwInvalid(final String name) throws InvalidOption {
        if (name.equals(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS)) {
            throwInvalid(null, "no arguments are allowed");
        } else {
            throwInvalid(name, "invalid option");
        }
    }

    private static void throwInvalid(final String name, final String message) throws InvalidOption {
        final String display;
        if (Objects.isNull(name)) {
            display = "";
        } else if (name.trim().isEmpty()) {
            display = " (with no name)";
        } else {
            display = " \"" + name + "\"";
        }
        throw new InvalidOption(message + display);
    }
}
