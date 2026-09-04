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
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

class MatrixParameterRequestFilterTest {

    /**
     * @see MatrixParameterRequestFilter#filter(ContainerRequestContext)
     * @verifies pass through path without matrix parameters
     */
    @Test
    void filter_shouldPassThroughPathWithoutMatrixParameters() throws IOException {
        ContainerRequestContext ctx = mockContextWithSegments(
                segment("records", Map.of()),
                segment("AC03456323", Map.of()),
                segment("files", Map.of()),
                segment("images", Map.of()),
                segment("00000001.tif", Map.of()),
                segment("full", Map.of()),
                segment("512,512", Map.of()),
                segment("0", Map.of()),
                segment("default.jpg", Map.of()));
        new MatrixParameterRequestFilter().filter(ctx);
        verify(ctx, never()).abortWith(any(Response.class));
    }

    /**
     * @see MatrixParameterRequestFilter#filter(ContainerRequestContext)
     * @verifies abort with 400 when a trailing segment has a matrix parameter
     */
    @Test
    void filter_shouldAbortWith400WhenTrailingSegmentHasMatrixParameter() throws IOException {
        // Segment ";LbNtg" parses to path="" with matrix parameter LbNtg
        ContainerRequestContext ctx = mockContextWithSegments(
                segment("records", Map.of()),
                segment("AC03456323", Map.of()),
                segment("files", Map.of()),
                segment("images", Map.of()),
                segment("00000001.tif", Map.of()),
                segment("", Map.of("LbNtg", List.of(""))),
                segment("512,512", Map.of()),
                segment("0", Map.of()),
                segment("default.jpg", Map.of()));
        new MatrixParameterRequestFilter().filter(ctx);
        verify(ctx).abortWith(any(Response.class));
    }

    /**
     * @see MatrixParameterRequestFilter#filter(ContainerRequestContext)
     * @verifies abort with 400 when a leading segment has a matrix parameter
     */
    @Test
    void filter_shouldAbortWith400WhenLeadingSegmentHasMatrixParameter() throws IOException {
        // A matrix parameter on the {filename} segment - outside the trailing
        // region/size/rotation/quality.format tail - must be caught too.
        ContainerRequestContext ctx = mockContextWithSegments(
                segment("records", Map.of()),
                segment("AC03456323", Map.of()),
                segment("files", Map.of()),
                segment("images", Map.of()),
                segment("", Map.of("evil.tif", List.of(""))),
                segment("full", Map.of()),
                segment("512,512", Map.of()),
                segment("0", Map.of()),
                segment("default.jpg", Map.of()));
        new MatrixParameterRequestFilter().filter(ctx);
        verify(ctx).abortWith(any(Response.class));
    }

    /**
     * @see MatrixParameterRequestFilter#filter(ContainerRequestContext)
     * @verifies pass through empty path
     */
    @Test
    void filter_shouldPassThroughEmptyPath() throws IOException {
        ContainerRequestContext ctx = mockContextWithSegments();
        new MatrixParameterRequestFilter().filter(ctx);
        verify(ctx, never()).abortWith(any(Response.class));
    }

    // --- helpers ---

    private record SegmentSpec(String path, Map<String, List<String>> matrixParams) {
    }

    private static SegmentSpec segment(String path, Map<String, List<String>> matrixParams) {
        return new SegmentSpec(path, matrixParams);
    }

    /**
     * Creates a mocked {@link ContainerRequestContext} with the given path segments.
     *
     * @param specs path segment specs (decoded path plus matrix parameters, as returned by {@link PathSegment})
     * @return mocked context
     */
    private static ContainerRequestContext mockContextWithSegments(SegmentSpec... specs) {
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        List<PathSegment> segmentList = List.of(specs).stream()
                .map(spec -> {
                    PathSegment ps = mock(PathSegment.class);
                    when(ps.getPath()).thenReturn(spec.path());
                    MultivaluedMap<String, String> matrix = new MultivaluedHashMap<>();
                    spec.matrixParams().forEach(matrix::addAll);
                    when(ps.getMatrixParameters()).thenReturn(matrix);
                    return ps;
                })
                .toList();
        when(uriInfo.getPathSegments()).thenReturn(segmentList);
        when(uriInfo.getPath()).thenReturn("");
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        return ctx;
    }
}
