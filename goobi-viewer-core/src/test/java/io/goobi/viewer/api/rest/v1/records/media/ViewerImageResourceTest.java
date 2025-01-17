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
package io.goobi.viewer.api.rest.v1.records.media;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE_IIIF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE_INFO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE_PDF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.LicenseType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author florian
 *
 */
class ViewerImageResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String FILENAME = "00000010";
    private static final String PI_SPECIAL_CHARACTERS = "ARVIErdm5";
    private static final String FILENAME_SPECIAL_CHARACTERS = "erdmagnetisches+observatorium+vi_blatt_5.tif";
    private static final String REGION = "full";
    private static final String SIZE = "5,5";
    private static final String ROTATION = "0";
    private static final String QUALITY = "default";
    private static final String FORMAT = "jpg";

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        DataManager.getInstance()
                .injectConfiguration(new Configuration(new File("src/test/resources/config_viewer_no_local_access.test.xml").getAbsolutePath()));
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testGetImageInformation() {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_INFO).params(PI, FILENAME + ".tif").build();
        String id = urls.path(RECORDS_FILES_IMAGE).params(PI, FILENAME + ".tif").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String responseString = response.readEntity(String.class);
            JSONObject info = new JSONObject(responseString);
            assertTrue(info.getString("@id").endsWith(id));
        }
    }

    @Test
    void testGetImageInformationFromBaseUrl() {
        String url = urls.path(RECORDS_FILES_IMAGE).params(PI, FILENAME + ".tif").build();
        String id = urls.path(RECORDS_FILES_IMAGE).params(PI, FILENAME + ".tif").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String responseString = response.readEntity(String.class);
            JSONObject info = new JSONObject(responseString);
            assertTrue(info.getString("@id").endsWith(id), "@id should end with '" + id + " but was: " + info.getString("@id"));
        }
    }

    @Test
    void testGetImageInformationSpecialCharacters() {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_INFO).params(PI_SPECIAL_CHARACTERS, FILENAME_SPECIAL_CHARACTERS).build();
        String id = urls.path(RECORDS_FILES_IMAGE).params(PI_SPECIAL_CHARACTERS, FILENAME_SPECIAL_CHARACTERS).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String responseString = response.readEntity(String.class);
            JSONObject info = new JSONObject(responseString);
            assertTrue(info.getString("@id").endsWith(id.replace(" ", "+")));
        }
    }

    @Test
    void testGetImageSpecialCharacters() {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                .params(PI_SPECIAL_CHARACTERS, FILENAME_SPECIAL_CHARACTERS, REGION, SIZE, ROTATION, QUALITY, FORMAT)
                .build();
        try (Response response = target(url)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            String contentLocation = response.getHeaderString("Content-Location");
            byte[] entity = response.readEntity(byte[].class);
            assertEquals(200, status, "Should return status 200. Error message: " + new String(entity));
            assertEquals("file:///opt/digiverso/viewer/data/3/media/" + PI_SPECIAL_CHARACTERS.replace("+", "%20") + "/"
                    + FILENAME_SPECIAL_CHARACTERS.replace("+", "%20"), contentLocation);
            assertTrue(entity.length >= 5 * 5 * 8 * 3); //entity is at least as long as the image data
        }
    }

    @Test
    void testGetImage() {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                .params(PI, FILENAME + ".tif", REGION, SIZE, ROTATION, QUALITY, FORMAT)
                .build();
        try (Response response = target(url)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            String contentLocation = response.getHeaderString("Content-Location");
            byte[] entity = response.readEntity(byte[].class);
            assertEquals(200, status, "Should return status 200. Error message: " + new String(entity));
            assertEquals("file:///opt/digiverso/viewer/data/1/media/" + PI + "/" + FILENAME + ".tif", contentLocation);
            assertTrue(entity.length >= 5 * 5 * 8 * 3); //entity is at least as long as the image data
        }
    }

    /**
     * This originally requested the REST-API like the other tests in this class. But for still unknown reasons (presumably with the rest-server used
     * for the tests), the test reliably fails in some enviroments. It now tests the underlying java-methods of the endpoint directly
     * 
     * @throws IOException
     * @throws WebApplicationException
     * @throws ContentLibException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    @Test
    void testGetPdf() throws IOException, WebApplicationException, ContentLibException, PresentationException, IndexUnreachableException {
        String url = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_PDF).params(PI, FILENAME).build();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn(url);
        Mockito.when(request.getAttribute("pi")).thenReturn(PI);
        Mockito.when(request.getAttribute("filename")).thenReturn(FILENAME + ".tif");
        Mockito.when(request.getParameterMap())
                .thenReturn(Map.of("imageSource",
                        new String[] { DataFileTools.getDataRepositoryPathForRecord(PI) + "/"
                                + DataManager.getInstance().getConfiguration().getMediaFolder() }));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);

        RecordsFilesImageResource resource =
                new RecordsFilesImageResource(context, request, response, urls, PI, FILENAME + ".tif", ContentServerCacheManager.getInstance());
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            resource.getPdf().write(baos);
            // System.out.println("Written byte array stream of size " + baos.size());
            assertTrue(baos.size() > 0);
        }
    }

    @Test
    void testGetImageClosedLicense() throws DAOException {
        LicenseType licenseType = new LicenseType("pdf_locked");
        licenseType.setOpenAccess(false);
        licenseType.setPrivileges(Collections.emptySet());
        DataManager.getInstance().getDao().addLicenseType(licenseType);

        String urlImage = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                .params("PPNuag_foto_v4", "00000001.tif", REGION, "!5,5", ROTATION, QUALITY, FORMAT)
                .build();
        String urlThumb = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                .params("PPNuag_foto_v4", "00000001.tif", REGION, "!1,1", ROTATION, QUALITY, FORMAT)
                .build();

        try (Response response = target(urlThumb)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            byte[] entity = response.readEntity(byte[].class);
            assertEquals(403, status, "Should return status 403. Error message: " + new String(entity));
        }

        try (Response response = target(urlImage)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            byte[] entity = response.readEntity(byte[].class);
            assertEquals(403, status, "Should return status 403. Error message: " + new String(entity));
        }

        DataManager.getInstance().getDao().deleteLicenseType(licenseType);
    }

    @Test
    void testGetImageOpenLicense() throws DAOException {
        LicenseType licenseType = new LicenseType("pdf_locked");
        licenseType.setOpenAccess(true);
        DataManager.getInstance().getDao().addLicenseType(licenseType);

        String urlImage = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                .params("PPNuag_foto_v4", "00000001.tif", REGION, "!5,5", ROTATION, QUALITY, FORMAT)
                .build();
        String urlThumb = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                .params("PPNuag_foto_v4", "00000001.tif", REGION, "!1,1", ROTATION, QUALITY, FORMAT)
                .build();

        try (Response response = target(urlThumb)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            byte[] entity = response.readEntity(byte[].class);
            assertEquals(200, status, "Should return status 200. Error message: " + new String(entity));
        }

        try (Response response = target(urlImage)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            byte[] entity = response.readEntity(byte[].class);
            assertEquals(200, status, "Should return status 200. Error message: " + new String(entity));
        }

        DataManager.getInstance().getDao().deleteLicenseType(licenseType);
    }

    @Test
    void testGetImageThumbnailLicense() throws DAOException {
        LicenseType licenseType = new LicenseType("pdf_locked");
        licenseType.setOpenAccess(false);
        licenseType.setPrivileges(Collections.singleton(LicenseType.PRIV_VIEW_THUMBNAILS));
        DataManager.getInstance().getDao().addLicenseType(licenseType);

        String urlImage = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                .params("PPNuag_foto_v4", "00000001.tif", REGION, "!5,5", ROTATION, QUALITY, FORMAT)
                .build();
        String urlThumb = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                .params("PPNuag_foto_v4", "00000001.tif", REGION, "!1,1", ROTATION, QUALITY, FORMAT)
                .build();

        try (Response response = target(urlThumb)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            byte[] entity = response.readEntity(byte[].class);
            assertEquals(200, status, "Should return status 200. Error message: " + new String(entity));
        }

        try (Response response = target(urlImage)
                .request()
                .accept("image")
                .get()) {
            int status = response.getStatus();
            byte[] entity = response.readEntity(byte[].class);
            assertEquals(403, status, "Should return status 403. Error message: " + new String(entity));
        }

        DataManager.getInstance().getDao().deleteLicenseType(licenseType);
    }
}
