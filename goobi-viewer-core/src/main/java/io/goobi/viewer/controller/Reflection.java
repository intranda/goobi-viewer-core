package io.goobi.viewer.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Reflection {

    private static final Logger logger = LogManager.getLogger(FileTools.class);

    public static Optional<Object> getMethodReturnValue(Object object, String method) {
        try {
            return Optional.ofNullable(object.getClass().getMethod(method).invoke(object));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            logger.error("Error calling method {} ob object {}", method, object);
            return Optional.empty();
        }
    }
}
