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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.Point;
import mil.nga.sf.geojson.Position;

public class WKTPointReader implements ICoordinateReader {

    private static final String POINT_REGEX = "(-?(?:\\d*\\.\\d+|\\d+)(?:\\s+|$)){2,}";
    private static final String COORDINATE_REGEX = "-?(?:\\d*\\.\\d+|\\d+)(?:\\s+|$)";

    @Override
    public boolean canRead(String value) {
        return value.matches(POINT_REGEX);
    }

    @Override
    public Geometry read(String value) {
        Matcher matcher = Pattern.compile(COORDINATE_REGEX).matcher(value);
        List<Double> coords = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group();
            coords.add(Double.valueOf(group));
        }
        if (coords.size() == 2) {
            Position position = new Position(coords.get(0), coords.get(1));
            return new Point(position);
        } else if (coords.size() == 3) {
            Position position = new Position(coords.get(0), coords.get(1), coords.get(2));
            return new Point(position);
        } else if (coords.size() > 3) {
            Position position = new Position(coords.get(0), coords.get(1), coords.get(2), coords.subList(3, coords.size()).toArray(Double[]::new));
            return new Point(position);
        } else {
            throw new IllegalArgumentException("Cannot parse '" + value + "' as WKT point");
        }
    }

}
