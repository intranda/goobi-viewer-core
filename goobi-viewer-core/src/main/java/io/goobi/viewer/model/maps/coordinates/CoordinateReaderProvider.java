/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
