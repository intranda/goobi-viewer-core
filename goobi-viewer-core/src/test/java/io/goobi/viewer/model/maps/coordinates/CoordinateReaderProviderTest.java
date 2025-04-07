package io.goobi.viewer.model.maps.coordinates;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CoordinateReaderProviderTest {

    private static final String JSON_POLYGON =
            "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[5.3,54.71666666666667],[19.9,54.71666666666667],[19.9,44.75],[5.3,44.75],[5.3,54.71666666666667]]]},\"properties\":{}}]}";
    private static final String JSON_POINT =
            "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[26.72509,58.38062]},\"properties\":{}}]}";
    private static final String POINT_2D = "15.423 51.235 ";
    private static final String POINT_3D = "15.423 51.235 -7.24365";
    private static final String POINT_5D = "15.423 51.235 -7.24365 45.52 -23.3";
    private static final String POLYGON = "POLYGON((5.3 54.71666666666667, 19.9 54.71666666666667, 19.9 44.75, 5.3 44.75, 5.3 54.71666666666667))";

    @Test
    void testThrowExceptionIfNoReaderFound() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CoordinateReaderProvider.getReader("bla"));
    }

    @Test
    void testReadJsonPoint() {
        Assertions.assertEquals(GeoJsonReader.class, CoordinateReaderProvider.getReader(JSON_POINT).getClass());
    }

    @Test
    void testReadJsonPolygon() {
        Assertions.assertEquals(GeoJsonReader.class, CoordinateReaderProvider.getReader(JSON_POLYGON).getClass());
    }

    @Test
    void testReadPoints() {
        Assertions.assertEquals(WKTPointReader.class, CoordinateReaderProvider.getReader(POINT_2D).getClass());
        Assertions.assertEquals(WKTPointReader.class, CoordinateReaderProvider.getReader(POINT_3D).getClass());
        Assertions.assertEquals(WKTPointReader.class, CoordinateReaderProvider.getReader(POINT_5D).getClass());
    }

    @Test
    void testReadPolygon() {
        Assertions.assertEquals(WKTPolygonReader.class, CoordinateReaderProvider.getReader(POLYGON).getClass());
    }

}
