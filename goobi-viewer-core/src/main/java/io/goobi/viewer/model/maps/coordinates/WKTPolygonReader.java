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

    private static final String POLYGON_REGEX = "POLYGON\\(\\((-?[\\d.]+\\s+-?[\\d.]+,\\s*)++(-?[\\d.]+\\s+-?[\\d.]+)\\)\\)";
    private static final String POINT_REGEX = "(-?[\\d.]++\\s*){2,}";

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
            points.add(((Point) pointReader.read(matcher.group())));
        }
        return new Polygon(List.of(new LineString(points)));
    }

}
