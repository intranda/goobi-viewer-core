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
import mil.nga.sf.geojson.LineString;
import mil.nga.sf.geojson.Point;
import mil.nga.sf.geojson.Polygon;

public class WKTPolygonReader implements ICoordinateReader {

    private static final String POLYGON_REGEX =
            "POLYGON\\(\\((-?(?:\\d*\\.\\d+|\\d+)\\s+-?(?:\\d*\\.\\d+|\\d+),\\s*)++(-?(?:\\d*\\.\\d+|\\d+)\\s+-?(?:\\d*\\.\\d+|\\d+))\\)\\)";
    private static final String POINT_REGEX = "(-?(?:\\d*\\.\\d+|\\d+)\\s*){2,}";

    @Override
    public boolean canRead(String value) {
        return value.matches(POLYGON_REGEX);
    }

    @Override
    public Geometry read(String value) {
        Matcher matcher = Pattern.compile(POINT_REGEX).matcher(value);
        WKTPointReader pointReader = new WKTPointReader();
        List<Point> points = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group();
            points.add(((Point) pointReader.read(group)));
        }
        return new Polygon(List.of(new LineString(points)));
    }

}
