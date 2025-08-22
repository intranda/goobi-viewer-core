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

import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PhysicalElement;

/**
 * @author florian
 *
 */
class CanvasBuilderTest extends AbstractSolrEnabledTest {

    ApiUrls urls = new ApiUrls("http://localhost:8080/viewer/api/v2");
    CanvasBuilder builder = new CanvasBuilder(urls);

    @Test
    void test_build_shouldIncludeImage()
            throws ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException, DAOException {

        PhysicalElement element = Mockito.mock(PhysicalElement.class);
        Mockito.when(element.getPi()).thenReturn("PI_01");
        Mockito.when(element.getOrder()).thenReturn(1);
        Mockito.when(element.getOrderLabel()).thenReturn("eins");
        Mockito.when(element.isFulltextAvailable()).thenReturn(false);
        Mockito.when(element.isHasImage()).thenReturn(true);
        Mockito.when(element.getFileName()).thenReturn("00000001.tif");
        Mockito.when(element.getFilepath()).thenReturn("00000001.tif");
        Mockito.when(element.getBaseMimeType()).thenReturn(BaseMimeType.IMAGE);

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

}
