package io.goobi.viewer.model.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

class FilterQueryParserTest {

    private static final String URI_FIRST_QUERY =
            "?filterQuery=WKT_COORDS%3A\"Intersects(POINT(11.24626+43.77925))+distErrPct%3D0\"&test=123";

    private static final String URI_MIDDLE_QUERY =
            "?jlksdf=nfkjn&filterQuery=WKT_COORDS%3A\"Intersects(POINT(11.24626+43.77925))+distErrPct%3D0\"&test=123";

    private static final String URI_LAST_QUERY =
            "?jlksdf=nfkjn&filterQuery=WKT_COORDS%3A\"Intersects(POINT(11.24626+43.77925))+distErrPct%3D0\"";

    @Test
    void test_firstQuery() {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getQueryString()).thenReturn(URI_FIRST_QUERY);

        FilterQueryParser parser = new FilterQueryParser();
        Assertions.assertEquals("WKT_COORDS:\"Intersects(POINT(11.24626 43.77925)) distErrPct=0\"",
                parser.getFilterQuery(request).orElse(""));

    }

    @Test
    void test_middleQuery() {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getQueryString()).thenReturn(URI_MIDDLE_QUERY);

        FilterQueryParser parser = new FilterQueryParser();
        Assertions.assertEquals("WKT_COORDS:\"Intersects(POINT(11.24626 43.77925)) distErrPct=0\"",
                parser.getFilterQuery(request).orElse(""));

    }

    @Test
    void test_lastQuery() {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getQueryString()).thenReturn(URI_LAST_QUERY);

        FilterQueryParser parser = new FilterQueryParser();
        Assertions.assertEquals("WKT_COORDS:\"Intersects(POINT(11.24626 43.77925)) distErrPct=0\"",
                parser.getFilterQuery(request).orElse(""));

    }

}
