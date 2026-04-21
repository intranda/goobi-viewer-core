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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.PagePermissions;
import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author Florian
 *
 */
class SequenceBuilderTest extends AbstractDatabaseAndSolrEnabledTest {

    public static final String PI = PI_KLEIUNIV;
    public static final int ORDER = 1;

    @Test
    void testAddOtherContent() throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException, ContentNotFoundException, IOException {

        ManifestBuilder manifestBuilder = new ManifestBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));
        SequenceBuilder sequenceBuilder = new SequenceBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));

        List<StructElement> docs = manifestBuilder.getDocumentWithChildren(PI);
        if (docs.isEmpty()) {
            throw new ContentNotFoundException("No document found for pi " + PI);
        }
        StructElement mainDoc = docs.get(0);
        IPresentationModelElement manifest = manifestBuilder.generateManifest(mainDoc, Collections.emptyList());

        PhysicalElement page = sequenceBuilder.getPage(mainDoc, ORDER);

        Canvas2 canvas = sequenceBuilder.generateCanvas(mainDoc.getPi(), page);

        Map<AnnotationType, AnnotationList> annoMap = sequenceBuilder.addOtherContent(mainDoc, page, canvas, true);
        AnnotationList fulltext = annoMap.get(AnnotationType.FULLTEXT);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        ObjectWriter writer = mapper.writer().forType(AnnotationList.class);
        String json = writer.writeValueAsString(fulltext);
        Assertions.assertTrue(StringUtils.isNotBlank(json));
    }

    /**
     * When a non-empty PagePermissions is stored on the builder before generateCanvas is called,
     * page.isAccessPermissionImage() must NOT be invoked — the pre-fetched map is used instead.
     */
    @Test
    void testGenerateCanvas_usesPrefetchedPermissionsWithoutCallingPageMethod()
            throws URISyntaxException, ViewerConfigurationException, IndexUnreachableException,
            PresentationException, DAOException {

        SequenceBuilder sequenceBuilder = new SequenceBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));

        PhysicalElement page = Mockito.mock(PhysicalElement.class);
        Mockito.when(page.getPi()).thenReturn(PI);
        Mockito.when(page.getOrder()).thenReturn(ORDER);
        Mockito.when(page.getOrderLabel()).thenReturn(String.valueOf(ORDER));
        Mockito.when(page.getFileName()).thenReturn("00000001.tif");
        Mockito.when(page.getFilepath()).thenReturn("00000001.tif");
        Mockito.when(page.getMediaType()).thenReturn(new MimeType("image/tiff"));
        Mockito.when(page.getMimeType()).thenReturn("image/tiff");
        Mockito.when(page.getDisplayMimeType()).thenReturn("image/tiff");
        // Provide non-zero dimensions so getSize() skips the imageDelivery fallback
        Mockito.when(page.getImageWidth()).thenReturn(1000);
        Mockito.when(page.getImageHeight()).thenReturn(800);
        Mockito.when(page.getThumbnailUrl()).thenReturn(
                "https://viewer.goobi.io/api/v1/records/" + PI + "/files/images/00000001.tif/full/80,/0/default.jpg");

        // Inject non-empty pre-fetched permissions via package-private setter.
        // After PagePermissions was extended to 6 privilege maps (image, thumbnail, zoom,
        // download, fulltext, pdf), only image/fulltext/pdf are relevant here; the other
        // three stay empty because this test only verifies image-access behaviour.
        sequenceBuilder.setPagePermissions(new PagePermissions(
                Map.of(ORDER, AccessPermission.granted()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of(ORDER, AccessPermission.granted()),
                Map.of(ORDER, AccessPermission.granted())));

        sequenceBuilder.generateCanvas(PI, page);

        // With pre-fetched permissions, the per-page access-check method must NOT be called
        verify(page, never()).isAccessPermissionImage();
    }

    /**
     * @see SequenceBuilder#getSize(PhysicalElement)
     * @verifies use prefetched dimension cache without calling ImageHandler when dimensions cached
     */
    @Test
    void testGenerateCanvas_usesDimensionCacheWithoutCallingImageHandler()
            throws URISyntaxException, ViewerConfigurationException, IndexUnreachableException,
            PresentationException, DAOException {

        SequenceBuilder sequenceBuilder = new SequenceBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));

        PhysicalElement page = Mockito.mock(PhysicalElement.class);
        Mockito.when(page.getPi()).thenReturn("PI_01");
        Mockito.when(page.getOrder()).thenReturn(1);
        Mockito.when(page.getOrderLabel()).thenReturn("1");
        Mockito.when(page.getFileName()).thenReturn("00000001.tif");
        Mockito.when(page.getMediaType()).thenReturn(new MimeType("image/tiff"));
        Mockito.when(page.getDisplayMimeType()).thenReturn("image/tiff");
        Mockito.when(page.getThumbnailUrl()).thenReturn(
                "https://viewer.goobi.io/api/v1/records/PI_01/files/images/00000001.tif/full/80,/0/default.jpg");
        // No individual size → would normally trigger disk I/O via getImageInformation()
        Mockito.when(page.hasIndividualSize()).thenReturn(false);

        // Inject pre-fetched dimension cache: must be used instead of disk I/O
        sequenceBuilder.setPageDimensions(Map.of(1, new Dimension(1200, 800)));

        // If the cache is consulted: canvas dimensions are set correctly, no ContentLibException thrown
        // (getImageInformation() would fail trying to read "00000001.tif" from disk in a test env)
        Canvas2 canvas = sequenceBuilder.generateCanvas("PI_01", page);
        assertEquals(1200, canvas.getWidth());
        assertEquals(800, canvas.getHeight());
    }

}
