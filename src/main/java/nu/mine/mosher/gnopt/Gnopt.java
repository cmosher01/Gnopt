package nu.mine.mosher.gnopt;

import org.slf4j.*;

import java.lang.reflect.*;
import java.util.*;

public class Gnopt<OptProc> {
    private static final Logger LOG = LoggerFactory.getLogger(Gnopt.class);

    public static class InvalidOption extends Throwable {
        private InvalidOption(String message) {
            super(message);
        }
    }

    public static final String OPT_PREFIX = "--";

    private final Map<String, Method> processors;
    private final OptProc optProc;
    private boolean optionProcessingIsEnabled = true;

    private Gnopt(final Map<String, Method> processors, final OptProc optProc) {
        this.processors = processors;
        this.optProc = optProc;
    }

    /**
     * The main entrypoint for the Gnopt option processor.
     *
     * @param optProcClass
     * @param args
     * @param <OptProc>
     * @return
     * @throws InvalidOption
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static <OptProc> OptProc process(final Class<OptProc> optProcClass, final String... args) throws InvocationTargetException, IllegalAccessException, InstantiationException, InvalidOption {
        final Map<String, Method> processor = GnoptCompiler.compile(optProcClass);
        final OptProc optProc = optProcClass.newInstance();
        new Gnopt<>(processor, optProc).process(args);
        return optProc;
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

    private void option(final String arg) throws InvalidOption, IllegalAccessException, InvocationTargetException {
        if (arg.contains("=")) {
            final String[] parts = arg.split("=", 2);
            assert parts.length == 2;
            final String name = parts[0];
            final String value = parts[1];

            if (name.equals(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS)) {
                throwInvalid(name);
            }
            final Method method = processor(name);
            if (method.getParameterCount() == 0) {
                throwInvalid(name, "a value is not allowed for option");
            } else {
                method.invoke(this.optProc, Optional.of(value));
            }
        } else {
            final String name = arg;
            if (name.equals(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS)) {
                throwInvalid(name);
            }
            final Method method = processor(name);
            if (method.getParameterCount() == 0) {
                method.invoke(this.optProc);
            } else {
                method.invoke(this.optProc, Optional.<String>empty());
            }
        }
    }

    private void nonoption(final String arg) throws InvalidOption, IllegalAccessException, InvocationTargetException {
        processor(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS).invoke(this.optProc, arg);
    }

    private Method processor(final String name) throws InvalidOption {
        if (!this.processors.containsKey(name)) {
            throwInvalid(name);
        }
        return this.processors.get(name);
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
