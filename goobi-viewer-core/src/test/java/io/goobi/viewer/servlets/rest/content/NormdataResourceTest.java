package io.goobi.viewer.servlets.rest.content;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.normdataimporter.model.NormData;
import de.intranda.digiverso.normdataimporter.model.NormDataValue;

public class NormdataResourceTest {

    /**
     * @see NormdataResource#addNormDataValuesToJSON(NormData,Locale)
     * @verifies add values correctly
     */
    @Test
    public void addNormDataValuesToJSON_shouldAddValuesCorrectly() throws Exception {
        NormData normData = new NormData();
        normData.setKey("NORM_FOO");
        normData.getValues().add(new NormDataValue("bar", null, null));
        JSONObject jsonObj =NormdataResource.addNormDataValuesToJSON(normData, null);
        Assert.assertNotNull(jsonObj);
        Assert.assertTrue(jsonObj.has("NORM_FOO"));
        JSONArray jsonArray = (JSONArray) jsonObj.get("NORM_FOO");
        Assert.assertEquals(1, jsonArray.length());
        Assert.assertEquals("bar", ((JSONObject) jsonArray.get(0)).getString("text"));
    }
}