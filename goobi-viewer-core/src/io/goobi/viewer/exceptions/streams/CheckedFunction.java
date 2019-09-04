package io.goobi.viewer.exceptions.streams;

@FunctionalInterface

public interface CheckedFunction<T,R> {

    R apply(T t) throws Exception;

}