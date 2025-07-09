package io.goobi.viewer.model.maps.coordinates;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.Polygon;

class WKTPolygonReaderTest {

    private static final String POLYGON = "POLYGON((5.3 54.71666666666667, 19.9 54.71666666666667, 19.9 44.75, 5.3 44.75, 5.3 54.71666666666667))";

    @Test
    void testCanReadPolygon() {
        Assertions.assertTrue(new WKTPolygonReader().canRead(POLYGON));
    }

    @Test
    void testReadAsPolygon() {
        Geometry geometry = new WKTPolygonReader().read(POLYGON);
        Assertions.assertEquals(Polygon.class, geometry.getClass());
    }

    @Test
    void testReadPolygon() {
        Polygon polygon = (Polygon) new WKTPolygonReader().read(POLYGON);
        Assertions.assertEquals(1, polygon.getCoordinates().size());
        Assertions.assertEquals(5, polygon.getCoordinates().get(0).size());
        Assertions.assertEquals(5.3, polygon.getCoordinates().get(0).get(0).getX());
        Assertions.assertEquals(54.71666666666667, polygon.getCoordinates().get(0).get(0).getY());

    }

}
