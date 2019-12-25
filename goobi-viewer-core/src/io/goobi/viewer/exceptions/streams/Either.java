package io.goobi.viewer.exceptions.streams;

import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.math3.util.Pair;

/**
 * <p>Either class.</p>
 *
 */
@SuppressWarnings("rawtypes")
public class Either<L, R> {

    private final L left;

    private final R right;

    private Either(L left, R right) {

        this.left = left;

        this.right = right;

    }

    /**
     * <p>lift.</p>
     *
     * @param function a {@link io.goobi.viewer.exceptions.streams.CheckedFunction} object.
     * @param <T> a T object.
     * @param <R> a R object.
     * @return a {@link java.util.function.Function} object.
     */
    public static <T,R> Function<T, Either> lift(CheckedFunction<T,R> function) {

        return t -> {

          try {

            return Either.Right(function.apply(t));

          } catch (Exception ex) {

            return Either.Left(ex);

          }

        };

      }
    


/**
 * <p>liftWithValue.</p>
 *
 * @param function a {@link io.goobi.viewer.exceptions.streams.CheckedFunction} object.
 * @param <T> a T object.
 * @param <R> a R object.
 * @return a {@link java.util.function.Function} object.
 */
public static <T,R> Function<T, Either> liftWithValue(CheckedFunction<T,R> function) {

  return t -> {

    try {

      return Either.Right(function.apply(t));

    } catch (Exception ex) {

      return Either.Left(new Pair(ex,t));

    }

  };

}
    
    /**
     * <p>Left.</p>
     *
     * @param value a L object.
     * @param <L> a L object.
     * @param <R> a R object.
     * @return a {@link io.goobi.viewer.exceptions.streams.Either} object.
     */
    public static <L,R> Either<L,R> Left( L value) {

        return new Either(value, null);

    }

    /**
     * <p>Right.</p>
     *
     * @param value a R object.
     * @param <L> a L object.
     * @param <R> a R object.
     * @return a {@link io.goobi.viewer.exceptions.streams.Either} object.
     */
    public static <L,R> Either<L,R> Right( R value) {

        return new Either(null, value);

    }

    /**
     * <p>Getter for the field <code>left</code>.</p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<L> getLeft() {

        return Optional.ofNullable(left);

    }

    /**
     * <p>Getter for the field <code>right</code>.</p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<R> getRight() {

        return Optional.ofNullable(right);

    }

    /**
     * <p>isLeft.</p>
     *
     * @return a boolean.
     */
    public boolean isLeft() {

        return left != null;

    }

    /**
     * <p>isRight.</p>
     *
     * @return a boolean.
     */
    public boolean isRight() {

        return right != null;

    }

    /**
     * <p>mapLeft.</p>
     *
     * @param mapper a {@link java.util.function.Function} object.
     * @param <T> a T object.
     * @return a {@link java.util.Optional} object.
     */
    public <T> Optional<T> mapLeft(Function<? super L, T> mapper) {

        if (isLeft()) {

            return Optional.of(mapper.apply(left));

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

        if (isRight()) {

            return Optional.of(mapper.apply(right));

        }

        return Optional.empty();

    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {

        if (isLeft()) {

            return left.toString();

        }

        return right.toString();
    }

}
