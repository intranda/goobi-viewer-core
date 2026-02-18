package io.goobi.viewer.api.rest.filters;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;

class ImageRequestFilterTest {

    private final static String PI = "12345";
    private final static String FILENAME = "12345";
    private final static String REQUEST_URL = "/api/v1/records/" + PI + "/files/images/" + FILENAME + "/full/max/0/default.jpg";
    private final static String FORWARD_URL = "/api/v1/records/" + PI + "/files/images/" + FILENAME + ".tif/full/max/0/default.jpg";

    @Test
    void test_shouldForwardToCanonicalUrl() throws IOException, PresentationException, IndexUnreachableException {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.spy(HttpServletResponse.class);
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        ImageRequestFilter filter = new ImageRequestFilter(request, response, searchIndex);

        ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);

        Mockito.when(searchIndex.getFilename(PI, FILENAME)).thenReturn(Optional.of(FILENAME + ".tif"));
        Mockito.when(request.getAttribute(FilterTools.ATTRIBUTE_PI)).thenReturn(PI);
        Mockito.when(request.getAttribute(FilterTools.ATTRIBUTE_FILENAME)).thenReturn(FILENAME);
        Mockito.when(request.getRequestURI()).thenReturn(REQUEST_URL);

        filter.filter(requestContext);
        Mockito.verify(response, Mockito.times(1)).sendRedirect(FORWARD_URL);

    }

}
