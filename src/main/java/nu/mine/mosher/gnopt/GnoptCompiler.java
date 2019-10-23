package nu.mine.mosher.gnopt;

import org.slf4j.*;

import java.lang.reflect.*;
import java.util.*;

public class GnoptCompiler {
    private static final Logger LOG = LoggerFactory.getLogger(GnoptCompiler.class);

    /**
     * The name of the method in the given OptProc class that will be called for non-option arguments.
     * It is currently defined as two underscores.
     */
    public static final String METHOD_NAME_FOR_UNNAMED_ARGS = "__";

    public static final class InvalidOptionProcessorException extends RuntimeException {
    }

    /**
     * Given an option processor class, compiles it, analyzing it for errors.
     *
     * @param optProcClass {@link OptProc} class
     * @param <OptProc>    {@link Class} that will process the arguments at runtime
     * @return {@link Map} of option name to corresponding method that will process the argument at runtime
     * @throws InvalidOptionProcessorException if the {@link OptProc} is invalid
     */
    public static <OptProc> Map<String, Method> compile(final Class<OptProc> optProcClass) throws InvalidOptionProcessorException {
        final Map<String, Method> mapNameToMethod = new HashMap<>();
        boolean fail = false;

        LOG.trace("Compiling options from {}", optProcClass);

        final Set<String> namesEncountered = new HashSet<>();

        final Method[] methods = optProcClass.getMethods();
        for (final Method method : methods) {
            final String name = method.getName();

            if (method.getDeclaringClass().equals(Object.class)) {
                LOG.trace("\"{}\" method is from class java.lang.Object, skipping", name);
            } else {
                // check for DUPLICATE method name
                if (namesEncountered.contains(name)) {
                    LOG.error("Duplicate method name \"{}\", found in {}", name, optProcClass);
                    fail = true;
                } else {
                    namesEncountered.add(name);
                }

                // ensure method RETURNS VOID
                if (!method.getReturnType().equals(Void.TYPE)) {
                    LOG.error("Method \"{}\" must have a void return type, in {}", name, optProcClass);
                    fail = true;
                }

                // ensure method had proper PARAMETERS
                final Queue<Parameter> params = new LinkedList<>(Arrays.asList(method.getParameters()));
                if (name.equals(METHOD_NAME_FOR_UNNAMED_ARGS)) {
                    if (params.size() == 1 && isString(params.peek())) {
                        LOG.info("\"{}\" special argument-processing method; will be called for non-option arguments", name);
                        mapNameToMethod.put(name, method);
                    } else {
                        LOG.error("Argument processing method \"{}\" must have one and only one String argument, in {}", name, optProcClass);
                        fail = true;
                    }
                } else {
                    if (params.isEmpty()) {
                        LOG.info("\"{}\" flag-processing method; will be called for an option that is a flag (no value will be allowed)", name);
                        mapNameToMethod.put(name, method);
                    } else if (params.size() == 1 && isOptionalString(params.peek())) {
                        LOG.info("\"{}\" option-processing method; will be called for an option that allows a value", name);
                        mapNameToMethod.put(name, method);
                    } else {
                        LOG.error("Option processing method \"{}\" must have one and only one Optional<String> argument, in {}", name, optProcClass);
                        fail = true;
                    }
                }
            }
        }

        if (fail) {
            throw new InvalidOptionProcessorException();
        }

        return mapNameToMethod;
    }



    private static boolean isString(final Parameter p) {
        return p.getType().equals(String.class);
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
}
