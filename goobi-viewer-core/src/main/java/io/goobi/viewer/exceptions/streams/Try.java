package io.goobi.viewer.exceptions.streams;

import java.util.Optional;
import java.util.function.Function;

/**
 * Handle checked exception in java 8 streams.
 * Usage: stream.map(Try.lift(function)).map(try -> try.getValue().orElse(try.getException));
 *
 * @author florian
 * @param <R>
 */
@SuppressWarnings("rawtypes")
public class Try<R> {

    private final Exception exception;

    private final R success;

    private Try(Exception exception, R success) {

        this.exception = exception;

        this.success = success;

    }

    /**
     * <p>lift.</p>
     *
     * @param function a {@link io.goobi.viewer.exceptions.streams.CheckedFunction} object.
     * @param <T> a T object.
     * @param <R> a R object.
     * @return a {@link java.util.function.Function} object.
     */
    public static <T,R> Function<T, Try> lift(CheckedFunction<T,R> function) {

        return t -> {

          try {

            return Try.Success(function.apply(t));

          } catch (Exception ex) {

            return Try.Exception(ex);

          }

        };

      }
    


/**
 * <p>liftWithValue.</p>
 *
 * @param function a {@link io.goobi.viewer.exceptions.streams.CheckedFunction} object.
 * @param <R> a R object.
 * @return a {@link java.util.function.Function} object.
 */
public static <R> Function<Exception, Try> liftWithValue(CheckedFunction<Exception,R> function) {

  return t -> {

    try {

      return Try.Success(function.apply(t));

    } catch (Exception ex) {

      return Try.Exception(ex);

    }

  };

}
    
    /**
     * <p>Exception.</p>
     *
     * @param value a {@link java.lang.Exception} object.
     * @param <R> a R object.
     * @return a {@link io.goobi.viewer.exceptions.streams.Try} object.
     */
    public static <R> Try<R> Exception(Exception value) {

        return new Try<R>(value, null);

    }

    /**
     * <p>Success.</p>
     *
     * @param value a R object.
     * @param <R> a R object.
     * @return a {@link io.goobi.viewer.exceptions.streams.Try} object.
     */
    public static <R> Try<R> Success( R value) {

        return new Try<R>(null, value);

    }

    /**
     * <p>Getter for the field <code>exception</code>.</p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<Exception> getException() {

        return Optional.ofNullable(exception);

    }

    /**
     * <p>getValue.</p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<R> getValue() {

        return Optional.ofNullable(success);

    }

    /**
     * <p>isException.</p>
     *
     * @return a boolean.
     */
    public boolean isException() {

        return exception != null;

    }

    /**
     * <p>isSuccess.</p>
     *
     * @return a boolean.
     */
    public boolean isSuccess() {

        return success != null;

    }

    /**
     * <p>mapLeft.</p>
     *
     * @param mapper a {@link java.util.function.Function} object.
     * @param <T> a T object.
     * @return a {@link java.util.Optional} object.
     */
    public <T> Optional<T> mapLeft(Function<? super Exception, T> mapper) {

        if (isException()) {

            return Optional.of(mapper.apply(exception));

        }

        return Optional.empty();

    }

    /**
     * <p>mapRight.</p>
     *
     * @param mapper a {@link java.util.function.Function} object.
     * @param <T> a T object.
     * @return a {@link java.util.Optional} object.
     */
    public <T> Optional<T> mapRight(Function<? super R, T> mapper) {

        if (isSuccess()) {

            return Optional.of(mapper.apply(success));

        }

        return Optional.empty();

    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {

        if (isException()) {

            return exception.toString();

        }

        return success.toString();
    }

}
