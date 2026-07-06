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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

class RecordWebArchiveResourceApiTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String PI_NOT_FOUND = "PI_DOES_NOT_EXIST_WEBARCHIVE";

    private static final String PRIMARY_QUERY_PREFIX = "+PI_TOPSTRUCT:" + PI + " +DOCTYPE:PAGE +MIMETYPE:application/warc";
    private static final String FALLBACK_QUERY_PREFIX = "+PI:" + PI + " +MD_WEBARCHIVE_IDENTIFIER:*";

    private SolrSearchIndex mockedIndex;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        SolrSearchIndex defaultIndex = DataManager.getInstance().getSearchIndex();
        mockedIndex = Mockito.spy(defaultIndex);
        DataManager.getInstance().injectSearchIndex(mockedIndex);
    }

    private String webarchiveJsonUrl(String pi) {
        String recordPath = urls.path(ApiUrls.RECORDS_RECORD).params(pi).build();
        return StringUtils.removeEnd(recordPath, "/") + "/webarchives.json";
    }

    /**
     * @verifies return local replay json when local archive docs exist
     * @see RecordWebArchiveResource#getWebarchiveJson()
     */
    @Test
    void getWebarchiveJson_shouldReturnLocalReplayJsonWhenLocalArchiveDocsExist() throws Exception {
        SolrDocumentList localDocs = new SolrDocumentList();
        localDocs.add(new SolrDocument(Map.of(SolrConstants.FILENAME, "test.wacz")));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(PRIMARY_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(localDocs);

        try (Response response = target(webarchiveJsonUrl(PI)).request().accept(MediaType.APPLICATION_JSON).get()) {
            assertEquals(200, response.getStatus());
            JSONObject json = new JSONObject(response.readEntity(String.class));
            JSONArray resources = json.getJSONArray("resources");
            assertEquals(1, resources.length());
            assertEquals("test.wacz", resources.getJSONObject(0).getString("name"));
        }
    }

    /**
     * @verifies return 404 when neither local nor external archive docs are found
     * @see RecordWebArchiveResource#getWebarchiveJson()
     */
    @Test
    void getWebarchiveJson_shouldReturn404WhenNoLocalOrExternalDocsFound() {
        try (Response response = target(webarchiveJsonUrl(PI_NOT_FOUND)).request().accept(MediaType.APPLICATION_JSON).get()) {
            assertEquals(404, response.getStatus());
        }
    }

    /**
     * @verifies return replay json with one external resource per identifier when multiple identifiers are found
     * @see RecordWebArchiveResource#getWebarchiveJson()
     */
    @Test
    void getWebarchiveJson_shouldReturnReplayJsonWithMultipleExternalResources() throws Exception {
        SolrDocumentList fallbackDocs = new SolrDocumentList();
        fallbackDocs.add(new SolrDocument(Map.of(SolrConstants.MD_WEBARCHIVE_IDENTIFIER,
                List.of("https://example.org/archive/site1.wacz", "https://example.org/archive/site2.wacz"))));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(fallbackDocs);

        try (Response response = target(webarchiveJsonUrl(PI)).request().accept(MediaType.APPLICATION_JSON).get()) {
            assertEquals(200, response.getStatus());
            JSONObject json = new JSONObject(response.readEntity(String.class));
            JSONArray resources = json.getJSONArray("resources");
            assertEquals(2, resources.length());
            assertEquals("site1.wacz", resources.getJSONObject(0).getString("name"));
            assertEquals("https://example.org/archive/site1.wacz", resources.getJSONObject(0).getString("path"));
            assertEquals("site2.wacz", resources.getJSONObject(1).getString("name"));
            assertEquals("https://example.org/archive/site2.wacz", resources.getJSONObject(1).getString("path"));
        }
    }

    /**
     * @verifies return replay json with a single external resource when exactly one identifier is found and it is not a json url
     * @see RecordWebArchiveResource#getWebarchiveJson()
     */
    @Test
    void getWebarchiveJson_shouldReturnReplayJsonWithSingleExternalResourceWhenNotJson() throws Exception {
        SolrDocumentList fallbackDocs = new SolrDocumentList();
        fallbackDocs.add(new SolrDocument(
                Map.of(SolrConstants.MD_WEBARCHIVE_IDENTIFIER, List.of("https://example.org/archive/site1.wacz"))));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(fallbackDocs);

        try (Response response = target(webarchiveJsonUrl(PI)).request().accept(MediaType.APPLICATION_JSON).get()) {
            assertEquals(200, response.getStatus());
            JSONObject json = new JSONObject(response.readEntity(String.class));
            JSONArray resources = json.getJSONArray("resources");
            assertEquals(1, resources.length());
            assertEquals("site1.wacz", resources.getJSONObject(0).getString("name"));
        }
    }

    /**
     * @verifies return 404 when a matching document is found but every identifier fails to parse
     * @see RecordWebArchiveResource#getWebarchiveJson()
     */
    @Test
    void getWebarchiveJson_shouldReturn404WhenAllExternalIdentifiersFailToParse() throws Exception {
        SolrDocumentList fallbackDocs = new SolrDocumentList();
        fallbackDocs.add(new SolrDocument(
                Map.of(SolrConstants.MD_WEBARCHIVE_IDENTIFIER, List.of("http://exa mple.org/broken"))));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(fallbackDocs);

        try (Response response = target(webarchiveJsonUrl(PI)).request().accept(MediaType.APPLICATION_JSON).get()) {
            assertEquals(404, response.getStatus());
        }
    }
}
