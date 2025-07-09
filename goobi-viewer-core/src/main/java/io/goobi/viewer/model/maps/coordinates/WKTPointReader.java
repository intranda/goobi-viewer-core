package io.goobi.viewer.model.maps.coordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.Point;
import mil.nga.sf.geojson.Position;

public class WKTPointReader implements ICoordinateReader {

    private static final String POINT_REGEX = "(-?[\\d.]++\\s*){2,}";
    private static final String COORDINATE_REGEX = "-?[\\d.]++\\s*";

    @Override
    public boolean canRead(String value) {
        return value.matches(POINT_REGEX);
    }

    @Override
    public Geometry read(String value) {
        Matcher matcher = Pattern.compile(COORDINATE_REGEX).matcher(value);
        List<Double> coords = new ArrayList<>();
        while (matcher.find()) {
            coords.add(Double.valueOf(matcher.group()));
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
