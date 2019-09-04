package io.goobi.viewer.exceptions.streams;

import java.util.Optional;
import java.util.function.Function;

/**
 * Handle checked exception in java 8 streams. 
 * Usage: stream.map(Try.lift(function)).map(try -> try.getValue().orElse(try.getException));
 * 
 * @author florian
 *
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

    public static <T,R> Function<T, Try> lift(CheckedFunction<T,R> function) {

        return t -> {

          try {

            return Try.Success(function.apply(t));

          } catch (Exception ex) {

            return Try.Exception(ex);

          }

        };

      }
    


public static <R> Function<Exception, Try> liftWithValue(CheckedFunction<Exception,R> function) {

  return t -> {

    try {

      return Try.Success(function.apply(t));

    } catch (Exception ex) {

      return Try.Exception(ex);

    }

  };

}
    
    public static <R> Try<R> Exception(Exception value) {

        return new Try<R>(value, null);

    }

    public static <R> Try<R> Success( R value) {

        return new Try<R>(null, value);

    }

    public Optional<Exception> getException() {

        return Optional.ofNullable(exception);

    }

    public Optional<R> getValue() {

        return Optional.ofNullable(success);

    }

    public boolean isException() {

        return exception != null;

    }

    public boolean isSuccess() {

        return success != null;

    }

    public <T> Optional<T> mapLeft(Function<? super Exception, T> mapper) {

        if (isException()) {

            return Optional.of(mapper.apply(exception));

        }

        return Optional.empty();

    }

    public <T> Optional<T> mapRight(Function<? super R, T> mapper) {

        if (isSuccess()) {

            return Optional.of(mapper.apply(success));

        }

        return Optional.empty();

    }

    public String toString() {

        if (isException()) {

            return exception.toString();

        }

        return success.toString();
    }

}