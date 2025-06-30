package io.goobi.viewer.model.maps.coordinates;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

public class CoordinateReaderProvider {

    private static final Logger logger = LogManager.getLogger(CoordinateReaderProvider.class);

    private final List<ICoordinateReader> coordinateReader;

    public CoordinateReaderProvider() {
        this.coordinateReader = getCoordinateReaderInstances();

    }

    public ICoordinateReader getReader(String value) {
        return coordinateReader.stream()
                .filter(reader -> reader.canRead(value))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No reader found for input string '" + value + "'"));
    }

    private static List<ICoordinateReader> getCoordinateReaderInstances() {
        Set<Class<? extends ICoordinateReader>> readerClasses =
                new Reflections("io.goobi.viewer.model.maps.coordinates").getSubTypesOf(ICoordinateReader.class);
        List<ICoordinateReader> readers = new ArrayList<>(readerClasses.size());
        for (Class<?> clazz : readerClasses) {
            try {
                ICoordinateReader reader = (ICoordinateReader) clazz.getDeclaredConstructor().newInstance();
                readers.add(reader);

            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException
                    | SecurityException e) {
                logger.warn("Error instantiating ");
            }
        }
        return readers;
    }
}
