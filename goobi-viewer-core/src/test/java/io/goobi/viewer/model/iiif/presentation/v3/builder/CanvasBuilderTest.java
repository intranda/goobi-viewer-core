/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.awt.Dimension;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.PagePermissions;
import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.PhysicalElement;

/**
 * @author florian
 *
 */
class CanvasBuilderTest extends AbstractSolrEnabledTest {

    ApiUrls urls = new ApiUrls("http://localhost:8080/viewer/api/v2");
    CanvasBuilder builder = new CanvasBuilder(urls, null);

    /**
     * @verifies include image
     * @see CanvasBuilder#build
     */
    @Test
    void build_shouldIncludeImage()
            throws ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException, DAOException {

        PhysicalElement element = Mockito.mock(PhysicalElement.class);
        Mockito.when(element.getPi()).thenReturn("PI_01");
        Mockito.when(element.getOrder()).thenReturn(1);
        Mockito.when(element.getOrderLabel()).thenReturn("eins");
        Mockito.when(element.isFulltextAvailable()).thenReturn(false);
        Mockito.when(element.isHasImage()).thenReturn(true);
        Mockito.when(element.getFileName()).thenReturn("00000001.tif");
        Mockito.when(element.getFilepath()).thenReturn("00000001.tif");
        Mockito.when(element.getMediaType()).thenReturn(new MimeType("image/tiff"));

        {
            Mockito.when(element.getMimeType()).thenReturn("image/tiff");
            Canvas3 canvas = builder.build(element);
            String imageId = canvas.getItems().get(0).getItems().get(0).getBody().getId().toString();
            assertEquals("https://viewer.goobi.io/api/v1/records/PI_01/files/images/00000001.tif/full/!10,11/0/default.jpg", imageId);
        }
        {
            Mockito.when(element.getMimeType()).thenReturn("image");
            Canvas3 canvas = builder.build(element);
            String imageId = canvas.getItems().get(0).getItems().get(0).getBody().getId().toString();
            assertEquals("https://viewer.goobi.io/api/v1/records/PI_01/files/images/00000001.tif/full/!10,11/0/default.jpg", imageId);
        }
        {
            Mockito.when(element.getMimeType()).thenReturn("image/png");
            Canvas3 canvas = builder.build(element);
            String imageId = canvas.getItems().get(0).getItems().get(0).getBody().getId().toString();
            assertEquals("https://viewer.goobi.io/api/v1/records/PI_01/files/images/00000001.tif/full/!10,11/0/default.jpg", imageId);
        }

    }

    /**
     * When a non-empty PagePermissions is pre-loaded onto the builder, build(page) must NOT call
     * page.isAccessPermissionImage() or page.isAccessPermissionFulltext() — the pre-fetched map
     * is used instead, eliminating per-page Solr queries in the manifest loop.
     * @verifies build for given input
     */
    @Test
    void build_shouldBuildForGivenInput()
            throws ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException, DAOException {

        PhysicalElement element = Mockito.mock(PhysicalElement.class);
        Mockito.when(element.getPi()).thenReturn("PI_01");
        Mockito.when(element.getOrder()).thenReturn(1);
        Mockito.when(element.getOrderLabel()).thenReturn("eins");
        Mockito.when(element.isFulltextAvailable()).thenReturn(false);
        Mockito.when(element.isHasImage()).thenReturn(true);
        Mockito.when(element.getFileName()).thenReturn("00000001.tif");
        Mockito.when(element.getFilepath()).thenReturn("00000001.tif");
        Mockito.when(element.getMediaType()).thenReturn(new MimeType("image/tiff"));
        Mockito.when(element.getMimeType()).thenReturn("image/tiff");

        // Inject non-empty pre-fetched permissions via package-private setter
        builder.setPagePermissions(new PagePermissions(
                Map.of(1, AccessPermission.granted()),
                Map.of(1, AccessPermission.granted()),
                Map.of(1, AccessPermission.granted())));

        builder.build(element);

        // With pre-fetched permissions, CanvasBuilder's own addImageResource must NOT call
        // page.isAccessPermissionImage() — only AbstractBuilder.getThumbnail() still calls it once.
        // Total: exactly 1 invocation instead of the 2 that occurred before this optimization.
        verify(element, Mockito.times(1)).isAccessPermissionImage();
        // Fulltext is never called (fulltext not available in this test, so the block is skipped)
        verify(element, never()).isAccessPermissionFulltext();
    }

    /**
     * @verifies return 1200 for given input
     */
    @Test
    void build_shouldReturn1200ForGivenInput()
            throws ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException, DAOException {

        PhysicalElement element = Mockito.mock(PhysicalElement.class);
        Mockito.when(element.getPi()).thenReturn("PI_01");
        Mockito.when(element.getOrder()).thenReturn(1);
        Mockito.when(element.getOrderLabel()).thenReturn("1");
        Mockito.when(element.getFileName()).thenReturn("00000001.tif");
        Mockito.when(element.getFilepath()).thenReturn("00000001.tif");
        Mockito.when(element.getMediaType()).thenReturn(new MimeType("image/tiff"));
        Mockito.when(element.getMimeType()).thenReturn("image/tiff");
        // No individual size → would normally trigger disk I/O via getImageInformation()
        Mockito.when(element.getImageWidth()).thenReturn(0);
        Mockito.when(element.getImageHeight()).thenReturn(0);

        // Inject pre-fetched dimension cache: must be used instead of disk I/O
        builder.setPageDimensions(Map.of(1, new Dimension(1200, 800)));

        // If the cache is consulted: canvas dimensions are set correctly, no ContentLibException thrown
        // (getImageInformation() would throw trying to read "00000001.tif" from disk in a test env)
        Canvas3 canvas = builder.build(element);
        assertEquals(1200, canvas.getWidth());
        assertEquals(800, canvas.getHeight());
    }

}
