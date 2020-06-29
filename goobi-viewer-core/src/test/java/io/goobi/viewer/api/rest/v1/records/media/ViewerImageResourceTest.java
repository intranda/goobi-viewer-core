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
package io.goobi.viewer.api.rest.v1.records.media;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.iiif.discovery.Activity;
import de.intranda.api.iiif.discovery.OrderedCollection;
import de.intranda.api.iiif.image.ImageInformation;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import io.goobi.viewer.api.rest.AbstractRestApiTest;

/**
 * @author florian
 *
 */
public class ViewerImageResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String FILENAME = "00000010";
    private static final String REGION = "full";
    private static final String SIZE = "5,5";
    private static final String ROTATION = "0";
    private static final String QUALITY = "default";
    private static final String FORMAT = "jpg";


    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    
    @Test
    public void testGetImageInformation() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_INFO).params(PI, FILENAME + ".tif").build();
        String id = urls.path(RECORDS_FILES_IMAGE).params(PI, FILENAME + ".tif").build();
        try(Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String responseString = response.readEntity(String.class);
            JSONObject info = new JSONObject(responseString);
            assertTrue(info.getString("@id").endsWith(id));
        }
    }
    
    @Test
    public void testGetImageInformationFromBaseUrl() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_FILES_IMAGE).params(PI, FILENAME).build();
        String id = urls.path(RECORDS_FILES_IMAGE).params(PI, FILENAME).build();
        try(Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String responseString = response.readEntity(String.class);
            JSONObject info = new JSONObject(responseString);
            assertTrue(info.getString("@id").endsWith(id));
        }
    }
    
    @Test
    public void testGetImage() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF).params(PI, FILENAME + ".tif", REGION, SIZE, ROTATION, QUALITY, FORMAT).build();
        try(Response response = target(url)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            String contentLocation = response.getHeaderString("Content-Location");
            byte[] entity = response.readEntity(byte[].class);
            assertEquals("Should return status 200. Error message: " + new String(entity), 200, status);
            assertEquals("file:///opt/digiverso/viewer/data/1/media/" + PI + "/" + FILENAME + ".tif", contentLocation);
            assertTrue(entity.length >= 5*5*8*3); //entity is at least as long as the image data
        }
    }
    
    @Test
    public void testGetPdf() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_PDF).params(PI, FILENAME).build();
        try(Response response = target(url)
                .request()
                .accept("application/pdf")
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            byte[] entity = response.readEntity(byte[].class);
            assertTrue(entity.length >= 5*5*8*3); //entity is at least as long as the image data
        }
    }

}
