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
    private static final String FEATURE_NULL_GEOMETRY =
            "{\"type\":\"Feature\",\"geometry\":null,\"properties\":{}}";
    private static final String COLLECTION_WITH_NULL_GEOMETRY =
            "{\"type\":\"FeatureCollection\",\"features\":["
                    + "{\"type\":\"Feature\",\"geometry\":null,\"properties\":{}},"
                    + "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\","
                    + "\"coordinates\":[26.72509,58.38062]},\"properties\":{}}"
                    + "]}";

    /**
     * @verifies parse GeoJSON point with correct coordinates
     * @see GeoJsonReader#read(String)
     */
    @Test
    void read_shouldParseGeoJsonPointWithCorrectCoordinates() {
        ICoordinateReader reader = new GeoJsonReader();
        Assertions.assertTrue(reader.canRead(POINT));
        Geometry geometry = reader.read(POINT);
        Assertions.assertEquals(Point.class, geometry.getClass());
        Assertions.assertEquals(58.38062, ((Point) geometry).getCoordinates().getY());
        Assertions.assertEquals(26.72509, ((Point) geometry).getCoordinates().getX());
    }

    /**
     * @verifies parse GeoJSON polygon with correct structure
     * @see GeoJsonReader#read(String)
     */
    @Test
    void read_shouldParseGeoJsonPolygonWithCorrectStructure() {
        ICoordinateReader reader = new GeoJsonReader();
        Assertions.assertTrue(reader.canRead(POLYGON));
        Geometry geometry = reader.read(POLYGON);
        Assertions.assertEquals(Polygon.class, geometry.getClass());
        Assertions.assertEquals(1, ((Polygon) geometry).getCoordinates().size());
        Assertions.assertEquals(5, ((Polygon) geometry).getCoordinates().get(0).size());
    }

    /**
     * @verifies return null for feature with null geometry
     * @see GeoJsonReader#read(String)
     */
    @Test
    void read_shouldReturnNullForFeatureWithNullGeometry() {
        ICoordinateReader reader = new GeoJsonReader();
        Assertions.assertTrue(reader.canRead(FEATURE_NULL_GEOMETRY));
        Assertions.assertNull(reader.read(FEATURE_NULL_GEOMETRY));
    }

    /**
     * @verifies skip null geometry in feature collection
     * @see GeoJsonReader#read(String)
     */
    @Test
    void read_shouldSkipNullGeometryInFeatureCollection() {
        ICoordinateReader reader = new GeoJsonReader();
        Geometry geometry = reader.read(COLLECTION_WITH_NULL_GEOMETRY);
        Assertions.assertNotNull(geometry);
        Assertions.assertEquals(Point.class, geometry.getClass());
        Assertions.assertEquals(58.38062, ((Point) geometry).getCoordinates().getY());
        Assertions.assertEquals(26.72509, ((Point) geometry).getCoordinates().getX());
    }

}
