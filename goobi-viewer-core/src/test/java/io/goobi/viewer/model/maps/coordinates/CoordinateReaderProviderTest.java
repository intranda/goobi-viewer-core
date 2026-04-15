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

    private final CoordinateReaderProvider coordinateReaderProvider = new CoordinateReaderProvider();

    /**
     * @verifies throw exception if no reader found
     * @see CoordinateReaderProvider#getReader
     */
    @Test
    void getReader_shouldThrowExceptionIfNoReaderFound() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> coordinateReaderProvider.getReader("bla"));
    }

    /**
     * @verifies read json point
     * @see CoordinateReaderProvider#getReader
     */
    @Test
    void getReader_shouldReadJsonPoint() {
        Assertions.assertEquals(GeoJsonReader.class, coordinateReaderProvider.getReader(JSON_POINT).getClass());
    }

    /**
     * @verifies read json polygon
     * @see CoordinateReaderProvider#getReader
     */
    @Test
    void getReader_shouldReadJsonPolygon() {
        Assertions.assertEquals(GeoJsonReader.class, coordinateReaderProvider.getReader(JSON_POLYGON).getClass());
    }

    /**
     * @verifies read points
     * @see CoordinateReaderProvider#getReader
     */
    @Test
    void getReader_shouldReadPoints() {
        Assertions.assertEquals(WKTPointReader.class, coordinateReaderProvider.getReader(POINT_2D).getClass());
        Assertions.assertEquals(WKTPointReader.class, coordinateReaderProvider.getReader(POINT_3D).getClass());
        Assertions.assertEquals(WKTPointReader.class, coordinateReaderProvider.getReader(POINT_5D).getClass());
    }

    /**
     * @verifies read polygon
     * @see CoordinateReaderProvider#getReader
     */
    @Test
    void getReader_shouldReadPolygon() {
        Assertions.assertEquals(WKTPolygonReader.class, coordinateReaderProvider.getReader(POLYGON).getClass());
    }

}
