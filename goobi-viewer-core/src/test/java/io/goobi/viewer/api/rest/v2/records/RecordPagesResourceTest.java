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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PAGES_COMMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.intranda.api.annotation.wa.collection.AnnotationPage;
import io.goobi.viewer.api.rest.v2.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author florian
 *
 */
class RecordPagesResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String PAGENO = "10";
    private static final String PI_ANNOTATIONS = "PI_1";
    private static final String PI_SPACE_IN_FILENAME = "4fda256e-70b3-11ea-b891-08606e6a464a";
    private static final String PAGENO_ANNOTATIONS = "1";

    @Test
    void testGetCanvas() {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI, PAGENO).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject canvas = new JSONObject(entity);
            assertEquals(url, canvas.getString("id"));
            assertEquals("Canvas", canvas.getString("type"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v2.records.RecordResource#getAnnotationsForRecord(java.lang.String)}.
     * 
     * @throws JsonProcessingException
     * @throws DAOException
     * @throws NumberFormatException
     */
    @Test
    void testGetAnnotationsForPage() throws JsonProcessingException, NumberFormatException, DAOException {
        long annoCount = DataManager.getInstance().getDao().getAnnotationCountForTarget(PI_ANNOTATIONS, Integer.parseInt(PAGENO_ANNOTATIONS));
        try (Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationPage annoPage = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(annoPage);
            assertEquals("AnnotationPage", annoPage.getType());
            assertEquals(annoCount, annoPage.getItems().size());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v2.records.RecordResource#getCommentsForRecord(java.lang.String)}.
     * 
     * @throws JsonProcessingException
     */
    @Test
    void testGetCommentsForPage() throws JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationPage annoPage = mapper.readValue(entity, AnnotationPage.class);
            assertNotNull(annoPage);
            assertEquals(3, annoPage.getItems().size());
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
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject canvas = new JSONObject(entity);
            JSONArray renderings = canvas.getJSONArray("rendering");
            assertFalse(renderings.isEmpty());
            List linkList = renderings.toList();
            Map pdfLink = renderings.toList()
                    .stream()
                    .map(Map.class::cast)
                    .filter(map -> "Text".equals(map.get("type")))
                    .findAny()
                    .orElse(null);
            assertNotNull(pdfLink, "No PDF link in canvas");
            String id = (String) pdfLink.get("id");
            Assertions.assertTrue(id.contains("IMG+20200322+144253.jpg"), "Wrong filename in " + id);
        }
    }
}
