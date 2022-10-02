package nu.mine.mosher.gnopt.compiler;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;



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

    /**
     * The name of the public static Map field that provides an optional mapping of Option Names to Method Names.
     * This will allow Option Names that are invalid as Method Names, such as "void" or "for".
     */
    public static final String STATIC_MAPPING_FIELD_NAME = "GNOPT";

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
    public static GnoptCompiler compile(final Class<?> classProcessor) throws InvalidOptionProcessorException {
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
     * Get the method that processes the given option.
     * @param name name of the option (as passed on the command line)
     * @return the method to invoke to process the value of the option, or empty
     */
    public Optional<Method> processor(final String name) {
        return Optional.ofNullable(this.mapNameToMethod.get(name));
    }



    private static final Logger LOG = LoggerFactory.getLogger(GnoptCompiler.class);
    private static final Map<String, Predicate<Method>> REQUIREMENTS = requirements();

    private final Map<String, Method> mapNameToMethod = new HashMap<>();
    private boolean failure;

    private GnoptCompiler() {
    }

    private void comp(final Class<?> classProcessor) {
        LOG.trace("====> Compiling option-processor {}", classProcessor);
        for (final Method method : classProcessor.getMethods()) {
            if (isSkipped(method)) {
                LOG.trace("----> Skipping, method=\"{}\"", method);
            } else {
                LOG.trace("----> Checking, method=\"{}\"", method);
                useMethodIfValid(method, method.getName());
            }
        }

        final var map = getMapping(classProcessor);
        map.forEach((k,v) -> {
            try {
                final var m = classProcessor.getMethod(v, Optional.class);
                LOG.trace("----> Checking, method=\"{}\"", m);
                useMethodIfValid(m, k);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void useMethodIfValid(final Method method, final String optionName) {
        // TODO: add some validation of the optionName such as: can't be empty, can't be "__", others?

        boolean badMethod = false;

        for (final Map.Entry<String, Predicate<Method>> req : REQUIREMENTS.entrySet()) {
            if (!req.getValue().test(method)) {
                LOG.error("Failure, requirement=\"{}\", method=\"{}\"", req.getKey(), method);
                badMethod = true;
            }
        }

        if (badMethod) {
            this.failure = true;
        } else {
            this.mapNameToMethod.put(optionName, method);
        }
    }

    private static Map<String, Predicate<Method>> requirements() {
        return Map.of(
            "return type must be void", m -> m.getReturnType().equals(Void.TYPE),
            "must have one and only one Optional<String> argument", m -> m.getParameters().length == 1 && isOptionalString(m.getParameters()[0]),
            "cannot be abstract", m -> !Modifier.isAbstract(m.getModifiers())
        );
    }

    private static boolean isOptionalString(final Parameter p) {
        final Type typ = p.getParameterizedType();
        if (!(typ instanceof ParameterizedType)) {
            return false;
        }
        final ParameterizedType ptyp = (ParameterizedType)typ;
        return
            ptyp.getRawType().equals(Optional.class) &&
            ptyp.getActualTypeArguments()[0].equals(String.class);
    }

    private static Map<String,String> getMapping(final Class<?> classProcessor) {
        try {
            final var map = classProcessor.getField(STATIC_MAPPING_FIELD_NAME);
            return (Map<String, String>)map.get(null);
        } catch (final Throwable e) {
            return Map.of();
        }
    }

    private static boolean isSkipped(final Method method) {
        return
            method.getDeclaringClass().equals(Object.class) ||
            Modifier.isStatic(method.getModifiers()) ||
            isHidden(method);
    }

    private static boolean isHidden(final Method method) {
        final var name = method.getName();
        return
            name.startsWith(METHOD_NAME_FOR_UNNAMED_ARGS) &&
            !name.equals(METHOD_NAME_FOR_UNNAMED_ARGS);
    }
}
