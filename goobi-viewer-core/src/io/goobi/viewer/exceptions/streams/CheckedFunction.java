package io.goobi.viewer.exceptions.streams;

/**
 * <p>CheckedFunction interface.</p>
 */
@FunctionalInterface
public interface CheckedFunction<T,R> {

    /**
     * <p>apply.</p>
     *
     * @param t a T object.
     * @return a R object.
     * @throws java.lang.Exception if any.
     */
    R apply(T t) throws Exception;

}
