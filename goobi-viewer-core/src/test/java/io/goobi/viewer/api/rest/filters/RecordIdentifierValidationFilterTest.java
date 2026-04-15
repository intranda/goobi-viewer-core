/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

class RecordIdentifierValidationFilterTest {

    /**
     * @see RecordIdentifierValidationFilter#filter(ContainerRequestContext)
     * @verifies pass through valid pi
     */
    @Test
    void filter_shouldPassThroughValidPi() throws IOException {
        ContainerRequestContext ctx = mockContextWithPath("records", "PI123");
        new RecordIdentifierValidationFilter().filter(ctx);
        verify(ctx, never()).abortWith(any(Response.class));
    }

    /**
     * @see RecordIdentifierValidationFilter#filter(ContainerRequestContext)
     * @verifies abort with 400 when PI contains space
     */
    @Test
    void filter_shouldAbortWith400WhenPiContainsSpace() throws IOException {
        // Decoded PI — %20 in the URL becomes a literal space in the path segment
        ContainerRequestContext ctx = mockContextWithPath("records", "StuG_0425_160725 Ordner");
        new RecordIdentifierValidationFilter().filter(ctx);
        verify(ctx).abortWith(any(Response.class));
    }

    /**
     * @see RecordIdentifierValidationFilter#filter(ContainerRequestContext)
     * @verifies pass through when no records segment
     */
    @Test
    void filter_shouldPassThroughWhenNoRecordsSegment() throws IOException {
        ContainerRequestContext ctx = mockContextWithPath("other", "value");
        new RecordIdentifierValidationFilter().filter(ctx);
        verify(ctx, never()).abortWith(any(Response.class));
    }

    /**
     * @see RecordIdentifierValidationFilter#filter(ContainerRequestContext)
     * @verifies abort with 400 when PI contains colon
     */
    @Test
    void filter_shouldAbortWith400WhenPiContainsColon() throws IOException {
        ContainerRequestContext ctx = mockContextWithPath("records", "PI:invalid");
        new RecordIdentifierValidationFilter().filter(ctx);
        verify(ctx).abortWith(any(Response.class));
    }

    // --- helpers ---

    /**
     * Creates a mocked {@link ContainerRequestContext} with the given path segments.
     *
     * @param segments path segment strings (decoded)
     * @return mocked context
     */
    private static ContainerRequestContext mockContextWithPath(String... segments) {
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        List<PathSegment> segmentList = List.of(segments).stream()
                .map(s -> {
                    PathSegment ps = mock(PathSegment.class);
                    when(ps.getPath()).thenReturn(s);
                    return ps;
                })
                .toList();
        when(uriInfo.getPathSegments()).thenReturn(segmentList);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        return ctx;
    }
}
