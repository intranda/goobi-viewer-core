package io.goobi.viewer.controller;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.model.viewer.PageType;

public class JsonToolsTest extends AbstractSolrEnabledTest {

    private static final String PI = PI_KLEIUNIV;

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
        JSONObject json = JsonTools.getRecordJsonObject(doc, rootUrl);
        Assert.assertNotNull(json);
        Assert.assertEquals(PI, json.get("id"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.TITLE), json.get("title"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.DATECREATED), json.get("dateCreated"));
        Assert.assertEquals(doc.getFieldValues("MD_PERSON_UNTOKENIZED"), json.get("personList"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.DC), json.get("collection"));
        // URL root depends on the current config state and may variate, so only compare the args
        Assert.assertTrue("Thumbnail url was " + ((String) json.get("thumbnailUrl")), ((String) json.get("thumbnailUrl"))
                .contains("image/" + PI + "/" + doc.getFieldValue(SolrConstants.THUMBNAIL) + "/full/!100,120/0/default.jpg"));
        // URL root depends on the current config state and may variate, so only compare the args
        Assert.assertTrue("Image url was " + ((String) json.get("thumbnailUrl")), ((String) json.get("mediumimage"))
                .contains("image/" + PI + "/" + doc.getFieldValue(SolrConstants.THUMBNAIL) + "/full/!600,500/0/default.jpg"));
        Assert.assertEquals(rootUrl + "/" + PageType.viewObject.getName() + "/" + PI + "/1/LOG_0000/", json.get("url"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants._CALENDAR_YEAR), json.get("date"));
    }
}