package io.goobi.viewer.model.maps.coordinates;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.Point;

class WKTPointReaderTest {

    private static final String INCOMPLETE_POINT = "51.235";
    private static final String POINT_2D = "15.423 51.235 ";
    private static final String POINT_3D = "15.423 51.235 -7.24365";
    private static final String POINT_5D = "15.423 51.235 -7.24365 45.52 -23.3";

    @Test
    void testCanReadPoint() {
        Assertions.assertFalse(new WKTPointReader().canRead(INCOMPLETE_POINT));
        Assertions.assertTrue(new WKTPointReader().canRead(POINT_2D));
        Assertions.assertTrue(new WKTPointReader().canRead(POINT_3D));
        Assertions.assertTrue(new WKTPointReader().canRead(POINT_5D));
    }

    @Test
    void restRead2D() {
        Geometry point = new WKTPointReader().read(POINT_2D);
        Assertions.assertEquals(Point.class, point.getClass());
        Assertions.assertEquals(15.423, ((Point) point).getPosition().getX());
        Assertions.assertEquals(51.235, ((Point) point).getPosition().getY());
    }

    @Test
    void restRead3D() {
        Geometry point = new WKTPointReader().read(POINT_3D);
        Assertions.assertEquals(Point.class, point.getClass());
        Assertions.assertEquals(15.423, ((Point) point).getPosition().getX());
        Assertions.assertEquals(51.235, ((Point) point).getPosition().getY());
        Assertions.assertEquals(-7.24365, ((Point) point).getPosition().getZ());
    }

    @Test
    void restRead5D() {
        Geometry point = new WKTPointReader().read(POINT_5D);
        Assertions.assertEquals(Point.class, point.getClass());
        Assertions.assertEquals(15.423, ((Point) point).getPosition().getX());
        Assertions.assertEquals(51.235, ((Point) point).getPosition().getY());
        Assertions.assertEquals(-7.24365, ((Point) point).getPosition().getZ());
        Assertions.assertEquals(45.52, ((Point) point).getPosition().getM());
    }

}
