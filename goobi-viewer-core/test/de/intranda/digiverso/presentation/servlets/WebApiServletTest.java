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
package de.intranda.digiverso.presentation.servlets;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.viewer.PageType;

public class WebApiServletTest extends AbstractSolrEnabledTest {

    private static final String PI = "PPN517154005";

    /**
     * @see WebApiServlet#getRecordJsonObject(SolrDocument,String)
     * @verifies add all metadata
     */
    @Test
    public void getRecordJsonObject_shouldAddAllMetadata() throws Exception {
        String rootUrl = "http://localhost:8080/viewer";
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + PI, 0, 1, null, null, null);
        Assert.assertFalse("Required Solr document not found in index: " + PI, response.getResults().isEmpty());
        SolrDocument doc = response.getResults().get(0);
        JSONObject json = WebApiServlet.getRecordJsonObject(doc, rootUrl);
        Assert.assertNotNull(json);
        Assert.assertEquals(PI, json.get("id"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.TITLE), json.get("title"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.DATECREATED), json.get("dateCreated"));
        Assert.assertEquals(doc.getFieldValues("MD_PERSON_UNTOKENIZED"), json.get("personList"));
        Assert.assertEquals(doc.getFieldValue(SolrConstants.DC), json.get("collection"));
        // URL root depends on the current config state and may variate, so only compare the args
        Assert.assertTrue("Thumbnail url was " + ((String) json.get("thumbnailUrl")),((String) json.get("thumbnailUrl")).contains("image/" + PI + "/" + doc.getFieldValue(SolrConstants.THUMBNAIL) + "/full/!100,120/0/default.jpg"));
        // URL root depends on the current config state and may variate, so only compare the args
        Assert.assertTrue("Image url was " + ((String) json.get("thumbnailUrl")), ((String) json.get("mediumimage")).contains("image/" + PI + "/" + doc.getFieldValue(SolrConstants.THUMBNAIL) + "/full/!600,500/0/default.jpg"));
        Assert.assertEquals(rootUrl + "/" + PageType.viewImage.getName() + "/" + PI + "/1/LOG_0000/", json.get("url"));
        Assert.assertEquals(doc.getFieldValue("YEAR"), json.get("date"));
    }
}