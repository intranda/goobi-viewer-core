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
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageRequestFilterTest {

    private final static String PI = "12345";
    private final static String FILENAME = "12345";
    private final static String REQUEST_URL = "/api/v1/records/" + PI + "/files/images/" + FILENAME + "/full/max/0/default.jpg";
    private final static String FORWARD_URL = "/api/v1/records/" + PI + "/files/images/" + FILENAME + ".tif/full/max/0/default.jpg";

    /**
     * @verifies forward to canonical url
     */
    @Test
    void filter_shouldForwardToCanonicalUrl() throws IOException, PresentationException, IndexUnreachableException {

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

    /**
     * Regression guard: real production logs showed crawler hits with 13-digit numeric "image
     * orders" (ms-epoch-shaped) like 1563951015211. The \d+ regex lets them through, but
     * Integer.parseInt overflows. The catch must swallow the NumberFormatException and return
     * null instead of bubbling up.
     *
     * @see ImageRequestFilter#forwardToCanonicalUrl(String, String, HttpServletRequest)
     * @verifies return null and not throw when numeric image order exceeds Integer range
     */
    @Test
    void forwardToCanonicalUrl_shouldReturnNullAndNotThrowWhenNumericImageOrderExceedsIntegerRange()
            throws IOException, PresentationException, IndexUnreachableException {

        String oversizedOrder = "1563951015211";

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        ImageRequestFilter filter = new ImageRequestFilter(request, searchIndex);

        // First lookup (by filename) yields nothing, falling through to the numeric branch
        // where Integer.parseInt will overflow.
        Mockito.when(searchIndex.getFilename(PI, oversizedOrder)).thenReturn(Optional.empty());

        String result = filter.forwardToCanonicalUrl(PI, oversizedOrder, request);

        assertNull(result);
        // Numeric overload must never be invoked because the parse fails before that point.
        Mockito.verify(searchIndex, Mockito.never()).getFilename(Mockito.eq(PI), Mockito.anyInt());
    }

}
