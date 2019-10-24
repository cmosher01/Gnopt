package nu.mine.mosher.gnopt.compiler;

import org.slf4j.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * A compiler that compiles an option processor class.
 *
 * @param <OptionProcessor>
 */
public class GnoptCompiler<OptionProcessor> {
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
     * @param classProcessor    {@link OptionProcessor} class
     * @param <OptionProcessor> {@link Class} that will process the arguments at runtime
     * @return {@link Map} of option name to corresponding method that will process the argument at runtime
     * @throws InvalidOptionProcessorException if the {@link OptionProcessor} is invalid
     */
    public static <OptionProcessor> GnoptCompiler<OptionProcessor> compile(final Class<OptionProcessor> classProcessor) throws InvalidOptionProcessorException {
        final GnoptCompiler<OptionProcessor> compiler = new GnoptCompiler<>(classProcessor);

        compiler.compile();

        if (compiler.failure()) {
            throw new InvalidOptionProcessorException();
        }
        return compiler;
    }

    public int countProcessors() {
        return this.mapNameToMethod.size();
    }

    public boolean failure() {
        return this.failure;
    }

    public boolean hasProcessorFor(final String name) {
        return this.mapNameToMethod.containsKey(name);
    }

    public Method processor(String name) {
        return this.mapNameToMethod.get(name);
    }

    private static final Logger LOG = LoggerFactory.getLogger(GnoptCompiler.class);

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

    private static boolean isString(final Parameter p) {
        return p.getType().equals(String.class);
    }

    private final Class<OptionProcessor> classProcessor;
    private boolean failure;
    private final Map<String, Method> mapNameToMethod = new HashMap<>();
    private final Set<String> namesEncountered = new HashSet<>();

    private GnoptCompiler(final Class<OptionProcessor> classProcessor) {
        this.classProcessor = classProcessor;
    }

    private void compile() {
        LOG.trace("====> Compiling option-processor {}", this.classProcessor);
        for (final Method method : this.classProcessor.getMethods()) {
            if (method.getDeclaringClass().equals(Object.class)) {
                LOG.trace("----> Skipping, method=\"{}\"", method);
            } else {
                LOG.trace("----> Checking, method=\"{}\"", method);
                useMethodIfValid(method);
            }
        }
    }

    private void error(final String requirement, final Method method) {
        this.failure = true;
        LOG.error("Failure, requirement=\"{}\", method=\"{}\"", requirement, method);
    }

    private void use(final String processorForWhat, final Method method) {
        this.mapNameToMethod.put(method.getName(), method);
        LOG.info("type=\"{}\", method=\"{}\"", processorForWhat, method);
    }

    private void useMethodIfValid(final Method method) {
        // check for DUPLICATE method name
        if (this.namesEncountered.contains(method.getName())) {
            error("method name must be unique", method);
        } else {
            this.namesEncountered.add(method.getName());
        }

        // ensure method RETURNS VOID
        if (!method.getReturnType().equals(Void.TYPE)) {
            error("return type must be void", method);
        }

        // ensure method had proper PARAMETERS
        final Queue<Parameter> params = new LinkedList<>(Arrays.asList(method.getParameters()));
        if (method.getName().equals(METHOD_NAME_FOR_UNNAMED_ARGS)) {
            if (params.size() == 1 && isString(params.peek())) {
                use("non-option arguments", method);
            } else {
                error("must have one and only one String argument", method);
            }
        } else {
            if (params.isEmpty()) {
                use("flag option (does not allow a value)", method);
            } else if (params.size() == 1 && isOptionalString(params.peek())) {
                use("option that allows a value", method);
            } else {
                error("must have one and only one Optional<String> argument", method);
            }
        }
    }
}
