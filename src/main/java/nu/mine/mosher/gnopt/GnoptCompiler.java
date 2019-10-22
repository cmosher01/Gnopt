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

        final Set<String> methodNamesEncountered = new HashSet<>();

        final Method[] methods = optProcClass.getMethods();
        for (final Method method : methods) {
            final String methodName = method.getName();

            if (method.getDeclaringClass().equals(Object.class)) {
                LOG.trace("\"{}\" method is from class java.lang.Object, skipping", methodName);
            } else {
                if (methodNamesEncountered.contains(methodName)) {
                    LOG.error("Duplicate method name \"{}\", found in {}", methodName, optProcClass);
                    fail = true;
                } else {
                    methodNamesEncountered.add(methodName);
                }
                if (!method.getReturnType().equals(Void.TYPE)) {
                    LOG.error("Method \"{}\" returning non-void, found in {}", methodName, optProcClass);
                    fail = true;
                } else {
                    final Parameter[] methodParameters = method.getParameters();
                    if (methodName.equals(METHOD_NAME_FOR_UNNAMED_ARGS)) {
                        if (!(methodParameters.length == 1 && methodParameters[0].getType().equals(String.class))) {
                            LOG.error("Argument processing method \"{}\" must have one and only one String argument, in {}", methodName, optProcClass);
                            fail = true;
                        } else {
                            LOG.info("\"{}\" special argument-processing method; will be called for non-option arguments", methodName);
                            mapNameToMethod.put(methodName, method);
                        }
                    } else {
                        if (1 < methodParameters.length) {
                            LOG.error("Method \"{}\" has two or more arguments, in {}", methodName, optProcClass);
                            fail = true;
                        } else {
                            if (methodParameters.length == 0) {
                                LOG.info("\"{}\" flag-processing method; will be called for an option that is a flag (no value will be allowed)", methodName);
                                mapNameToMethod.put(methodName, method);
                            } else {
                                final Type typ = methodParameters[0].getParameterizedType();
                                if (!(typ instanceof ParameterizedType &&
                                    ((ParameterizedType) typ).getRawType().equals(Optional.class) &&
                                    ((ParameterizedType) typ).getActualTypeArguments()[0].equals(String.class))
                                ) {
                                    LOG.error("Option processing method \"{}\" must have one and only one Optional<String> argument, in {}", methodName, optProcClass);
                                    fail = true;
                                } else {
                                    LOG.info("\"{}\" option-processing method; will be called for an option that allows a value", methodName);
                                    mapNameToMethod.put(methodName, method);
                                }
                            }
                        }
                    }
                }
            }
        }


        if (fail) {
            throw new InvalidOptionProcessorException();
        }

        return mapNameToMethod;
    }
}
