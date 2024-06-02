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
package io.goobi.viewer.api.rest.v1.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.imaging.IIIFUrlHandler;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.model.cms.media.CMSMediaItem;

/**
 * @author florian
 *
 */
class CMSMediaImageResourceTest extends AbstractRestApiTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testCallImageUrlCapitalSuffix() {
        String filename = "image4.JPG";
        String url = urls.path(ApiUrls.CMS_MEDIA, ApiUrls.CMS_MEDIA_FILES_FILE).params(filename).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            //since no file image4.JPG exists, 404 is returned. But that is ok as long as the method was called (otherwise 405 would be thrown)
            assertEquals(404, response.getStatus(), "Should return status 404; answer; " + entity);
        }
    }

    @Test
    void testCallImageUrlForGif() {
        String filename = "lorelai.gif";
        String url = "https://viewer.goobi.io/api/v1/cms/media/files/" + filename;
        ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(url));
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        CMSMediaImageResource resource = new CMSMediaImageResource(context, request, response, urls, filename);
        String resourceURI = resource.getResourceURI().toString();
        assertEquals(url, resourceURI);
    }

    @Test
    void testUrlencoding() throws UnsupportedEncodingException {
        String filename = "ä (b) c";
        String filenameEnc = URLEncoder.encode(filename, "utf-8");
        CMSMediaItem media = new CMSMediaItem();
        media.setFileName(filename);
        String apiUrl = urls.getApiUrl();
        ThumbnailHandler thumbs = new ThumbnailHandler(new IIIFUrlHandler(urls), null);
        String imageUrl = thumbs.getThumbnailUrl(media, 100, 200);
        //System.out.println(imageUrl);
        assertTrue(imageUrl.startsWith(apiUrl));
        assertTrue(imageUrl.contains(filenameEnc), imageUrl + " should contain " + filenameEnc);

        ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(imageUrl));
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        CMSMediaImageResource resource = new CMSMediaImageResource(context, request, response, urls, filename);
        String resourceURI = resource.getResourceURI().toString();
        assertTrue(resourceURI.startsWith(apiUrl));
        assertTrue(resourceURI.contains(filenameEnc), resourceURI + " should contain " + filenameEnc);
    }
}
