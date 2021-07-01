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
package io.goobi.viewer.solr;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.viewer.StringPair;

public class SolrToolsTest extends AbstractSolrEnabledTest {

    /**
     * @see SolrTools#getFieldValueMap(SolrDocument)
     * @verifies return all fields in the given doc except page urns
     */
    @Test
    public void getFieldValueMap_shouldReturnAllFieldsInTheGivenDocExceptPageUrns() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + PI_KLEIUNIV, null);
        Assert.assertNotNull(doc);
        Map<String, List<String>> fieldValueMap = SolrTools.getFieldValueMap(doc);
        Assert.assertFalse(fieldValueMap.containsKey(SolrConstants.IMAGEURN_OAI));
        Assert.assertFalse(fieldValueMap.containsKey("PAGEURNS"));
    }

    /**
     * @see SolrTools#getMetadataValues(SolrDocument,String)
     * @verifies return all values for the given field
     */
    @Test
    public void getMetadataValues_shouldReturnAllValuesForTheGivenField() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + PI_KLEIUNIV, null);
        Assert.assertNotNull(doc);
        List<String> values = SolrTools.getMetadataValues(doc, SolrConstants.DATEUPDATED);
        Assert.assertTrue(values.size() >= 2);
    }

    /**
     * @see SolrTools#getSingleFieldStringValue(SolrDocument,String)
     * @verifies not return null as string if value is null
     */
    @Test
    public void getSingleFieldStringValue_shouldNotReturnNullAsStringIfValueIsNull() throws Exception {
        SolrDocument doc = new SolrDocument();
        Assert.assertNull(SolrTools.getSingleFieldStringValue(doc, "MD_NOSUCHFIELD"));
    }

    /**
     * @see SolrTools#getSingleFieldStringValue(SolrDocument,String)
     * @verifies return value as string correctly
     */
    @Test
    public void getSingleFieldStringValue_shouldReturnValueAsStringCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField("NUM", 1337);
        Assert.assertEquals("1337", SolrTools.getSingleFieldStringValue(doc, "NUM"));
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies split fields correctly
     */
    @Test
    public void getSolrSortFieldsAsList_shouldSplitFieldsCorrectly() throws Exception {
        List<StringPair> result = SolrTools.getSolrSortFieldsAsList("SORT_A; SORT_B, desc;SORT_C,asc", ";", ",");
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("SORT_A", result.get(0).getOne());
        Assert.assertEquals("asc", result.get(0).getTwo());
        Assert.assertEquals("SORT_B", result.get(1).getOne());
        Assert.assertEquals("desc", result.get(1).getTwo());
        Assert.assertEquals("SORT_C", result.get(2).getOne());
        Assert.assertEquals("asc", result.get(2).getTwo());
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies split single field correctly
     */
    @Test
    public void getSolrSortFieldsAsList_shouldSplitSingleFieldCorrectly() throws Exception {
        List<StringPair> result = SolrTools.getSolrSortFieldsAsList("SORT_A , desc ", ";", ",");
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("SORT_A", result.get(0).getOne());
        Assert.assertEquals("desc", result.get(0).getTwo());
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if solrSortFields is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSolrSortFieldsIsNull() throws Exception {
        SolrTools.getSolrSortFieldsAsList(null, ";", ",");
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if splitFieldsBy is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSplitFieldsByIsNull() throws Exception {
        SolrTools.getSolrSortFieldsAsList("bla,blup", null, ",");
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if splitNameOrderBy is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSplitNameOrderByIsNull() throws Exception {
        SolrTools.getSolrSortFieldsAsList("bla,blup", ";", null);
    }

    /**
     * @see SolrTools#isHasImages(SolrDocument)
     * @verifies return correct value for docsctrct docs
     */
    @Test
    public void isHasImages_shouldReturnCorrectValueForDocsctrctDocs() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.THUMBNAIL, "foo.jpg");
        Assert.assertTrue(SolrTools.isHasImages(doc));
    }

    /**
     * @see SolrTools#isHasImages(SolrDocument)
     * @verifies return correct value for page docs
     */
    @Test
    public void isHasImages_shouldReturnCorrectValueForPageDocs() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.FILENAME, "foo.jpg");
        Assert.assertTrue(SolrTools.isHasImages(doc));
        doc.setField(SolrConstants.FILENAME, "foo.txt");
        Assert.assertFalse(SolrTools.isHasImages(doc));
    }

    @Test
    public void testGetMetadataValuesForLanguage() {
        SolrDocument doc = new SolrDocument();
        doc.addField("field_A", "value_A");
        doc.addField("field_B_LANG_EN", "field_B_en");
        doc.addField("field_B_LANG_DE", "field_B_de");
        doc.addField("field_B_LANG_EN", "field_B_en_2");

        Map<String, List<String>> mapA = SolrTools.getMetadataValuesForLanguage(doc, "field_A");
        Assert.assertEquals(1, mapA.size());
        Assert.assertEquals(1, mapA.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).size());
        Assert.assertEquals("value_A", mapA.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).get(0));

        Map<String, List<String>> mapB = SolrTools.getMetadataValuesForLanguage(doc, "field_B");
        Assert.assertEquals(2, mapB.size());
        Assert.assertEquals(mapB.get("en").size(), 2);
        Assert.assertEquals(mapB.get("de").size(), 1);
        Assert.assertEquals("field_B_de", mapB.get("de").get(0));
        Assert.assertEquals("field_B_en", mapB.get("en").get(0));
        Assert.assertEquals("field_B_en_2", mapB.get("en").get(1));
    }

    @Test
    public void testGetMultiLanguageFieldValueMap() {
        SolrDocument doc = new SolrDocument();
        doc.addField("field_A", "value_A");
        doc.addField("field_B", "value_B");
        doc.addField("field_B_LANG_EN", "field_B_en");
        doc.addField("field_B_LANG_DE", "field_B_de");
        doc.addField("field_B_LANG_EN", "field_B_en_2");

        Map<String, List<IMetadataValue>> map = SolrTools.getMultiLanguageFieldValueMap(doc);
        Assert.assertEquals(2, map.keySet().size());
        Assert.assertEquals("value_A", map.get("field_A").get(0).getValue().get());
        Assert.assertEquals("value_B", map.get("field_B").get(0).getValue().get());
        Assert.assertEquals("field_B_de", map.get("field_B").get(0).getValue("de").get());
        Assert.assertEquals("field_B_en", map.get("field_B").get(0).getValue("en").get());
        Assert.assertEquals("field_B_en_2", map.get("field_B").get(1).getValue("en").get());

        Assert.assertEquals("value_B", map.get("field_B").get(0).getValue("fr").orElse(map.get("field_B").get(0).getValue().orElse("")));
    }

}