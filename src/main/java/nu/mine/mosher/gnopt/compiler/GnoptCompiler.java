package nu.mine.mosher.gnopt.compiler;

import org.slf4j.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * A compiler that compiles an option processor class.
 * End users should prefer to use the {@link nu.mine.mosher.gnopt.Gnopt} class
 * instead of this class directly.
 */
public class GnoptCompiler {
    /**
     * The name of the method in the given OptionProcessor class that will be called for non-option arguments.
     * It is currently defined as two underscores.
     */
    public static final String METHOD_NAME_FOR_UNNAMED_ARGS = "\u005F\u005F";

    public static final class InvalidOptionProcessorException extends RuntimeException {
    }

    /**
     * Given an option processor class, compiles it, analyzing it for errors.
     *
     * @param classProcessor option processor class, cannot be {@code null}
     * @return {@link Map} of option name to corresponding method that will process the argument at runtime
     * @throws InvalidOptionProcessorException if the classProcessor is invalid
     *      (if so, {@link GnoptCompiler#failure()} will return {@code true})
     */
    public static GnoptCompiler compile(final Class classProcessor) throws InvalidOptionProcessorException {
        final GnoptCompiler compiler = new GnoptCompiler();

        compiler.comp(Objects.requireNonNull(classProcessor));

        if (compiler.failure()) {
            throw new InvalidOptionProcessorException();
        }
        return compiler;
    }

    /**
     * Checks of the compilation failed.
     * @return true if the compilation failed
     */
    public boolean failure() {
        return this.failure;
    }

    /**
     * Checks if there is a processor for the given command line option.
     * @param name name of the option (as passed on the command line)
     * @return true if a processor exists for that option
     */
    public boolean hasProcessorFor(final String name) {
        return this.mapNameToMethod.containsKey(name);
    }

    /**
     * Get the method that processes the given option.
     * @param name name of the option (as passed on the command line)
     * @return the method to invoke to process the value of the option
     */
    public Method processor(final String name) {
        return this.mapNameToMethod.get(name);
    }



    private static final Logger LOG = LoggerFactory.getLogger(GnoptCompiler.class);

    private final Map<String, Method> mapNameToMethod = new HashMap<>();
    private boolean failure;

    private GnoptCompiler() {
    }

    private void comp(final Class classProcessor) {
        LOG.trace("====> Compiling option-processor {}", classProcessor);
        for (final Method method : classProcessor.getMethods()) {
            if (method.getDeclaringClass().equals(Object.class)) {
                LOG.trace("----> Skipping, method=\"{}\"", method);
            } else {
                LOG.trace("----> Checking, method=\"{}\"", method);
                useMethodIfValid(method);
            }
        }
    }

    private void error(final String requirement, final Method method) {
        LOG.error("Failure, requirement=\"{}\", method=\"{}\"", requirement, method);
    }

    private void useMethodIfValid(final Method method) {
        boolean badMethod = false;

        // ensure method RETURNS void
        if (!method.getReturnType().equals(Void.TYPE)) {
            error("return type must be void", method);
            badMethod = true;
        }

        // ensure method has Optional<String> PARAMETER
        final Queue<Parameter> params = new LinkedList<>(Arrays.asList(method.getParameters()));
        if (!(params.size() == 1 && isOptionalString(params.peek()))) {
            error("must have one and only one Optional<String> argument", method);
            badMethod = true;
        }

        // ensure method is not ABSTRACT
        if (Modifier.isAbstract(method.getModifiers())) {
            error("cannot be abstract", method);
            badMethod = true;
        }

        if (badMethod) {
            this.failure = true;
        } else {
            this.mapNameToMethod.put(method.getName(), method);
        }
    }

    private static boolean isOptionalString(final Parameter p) {
        final Type typ = p.getParameterizedType();
        if (!(typ instanceof ParameterizedType)) {
            return false;
        }
        final ParameterizedType ptyp = (ParameterizedType) typ;
        return
            ptyp.getRawType().equals(Optional.class) &&
                ptyp.getActualTypeArguments()[0].equals(String.class);
    }
}
