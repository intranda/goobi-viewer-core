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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_SOURCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TEI;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
class RecordFileResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String PI_SPACE_IN_FILENAME = "ARVIErdm5";
    private static final String FILENAME = "00000010";

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getAlto(java.lang.String)}.
     *
     * @throws IOException
     * @throws JDOMException
     */
    @Test
    void testGetAlto() throws JDOMException, IOException {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_ALTO).params(PI, FILENAME + ".xml").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getPlaintext(java.lang.String)}.
     *
     * @throws IOException
     * @throws JDOMException
     */
    @Test
    void testGetPlaintext() throws JDOMException, IOException {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT).params(PI, FILENAME + ".txt").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getTEI(java.lang.String)}.
     *
     * @throws IOException
     * @throws JDOMException
     */
    @Test
    void testGetTEI() throws JDOMException, IOException {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_TEI).params(PI, FILENAME + ".xml").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }

    @Test
    void testGetSourceFile() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "text.txt").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
            String contentType = response.getHeaderString("Content-Type");
            String entity = response.readEntity(String.class);
            assertEquals("text/plain", contentType);
            assertEquals("apples", entity.trim());
        }
    }

    @Test
    void testGetMissingSourceFile() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "bla.txt").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(404, response.getStatus(), response.getStatusInfo().getReasonPhrase());
        }
    }

    @Test
    void testGetSourceFilePathTraversalAttack() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "/../../../../..//etc/passwd").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(404, response.getStatus(), "Should return status 404");
        }
    }

    @Test
    void testEscapeFilenamesInUrls() {
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.viewer[@enabled]", true);
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer());
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.pdf[@enabled]", true);
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF());

        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI_SPACE_IN_FILENAME, "1").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject canvas = new JSONObject(entity);
            JSONArray renderings = null;
            try {
                renderings = canvas.getJSONArray("rendering");
            } catch (JSONException e) {
                // Fallback for when "rendering" is not an array
                JSONObject rendering = canvas.getJSONObject("rendering");
                if (rendering != null) {
                    renderings = new JSONArray();
                    renderings.put(rendering);
                }
            }
            assertNotNull(renderings);
            assertFalse(renderings.isEmpty());
            Map pdfLink = renderings.toList()
                    .stream()
                    .map(object -> (Map) object)
                    .filter(map -> "dcTypes:Image".equals(map.get("@type")))
                    .findAny()
                    .orElse(null);
            assertNotNull(pdfLink, "No PDF link in canvas");
            String id = (String) pdfLink.get("@id");
            Assertions.assertTrue(id.contains("erdmagnetisches+observatorium+vi_blatt_5.tif"), "Wrong filename in " + id);
        }
    }
}
