package io.goobi.viewer.controller;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.viewer.PageType;

public class JsonToolsTest extends AbstractSolrEnabledTest {

    private static final String PI = PI_KLEIUNIV;

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractSolrEnabledTest.setUpClass();
    }

    /**
     * @see JsonTools#getRecordJsonObject(SolrDocument,String)
     * @verifies add all metadata
     */
    @Test
    public void getRecordJsonObject_shouldAddAllMetadata() throws Exception {
        String rootUrl = "http://localhost:8080/viewer";
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + PI, 0, 1, null, null, null);
        Assert.assertFalse("Required Solr document not found in index: " + PI, response.getResults().isEmpty());
        SolrDocument doc = response.getResults().get(0);
        JSONObject json = JsonTools.getRecordJsonObject(doc, rootUrl, null);
        Assert.assertNotNull(json);
        Assert.assertTrue(json.has("id"));
        Assert.assertEquals(PI, json.get("id"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.TITLE), json.get("title"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.DATECREATED), json.get("dateCreated"));
        //        Assert.assertEquals(doc.getFieldValues("MD_PERSON_UNTOKENIZED"), json.get("personList"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.DC), json.get("collection"));
        // URL root depends on the current config state and may variate, so only compare the args
        Assert.assertTrue("Thumbnail url was " + ((String) json.get("thumbnailUrl")), ((String) json.get("thumbnailUrl"))
                .contains("records/" + PI + "/files/images/" + doc.getFieldValue(SolrConstants.THUMBNAIL) + "/full/!100,120/0/default.jpg"));
        // URL root depends on the current config state and may variate, so only compare the args
        Assert.assertTrue("Image url was " + ((String) json.get("thumbnailUrl")), ((String) json.get("mediumimage"))
                .contains("records/" + PI + "/files/images/" + doc.getFieldValue(SolrConstants.THUMBNAIL) + "/full/!600,500/0/default.jpg"));
        Assert.assertEquals(rootUrl + "/" + PageType.viewObject.getName() + "/" + PI + "/1/LOG_0000/", json.get("url"));
        //        Assert.assertEquals(doc.getFieldValue(SolrConstants._CALENDAR_YEAR), json.get("date"));
    }

    /**
     * @see JsonTools#formatVersionString(String)
     * @verifies format string correctly
     */
    @Test
    public void formatVersionString_shouldFormatStringCorrectly() throws Exception {
        Assert.assertEquals("goobi-viewer-core 1337 2020-06-30 abcdefg",
                JsonTools.formatVersionString(
                        "{\"application\": \"goobi-viewer-core\", \"version\": \"1337\", \"build-date\": \"2020-06-30\", \"git-revision\": \"abcdefg\"}"));
    }

    /**
     * @see JsonTools#formatVersionString(String)
     * @verifies return notAvailableKey if json invalid
     */
    @Test
    public void formatVersionString_shouldReturnNotAvailableKeyIfJsonInvalid() throws Exception {
        Assert.assertEquals("admin__dashboard_versions_not_available", JsonTools.formatVersionString("not json"));
    }

    /**
     * @see JsonTools#shortFormatVersionString(String)
     * @verifies format string correctly
     */
    @Test
    public void shortFormatVersionString_shouldFormatStringCorrectly() throws Exception {
        Assert.assertEquals("1337 (abcdefg)",
                JsonTools.shortFormatVersionString(
                        "{\"application\": \"goobi-viewer-core\", \"version\": \"1337\", \"build-date\": \"2020-06-30\", \"git-revision\": \"abcdefg\"}"));
    }

    /**
     * @see JsonTools#shortFormatVersionString(String)
     * @verifies return notAvailableKey if json invalid
     */
    @Test
    public void shortFormatVersionString_shouldReturnNotAvailableKeyIfJsonInvalid() throws Exception {
        Assert.assertEquals("admin__dashboard_versions_not_available", JsonTools.shortFormatVersionString("not json"));
    }
}