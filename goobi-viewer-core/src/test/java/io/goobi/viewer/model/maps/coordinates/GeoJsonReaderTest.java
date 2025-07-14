package io.goobi.viewer.model.maps.coordinates;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.Point;
import mil.nga.sf.geojson.Polygon;

class GeoJsonReaderTest {

    private static final String POLYGON =
            "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[5.3,54.71666666666667],[19.9,54.71666666666667],[19.9,44.75],[5.3,44.75],[5.3,54.71666666666667]]]},\"properties\":{}}]}";
    private static final String POINT =
            "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[26.72509,58.38062]},\"properties\":{}}]}";

    @Test
    void testReadPoint() {
        ICoordinateReader reader = new GeoJsonReader();
        Assertions.assertTrue(reader.canRead(POINT));
        Geometry geometry = reader.read(POINT);
        Assertions.assertEquals(Point.class, geometry.getClass());
        Assertions.assertEquals(58.38062, ((Point) geometry).getCoordinates().getY());
        Assertions.assertEquals(26.72509, ((Point) geometry).getCoordinates().getX());
    }

    @Test
    void testReadPolygon() {
        ICoordinateReader reader = new GeoJsonReader();
        Assertions.assertTrue(reader.canRead(POLYGON));
        Geometry geometry = reader.read(POLYGON);
        Assertions.assertEquals(Polygon.class, geometry.getClass());
        Assertions.assertEquals(1, ((Polygon) geometry).getCoordinates().size());
        Assertions.assertEquals(5, ((Polygon) geometry).getCoordinates().get(0).size());
    }

}
