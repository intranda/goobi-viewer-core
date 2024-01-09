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
package io.goobi.viewer.controller;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrConstants;

public class JsonToolsTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final String PI = PI_KLEIUNIV;

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see JsonTools#getRecordJsonObject(SolrDocument,String)
     * @verifies add all metadata
     */
    @Test
    void getRecordJsonObject_shouldAddAllMetadata() throws Exception {
        String rootUrl = "http://localhost:8080/viewer";
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + PI, 0, 1, null, null, null);
        Assertions.assertFalse(response.getResults().isEmpty(), "Required Solr document not found in index: " + PI);
        SolrDocument doc = response.getResults().get(0);
        JSONObject json = JsonTools.getRecordJsonObject(doc, rootUrl, BeanUtils.getImageDeliveryBean().getThumbs());
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.has("id"));
        Assertions.assertEquals(PI, json.get("id"));
        Assertions.assertEquals(doc.getFieldValue(SolrConstants.TITLE), json.get("title"));
        Assertions.assertEquals(doc.getFieldValue(SolrConstants.DATECREATED), json.get("dateCreated"));
        //        Assertions.assertEquals(doc.getFieldValues("MD_PERSON_UNTOKENIZED"), json.get("personList"));
        Assertions.assertEquals(doc.getFieldValue(SolrConstants.DC), json.get("collection"));
        // URL root depends on the current config state and may vary, so only compare the args
        String thumbnailUrl = (String) json.get("thumbnailUrl");
        Assertions.assertTrue(thumbnailUrl
                .contains("records/" + PI + "/files/images/" + doc.getFieldValue(SolrConstants.THUMBNAIL) + "/full/!100,120/0/default.jpg"),
                "Thumbnail url was: " + ((String) json.get("thumbnailUrl")));
        // URL root depends on the current config state and may variate, so only compare the args
        Assertions.assertTrue(((String) json.get("mediumimage"))
                .contains("records/" + PI + "/files/images/" + doc.getFieldValue(SolrConstants.THUMBNAIL) + "/full/!600,500/0/default.jpg"),
                "Image url was " + thumbnailUrl);
        Assertions.assertEquals(rootUrl + "/" + PageType.viewObject.getName() + "/" + PI + "/", json.get("url"));
        //        Assertions.assertEquals(doc.getFieldValue(SolrConstants._CALENDAR_YEAR), json.get("date"));
    }

    /**
     * @see JsonTools#formatVersionString(String)
     * @verifies format string correctly
     */
    @Test
    void formatVersionString_shouldFormatStringCorrectly() throws Exception {
        Assertions.assertEquals("goobi-viewer-core 1337 2020-06-30 abcdefg",
                JsonTools.formatVersionString(
                        "{\"application\": \"goobi-viewer-core\", \"version\": \"1337\", \"build-date\": \"2020-06-30\", \"git-revision\": \"abcdefg\"}"));
    }

    /**
     * @see JsonTools#formatVersionString(String)
     * @verifies return notAvailableKey if json invalid
     */
    @Test
    void formatVersionString_shouldReturnNotAvailableKeyIfJsonInvalid() throws Exception {
        Assertions.assertEquals("admin__dashboard_versions_not_available", JsonTools.formatVersionString("not json"));
    }

    /**
     * @see JsonTools#shortFormatVersionString(String)
     * @verifies format string correctly
     */
    @Test
    void shortFormatVersionString_shouldFormatStringCorrectly() throws Exception {
        Assertions.assertEquals("1337 (abcdefg)",
                JsonTools.shortFormatVersionString(
                        "{\"application\": \"goobi-viewer-core\", \"version\": \"1337\", \"build-date\": \"2020-06-30\", \"git-revision\": \"abcdefg\"}"));
    }

    /**
     * @see JsonTools#shortFormatVersionString(String)
     * @verifies return notAvailableKey if json invalid
     */
    @Test
    void shortFormatVersionString_shouldReturnNotAvailableKeyIfJsonInvalid() throws Exception {
        Assertions.assertEquals("admin__dashboard_versions_not_available", JsonTools.shortFormatVersionString("not json"));
    }
}
