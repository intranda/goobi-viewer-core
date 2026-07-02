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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_COMMENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_NER_TAGS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_SEQUENCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Sequence;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
class RecordPageResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String PAGENO = "10";
    private static final String PI_ANNOTATIONS = "PI_1";
    private static final String PI_SPACE_IN_FILENAME = "4fda256e-70b3-11ea-b891-08606e6a464a";
    private static final String PAGENO_ANNOTATIONS = "1";
    // ALTO data exists both in the testing Solr index and in src/test/resources/data/viewer/data/1/alto/PPN648829383/
    private static final String PI_WITH_ALTO = "PPN648829383";
    private static final String PAGENO_WITH_ALTO = "1";
    // Indexed without FILENAME_ALTO, but with plain fulltext in src/test/resources/data/viewer/data/1/fulltext/PPN517154005/
    private static final String PI_WITHOUT_ALTO = "PPN517154005";

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
     * @verifies return one pages entry with three tags
     */
    @Test
    void getNERTags_shouldReturnOnePagesEntryWithThreeTags() {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_NER_TAGS).params(PI, PAGENO).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject doc = new JSONObject(entity);
            assertNotNull(doc.getJSONArray("pages"));
            assertEquals(1, doc.getJSONArray("pages").length());
            assertEquals(3, doc.getJSONArray("pages").getJSONObject(0).getJSONArray("tags").length());
        }
    }

    /**
     * @verifies return non null result
     * @see RecordPageResource#getSequence
     */
    @Test
    void getSequence_shouldReturnNonNullResult() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_SEQUENCE).params(PI).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            Sequence sequence = mapper.readValue(entity, Sequence.class);
            assertEquals(URI.create(url), sequence.getId());
            assertEquals(322, sequence.getCanvases().size());
        }
    }

    /**
     * @verifies return non null result
     * @see RecordPageResource#getCanvas
     */
    @Test
    void getCanvas_shouldReturnNonNullResult() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI, PAGENO).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            Canvas2 canvas = mapper.readValue(entity, Canvas2.class);
            assertEquals(URI.create(url), canvas.getId());
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordPageResource#getAnnotationsForRecord(java.lang.String)}.
     *
     * @throws JsonProcessingException
     * @throws JsonMappingException
     * @verifies return empty annotation collection when none indexed
     */
    @Test
    void getAnnotationsForRecord_shouldReturnEmptyAnnotationCollectionWhenNoneIndexed() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationCollection collection = mapper.readValue(entity, AnnotationCollection.class);
            assertNotNull(collection);
            assertEquals(0, collection.getTotalItems()); //No annotations indexed
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordPageResource#getCommentsForPage()}.
     *
     * @throws JsonProcessingException
     * @throws JsonMappingException
     * @verifies return non null result
     * @see RecordPageResource#getCommentsForPage
     */
    @Test
    void getCommentsForPage_shouldReturnNonNullResult() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS).params(PI_ANNOTATIONS, PAGENO_ANNOTATIONS).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            AnnotationList collection = mapper.readValue(entity, AnnotationList.class);
            assertNotNull(collection);
            assertEquals(3, collection.getResources().size());
        }
    }
    

    /**
     * @verifies escape spaces in filenames within rendering links
     */
    @Test
    void getCanvas_shouldEscapeSpacesInFilenamesWithinRenderingLinks() {
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.viewer[@enabled]", true);
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer());
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.pdf[@enabled]", true);
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF());

        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(PI_SPACE_IN_FILENAME, "1").build();
        try (Response response = target(url)
                .request()
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
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
                    .map(Map.class::cast)
                    .filter(map -> "dcTypes:Image".equals(map.get("@type")))
                    .findAny()
                    .orElse(null);
            assertNotNull(pdfLink, "No PDF link in canvas");
            String id = (String) pdfLink.get("@id");
            Assertions.assertTrue(id.contains("IMG+20200322+144253.jpg"), "Wrong filename in " + id);
        }
    }

    /**
     * Requests the page text annotation list and returns the number of contained annotation resources.
     *
     * @param pi record identifier
     * @param pageNo page number
     * @param granularity granularity query parameter value, or null to omit the parameter
     * @return number of resources in the returned annotation list
     */
    private int countTextAnnotations(String pi, String pageNo, String granularity) {
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_TEXT).params(pi, pageNo).build();
        WebTarget textTarget = target(url);
        if (granularity != null) {
            textTarget = textTarget.queryParam("granularity", granularity);
        }
        try (Response response = textTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONArray resources = new JSONObject(entity).optJSONArray("resources");
            return resources == null ? 0 : resources.length();
        }
    }

    /**
     * The default (line) granularity must return line-level annotations for a page with ALTO data,
     * so existing callers are unaffected by the new granularity parameter.
     *
     * @verifies return line annotations with default granularity
     * @see RecordPageResource#getTextForPage
     */
    @Test
    void getTextForPage_defaultGranularity_shouldReturnLineAnnotations() {
        int lineCount = countTextAnnotations(PI_WITH_ALTO, PAGENO_WITH_ALTO, null);
        assertTrue(lineCount > 0, "Default granularity should return line-level annotations");
    }

    /**
     * {@code granularity=word} must return word-level annotations, i.e. more annotations than the
     * line-level response for the same page.
     *
     * @verifies return word annotations with word granularity
     * @see RecordPageResource#getTextForPage
     */
    @Test
    void getTextForPage_wordGranularity_shouldReturnWordAnnotations() {
        int lineCount = countTextAnnotations(PI_WITH_ALTO, PAGENO_WITH_ALTO, null);
        int wordCount = countTextAnnotations(PI_WITH_ALTO, PAGENO_WITH_ALTO, "word");
        assertTrue(lineCount > 0, "Line-level response should contain annotations");
        assertTrue(wordCount > lineCount,
                "Word granularity should return more annotations than line granularity (got " + wordCount + " vs " + lineCount + ")");
    }

    /**
     * The granularity parameter value must be case-insensitive.
     *
     * @verifies treat granularity value case-insensitively
     * @see RecordPageResource#getTextForPage
     */
    @Test
    void getTextForPage_wordGranularity_shouldBeCaseInsensitive() {
        int wordCount = countTextAnnotations(PI_WITH_ALTO, PAGENO_WITH_ALTO, "word");
        int upperCaseCount = countTextAnnotations(PI_WITH_ALTO, PAGENO_WITH_ALTO, "WORD");
        assertTrue(wordCount > 0, "Word-level response should contain annotations");
        assertEquals(wordCount, upperCaseCount, "granularity=WORD should behave like granularity=word");
    }

    /**
     * An unknown granularity value (neither "line" nor "word") must be silently treated as "line".
     *
     * @verifies treat unknown granularity values as line
     * @see RecordPageResource#getTextForPage
     */
    @Test
    void getTextForPage_unknownGranularity_shouldFallBackToLine() {
        int lineCount = countTextAnnotations(PI_WITH_ALTO, PAGENO_WITH_ALTO, "line");
        int unknownCount = countTextAnnotations(PI_WITH_ALTO, PAGENO_WITH_ALTO, "paragraph");
        assertEquals(lineCount, unknownCount, "Unknown granularity should fall back to line granularity");
    }

    /**
     * For a page without an indexed ALTO file, {@code granularity=word} must fall back to the
     * default (plain fulltext) response instead of failing or returning an empty list.
     *
     * @verifies fall back to default response for word granularity without alto
     * @see RecordPageResource#getTextForPage
     */
    @Test
    void getTextForPage_wordGranularityWithoutAlto_shouldFallBackToDefaultResponse() {
        int defaultCount = countTextAnnotations(PI_WITHOUT_ALTO, "1", null);
        int wordCount = countTextAnnotations(PI_WITHOUT_ALTO, "1", "word");
        assertEquals(defaultCount, wordCount, "granularity=word without ALTO should return the default response");
    }

    /**
     * A PI containing illegal characters (e.g. carriage return %0D, unicode garbage) must return
     * HTTP 400, not 500. Before the fix, RecordPageResource did not validate the PI in its
     * constructor, so invalid PIs could reach Solr and cause NPEs or unexpected exceptions.
     * @verifies return http 400 when pi contains illegal characters
     */
    @Test
    void getAnnotationsForRecord_shouldReturnHttp400WhenPiContainsIllegalCharacters() {
        // Use a PI that contains a colon, which is blocked by PIValidator
        String url = urls.path(RECORDS_PAGES, RECORDS_PAGES_ANNOTATIONS).params("invalid:pi", 1).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(400, response.getStatus(), "PI with illegal characters should return HTTP 400");
        }
    }
}
