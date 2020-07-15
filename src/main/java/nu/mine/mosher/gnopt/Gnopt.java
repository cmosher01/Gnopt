package nu.mine.mosher.gnopt;

import nu.mine.mosher.gnopt.compiler.GnoptCompiler;
import org.slf4j.*;

import java.lang.reflect.*;
import java.util.*;

public class Gnopt<OptProc> {
    /**
     * Prefix string of characters that indicates an argument is an option.
     * GNU standard is two hyphen-minus characters.
     */
    public static final String OPT_PREFIX = "\u002D\u002D";

    public static class InvalidOption extends Exception {
        private InvalidOption(final String message) {
            super(message);
        }
        private InvalidOption(final Throwable cause) {
            super(cause);
        }
    }

    /**
     * The main entry-point for the option processor.
     *
     * @param classProcessor {@link Class} of option processor to create, cannot be {@code null}
     * @param args array of command-line arguments to analyze for options,
     *             can be {@code null} (which is treated as an empty array), or
     *             can have elements that are {@code null} (which are treated as empty strings)
     * @param <OptProc> class of classProcessor
     * @return new instance of classProcessor, after processing args
     * @throws InvalidOption if the option processing cannot be performed completely, for any reason
     */
    public static <OptProc> OptProc process(final Class<OptProc> classProcessor, final String... args) throws InvalidOption {
        final GnoptCompiler compilerProcessor = GnoptCompiler.compile(Objects.requireNonNull(classProcessor));

        final OptProc instanceProcessor = instantiate(classProcessor);

        if (Objects.nonNull(args)) {
            new Gnopt<>(compilerProcessor, instanceProcessor).process(args);
        }

        return instanceProcessor;
    }



    private static final Logger LOG = LoggerFactory.getLogger(Gnopt.class);

    private final GnoptCompiler compilerProcessor;
    private final OptProc instanceProcessor;

    /**
     * This is used to keep track of the "--" option that turns
     * off subsequent option processing on the command line.
     */
    private boolean optionProcessingIsEnabled = true;



    private Gnopt(final GnoptCompiler compilerProcessor, final OptProc instanceProcessor) {
        this.compilerProcessor = Objects.requireNonNull(compilerProcessor);
        this.instanceProcessor = Objects.requireNonNull(instanceProcessor);
    }

    private void process(final String[] args) throws InvalidOption {
        for (final String arg : Objects.requireNonNull(args)) {
            processArg(Objects.toString(arg, ""));
        }
    }

    private void processArg(final String arg) throws InvalidOption {
        LOG.trace("processing argument/option: {}", Objects.requireNonNull(arg).isEmpty() ? "(empty argument string)" : arg);
        if (this.optionProcessingIsEnabled && arg.startsWith(OPT_PREFIX)) {
            final String opt = arg.substring(OPT_PREFIX.length());
            if (opt.isEmpty()) {
                this.optionProcessingIsEnabled = false;
            } else {
                processOption(opt);
            }
        } else {
            processNonOption(arg);
        }
    }



    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private class NameValue {
        private final String name;
        private final Optional<String> value;

        private NameValue(final String name, final String value) {
            this.name = name;
            this.value = Optional.of(value);
        }

        private NameValue(final String[] r) throws InvalidOption {
            final String[] r2 = Arrays.copyOf(r, 2);
            this.name = filterName(r2[0]);
            if (this.name.equals(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS)) {
                throwInvalid(this.name);
            }
            this.value = Optional.ofNullable(r2[1]);
        }

        private String filterName(final String name) {
            return name.replace('-', '_');
        }

        private void process() throws InvalidOption {
            Gnopt.this.process(this.name, this.value);
        }
    }



    private void processOption(final String keyEqualsValue) throws InvalidOption {
        new NameValue(keyEqualsValue.split("=", 2)).process();
    }

    private void processNonOption(final String value) throws InvalidOption {
        new NameValue(GnoptCompiler.METHOD_NAME_FOR_UNNAMED_ARGS, value).process();
    }



    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void process(final String name, final Optional<String> value) throws InvalidOption {
        try {
            processor(name).invoke(this.instanceProcessor, value); // <----- !!!!!!! The main purpose of Gnopt is this line.
        } catch (final InvalidOption passThrough) {
            throw passThrough;
        } catch (final InvocationTargetException unwrap) {
            throw new InvalidOption(unwrap.getCause());
        } catch (final Throwable wrap) {
            throw new InvalidOption(wrap);
        }
    }

    private Method processor(final String name) throws InvalidOption {
        final Optional<Method> method = this.compilerProcessor.processor(name);
        if (!method.isPresent()) {
            throwInvalid(name);
        }
        return method.get();
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

    private static <OptProc> OptProc instantiate(final Class<OptProc> classProcessor) throws InvalidOption {
        try {
            return classProcessor.newInstance();
        } catch (final Throwable wrap) { // must catch Throwable, because newInstance
            throw new InvalidOption(wrap);
        }
    }
}
