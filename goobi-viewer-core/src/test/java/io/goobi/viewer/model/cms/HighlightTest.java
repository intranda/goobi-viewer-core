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
package io.goobi.viewer.model.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.HighlightData.ImageMode;
import io.goobi.viewer.model.cms.HighlightData.TargetType;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.solr.SolrSearchIndex;

class HighlightTest extends AbstractSolrEnabledTest {

    Configuration config = DataManager.getInstance().getConfiguration();

    /**
     * @verifies get correct image uri for uploaded image
     */
    @Test
    void getImageURI_shouldGetCorrectImageUriForUploadedImage() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        ThumbnailHandler thumbs = new ThumbnailHandler(URI.create("https:/viewer.goobi.io/api/v2/"), "/viewer/static/");

        Highlight object = new Highlight(new HighlightData(), thumbs, config);
        object.getData().setImageMode(ImageMode.UPLOADED_IMAGE);

        CMSMediaItem mediaItem = new CMSMediaItem();
        mediaItem.setFileName("image.png");
        object.setMediaItem(mediaItem);

        URI uri = object.getImageURI(1200, 1400);
        String uriPath = uri.getPath();
        assertTrue(uriPath.endsWith("api/v1/cms/media/files/image.png/full/!1200,1400/0/default.png"));
    }

    /**
     * @verifies get correct image uri for record representative
     */
    @Test
    void getImageURI_shouldGetCorrectImageUriForRecordRepresentative() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        ThumbnailHandler thumbs = new ThumbnailHandler(URI.create("https:/viewer.goobi.io/api/v2/"), "/viewer/static/");

        Highlight object = new Highlight(new HighlightData(), thumbs, config);
        object.getData().setImageMode(ImageMode.RECORD_REPRESENTATIVE);
        object.getData().setRecordIdentifier("PPN12345");

        URI uri = object.getImageURI(1200, 1400);
        String uriPath = uri.getPath();
        assertEquals("/viewer.goobi.io/api/v2/records/PPN12345/representative/full/!1200,1400/0/default.jpg", uriPath);
    }

    /**
     * When Solr is unreachable, getMetadataList() must return an empty list instead of
     * propagating IndexUnreachableException through JSF EL — which would corrupt
     * Mojarra's CompositeComponentStackManager and cause a StackOverflowError.
     * @verifies return empty list when solr unreachable
     */
    @Test
    void getMetadataList_shouldReturnEmptyListWhenSolrUnreachable() throws Exception {
        ThumbnailHandler thumbs = new ThumbnailHandler(URI.create("https://viewer.goobi.io/api/v2/"), "/viewer/static/");
        Configuration mockConfig = mock(Configuration.class);

        HighlightData data = new HighlightData();
        data.setTargetType(TargetType.RECORD);
        data.setRecordIdentifier("PPN123");
        Highlight highlight = new Highlight(data, thumbs, mockConfig);

        SolrSearchIndex mockIndex = mock(SolrSearchIndex.class);
        when(mockIndex.getDocumentByPI("PPN123")).thenThrow(new IndexUnreachableException("Solr unreachable"));

        DataManager mockDm = mock(DataManager.class);
        when(mockDm.getSearchIndex()).thenReturn(mockIndex);

        try (MockedStatic<DataManager> dmStatic = mockStatic(DataManager.class);
                MockedStatic<BeanUtils> buStatic = mockStatic(BeanUtils.class)) {
            dmStatic.when(DataManager::getInstance).thenReturn(mockDm);
            buStatic.when(BeanUtils::getLocale).thenReturn(Locale.GERMAN);

            List<Metadata> result = highlight.getMetadataList();
            assertTrue(result.isEmpty(), "getMetadataList() must return empty list when Solr is unreachable");
        }
    }

    /**
     * @verifies always be active
     * @see Highlight#isPresent
     */
    @Test
    void isPresent_shouldAlwaysBeActive() {
        Highlight object = new Highlight(new HighlightData());
        object.getData().setDateStart(null);
        object.getData().setDateEnd(null);
        assertFalse(object.isPast());
        assertTrue(object.isPresent());
        assertFalse(object.isFuture());
    }

    /**
     * @verifies return true for given input
     */
    @Test
    void isPresent_shouldReturnTrueForGivenInput() {
        LocalDateTime now = LocalDate.of(2023, 4, 15).atStartOfDay();
        Highlight object = new Highlight(new HighlightData());

        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertFalse(object.isPast(now));
        assertTrue(object.isCurrent(now));
        assertFalse(object.isFuture(now));

        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(null);
        assertFalse(object.isPast(now));
        assertTrue(object.isCurrent(now));
        assertFalse(object.isFuture(now));

        object.getData().setDateStart(null);
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertFalse(object.isPast(now));
        assertFalse(object.isPast(now));
        assertTrue(object.isCurrent(now));
        assertFalse(object.isFuture(now));
    }

    /**
     * @verifies return true for given input
     */
    @Test
    void isFuture_shouldReturnTrueForGivenInput() {
        LocalDateTime now = LocalDate.of(2023, 3, 15).atStartOfDay();
        Highlight object = new Highlight(new HighlightData());

        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertFalse(object.isPast(now));
        assertFalse(object.isCurrent(now));
        assertTrue(object.isFuture(now));

        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(null);
        assertFalse(object.isPast(now));
        assertFalse(object.isCurrent(now));
        assertTrue(object.isFuture(now));

    }

    /**
     * @verifies return true for given input
     */
    @Test
    void isPast_shouldReturnTrueForGivenInput() {
        LocalDateTime now = LocalDate.of(2023, 5, 15).atStartOfDay();
        Highlight object = new Highlight(new HighlightData());

        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertTrue(object.isPast(now));
        assertFalse(object.isCurrent(now));
        assertFalse(object.isFuture(now));

        object.getData().setDateStart(null);
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertTrue(object.isPast(now));
        assertFalse(object.isCurrent(now));
        assertFalse(object.isFuture(now));

    }

}
