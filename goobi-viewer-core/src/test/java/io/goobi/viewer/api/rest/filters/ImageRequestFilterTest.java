package io.goobi.viewer.api.rest.filters;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageRequestFilterTest {

    private final static String PI = "12345";
    private final static String FILENAME = "12345";
    private final static String REQUEST_URL = "/api/v1/records/" + PI + "/files/images/" + FILENAME + "/full/max/0/default.jpg";
    private final static String FORWARD_URL = "/api/v1/records/" + PI + "/files/images/" + FILENAME + ".tif/full/max/0/default.jpg";

    @Test
    void test_shouldForwardToCanonicalUrl() throws IOException, PresentationException, IndexUnreachableException {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        ImageRequestFilter filter = new ImageRequestFilter(request, searchIndex);

        ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);

        Mockito.when(searchIndex.getFilename(PI, FILENAME)).thenReturn(Optional.of(FILENAME + ".tif"));
        Mockito.when(request.getAttribute(FilterTools.ATTRIBUTE_PI)).thenReturn(PI);
        Mockito.when(request.getAttribute(FilterTools.ATTRIBUTE_FILENAME)).thenReturn(FILENAME);
        Mockito.when(request.getRequestURI()).thenReturn(REQUEST_URL);

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        Mockito.verify(requestContext, Mockito.times(1)).abortWith(responseCaptor.capture());
        assertEquals(Response.Status.FOUND.getStatusCode(), responseCaptor.getValue().getStatus());
        assertEquals(URI.create(FORWARD_URL), responseCaptor.getValue().getLocation());
    }

}
