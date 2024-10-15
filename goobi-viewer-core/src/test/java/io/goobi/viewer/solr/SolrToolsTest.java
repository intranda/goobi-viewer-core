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
package io.goobi.viewer.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.viewer.StringPair;

class SolrToolsTest extends AbstractSolrEnabledTest {

    /**
     * @see SolrTools#getFieldValueMap(SolrDocument)
     * @verifies return all fields in the given doc except page urns
     */
    @Test
    void getFieldValueMap_shouldReturnAllFieldsInTheGivenDocExceptPageUrns() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + PI_KLEIUNIV, null);
        Assertions.assertNotNull(doc);
        Map<String, List<String>> fieldValueMap = SolrTools.getFieldValueMap(doc);
        Assertions.assertTrue(fieldValueMap.containsKey(SolrConstants.PI_TOPSTRUCT));
        Assertions.assertTrue(fieldValueMap.containsKey(SolrConstants.MIMETYPE));
        Assertions.assertFalse(fieldValueMap.containsKey(SolrConstants.IMAGEURN_OAI));
        Assertions.assertFalse(fieldValueMap.containsKey("PAGEURNS"));
    }

    /**
     * @see SolrTools#getMetadataValues(SolrDocument,String)
     * @verifies return all values for the given field
     */
    @Test
    void getMetadataValues_shouldReturnAllValuesForTheGivenField() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":AC13451894", null);
        Assertions.assertNotNull(doc);
        List<String> values = SolrTools.getMetadataValues(doc, "MD_CREATOR");
        Assertions.assertTrue(values.size() >= 2);
    }

    /**
     * @see SolrTools#getSingleFieldStringValue(SolrDocument,String)
     * @verifies not return null as string if value is null
     */
    @Test
    void getSingleFieldStringValue_shouldNotReturnNullAsStringIfValueIsNull() throws Exception {
        SolrDocument doc = new SolrDocument();
        Assertions.assertNull(SolrTools.getSingleFieldStringValue(doc, "MD_NOSUCHFIELD"));
    }

    /**
     * @see SolrTools#getSingleFieldStringValue(SolrDocument,String)
     * @verifies return value as string correctly
     */
    @Test
    void getSingleFieldStringValue_shouldReturnValueAsStringCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField("NUM", 1337);
        assertEquals("1337", SolrTools.getSingleFieldStringValue(doc, "NUM"));
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies split fields correctly
     */
    @Test
    void getSolrSortFieldsAsList_shouldSplitFieldsCorrectly() throws Exception {
        List<StringPair> result = SolrTools.getSolrSortFieldsAsList("SORT_A; SORT_B, desc;SORT_C,asc", ";", ",");
        Assertions.assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("SORT_A", result.get(0).getOne());
        assertEquals("asc", result.get(0).getTwo());
        assertEquals("SORT_B", result.get(1).getOne());
        assertEquals("desc", result.get(1).getTwo());
        assertEquals("SORT_C", result.get(2).getOne());
        assertEquals("asc", result.get(2).getTwo());
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies split single field correctly
     */
    @Test
    void getSolrSortFieldsAsList_shouldSplitSingleFieldCorrectly() throws Exception {
        List<StringPair> result = SolrTools.getSolrSortFieldsAsList("SORT_A , desc ", ";", ",");
        Assertions.assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SORT_A", result.get(0).getOne());
        assertEquals("desc", result.get(0).getTwo());
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if solrSortFields is null
     */
    @Test
    void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSolrSortFieldsIsNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SolrTools.getSolrSortFieldsAsList(null, ";", ","));
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if splitFieldsBy is null
     */
    @Test
    void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSplitFieldsByIsNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SolrTools.getSolrSortFieldsAsList("bla,blup", null, ","));
    }

    /**
     * @see SolrTools#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if splitNameOrderBy is null
     */
    @Test
    void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSplitNameOrderByIsNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SolrTools.getSolrSortFieldsAsList("bla,blup", ";", null));
    }

    /**
     * @see SolrTools#isHasImages(SolrDocument)
     * @verifies return correct value for docsctrct docs
     */
    @Test
    void isHasImages_shouldReturnCorrectValueForDocsctrctDocs() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.THUMBNAIL, "foo.jpg");
        Assertions.assertTrue(SolrTools.isHasImages(doc));
    }

    /**
     * @see SolrTools#isHasImages(SolrDocument)
     * @verifies return correct value for page docs
     */
    @Test
    void isHasImages_shouldReturnCorrectValueForPageDocs() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.FILENAME, "foo.jpg");
        Assertions.assertTrue(SolrTools.isHasImages(doc));
        doc.setField(SolrConstants.FILENAME, "foo.txt");
        Assertions.assertFalse(SolrTools.isHasImages(doc));
    }

    /**
     * @see SolrTools#isHasImages(SolrDocument)
     * @verifies return correct value for iiif manifests in file name
     */
    @Test
    void isHasImages_shouldReturnCorrectValueForIiifManifestsInFileName() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.THUMBNAIL, "https://example.com/iiif/2/foo.jpg/info.json");
        Assertions.assertTrue(SolrTools.isHasImages(doc));
    }

    @Test
    void testGetMetadataValuesForLanguage() {
        SolrDocument doc = new SolrDocument();
        doc.addField("field_A", "value_A");
        doc.addField("field_B_LANG_EN", "field_B_en");
        doc.addField("field_B_LANG_DE", "field_B_de");
        doc.addField("field_B_LANG_EN", "field_B_en_2");

        Map<String, List<String>> mapA = SolrTools.getMetadataValuesForLanguage(doc, "field_A");
        assertEquals(1, mapA.size());
        assertEquals(1, mapA.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).size());
        assertEquals("value_A", mapA.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).get(0));

        Map<String, List<String>> mapB = SolrTools.getMetadataValuesForLanguage(doc, "field_B");
        assertEquals(2, mapB.size());
        assertEquals(2, mapB.get("en").size());
        assertEquals(1, mapB.get("de").size());
        assertEquals("field_B_de", mapB.get("de").get(0));
        assertEquals("field_B_en", mapB.get("en").get(0));
        assertEquals("field_B_en_2", mapB.get("en").get(1));
    }

    @Test
    void testGetMultiLanguageFieldValueMap() {
        SolrDocument doc = new SolrDocument();
        doc.addField("field_A", "value_A");
        doc.addField("field_B", "value_B");
        doc.addField("field_B_LANG_EN", "field_B_en");
        doc.addField("field_B_LANG_DE", "field_B_de");
        doc.addField("field_B_LANG_EN", "field_B_en_2");

        Map<String, List<IMetadataValue>> map = SolrTools.getMultiLanguageFieldValueMap(doc);
        assertEquals(2, map.keySet().size());
        assertEquals("value_A", map.get("field_A").get(0).getValue().get());
        assertEquals("value_B", map.get("field_B").get(0).getValue().get());
        assertEquals("field_B_de", map.get("field_B").get(0).getValue("de").get());
        assertEquals("field_B_en", map.get("field_B").get(0).getValue("en").get());
        assertEquals("field_B_en_2", map.get("field_B").get(1).getValue("en").get());

        assertEquals("value_B", map.get("field_B").get(0).getValue("fr").orElse(map.get("field_B").get(0).getValue().orElse("")));
    }

    /**
     * @see SolrTools#getAvailableValuesForField(String,String)
     * @verifies return all existing values for the given field
     */
    @Test
    void getAvailableValuesForField_shouldReturnAllExistingValuesForTheGivenField() throws Exception {
        List<String> values = SolrTools.getAvailableValuesForField("MD_YEARPUBLISH", SolrConstants.ISWORK + ":true");
        Assertions.assertFalse(values.isEmpty());
    }

    @Test
    void getAvailableValuesForField_shouldReturnAllEntireValues() throws Exception {
        List<String> values = SolrTools.getAvailableValuesForField("MD_PLACEPUBLISH", SolrConstants.ISWORK + ":true");
        Assertions.assertFalse(values.isEmpty());
        //values.forEach(System.out::println);
        Assertions.assertTrue(values.contains("Ateliersituation vor neutralem Hintergrund"));
    }

    /**
     * @see SolrTools#getExistingSubthemes()
     * @verifies return correct values
     */
    @Test
    void getExistingSubthemes_shouldReturnCorrectValues() throws Exception {
        List<String> result = SolrTools.getExistingSubthemes();
        assertEquals(2, result.size());
        Assertions.assertTrue(result.contains("subtheme1"));
        Assertions.assertTrue(result.contains("subtheme2"));
    }

    /**
     * @see SolrTools#extractExceptionMessageHtmlTitle(String)
     * @verifies return empty string if exceptionMessage empty
     */
    @Test
    void extractExceptionMessageHtmlTitle_shouldReturnEmptyStringIfExceptionMessageEmpty() throws Exception {
        assertEquals("", SolrTools.extractExceptionMessageHtmlTitle(null));
    }

    /**
     * @see SolrTools#extractExceptionMessageHtmlTitle(String)
     * @verifies return exceptionMessage if no pattern match found
     */
    @Test
    void extractExceptionMessageHtmlTitle_shouldReturnExceptionMessageIfNoPatternMatchFound() throws Exception {
        String html = "<html><head></head><body><h1>foo</h1></body</html>";
        assertEquals(html, SolrTools.extractExceptionMessageHtmlTitle(html));
    }

    /**
     * @see SolrTools#extractExceptionMessageHtmlTitle(String)
     * @verifies return title content correctly
     */
    @Test
    void extractExceptionMessageHtmlTitle_shouldReturnTitleContentCorrectly() throws Exception {
        String html = "<html><head><title>foo bar</title></head><body><h1>foo</h1></body</html>";
        assertEquals("foo bar", SolrTools.extractExceptionMessageHtmlTitle(html));
    }

    @Test
    void test_escapeSpecialCharacters() {
        assertEquals("x\\\"\\>\\<svG onLoad=alert\\(\\\"Hello_XSS_World\\\"\\)\\>",
                SolrTools.escapeSpecialCharacters("x\"><svG onLoad=alert(\"Hello_XSS_World\")>"));
        assertEquals("x\\\"\\>\\<svG onLoad=alert\\(\\\"Hello_XSS_World\\\"\\)\\>",
                SolrTools.escapeSpecialCharacters("x\\\"\\>\\<svG onLoad=alert\\(\\\"Hello_XSS_World\\\"\\)\\>"));
        assertEquals(null, SolrTools.escapeSpecialCharacters(null));
        assertEquals("LOG_0004", SolrTools.escapeSpecialCharacters("LOG_0004"));
    }

    @Test
    void test_unescapeSpecialCharacters() {
        assertEquals("x\"><svG onLoad=alert(\"Hello_XSS_World\")>",
                SolrTools.unescapeSpecialCharacters("x\\\"\\>\\<svG onLoad=alert\\(\\\"Hello_XSS_World\\\"\\)\\>"));
        assertEquals("x\"><svG onLoad=alert(\"Hello_XSS_World\")>",
                SolrTools.unescapeSpecialCharacters("x\"><svG onLoad=alert(\"Hello_XSS_World\")>"));
        assertEquals(null, SolrTools.unescapeSpecialCharacters(null));
        assertEquals("LOG_0004", SolrTools.unescapeSpecialCharacters("LOG_0004"));
    }

    /**
     * @see SolrTools#cleanUpQuery(String)
     * @verifies remove brace pairs
     */
    @Test
    void cleanUpQuery_shouldRemoveBracePairs() throws Exception {
        assertEquals("foo:bar", SolrTools.cleanUpQuery("{foo:bar}"));
    }

    /**
     * @see SolrTools#cleanUpQuery(String)
     * @verifies keep join parameter
     */
    @Test
    void cleanUpQuery_shouldKeepJoinParameter() throws Exception {
        assertEquals("{!join from=PI_TOPSTRUCT to=PI}foo:bar", SolrTools.cleanUpQuery("{!join from=PI_TOPSTRUCT to=PI}foo:bar"));
    }

    /**
     * @see SolrTools#cleanUpQuery(String)
     * @verifies keep single braces
     */
    @Test
    void cleanUpQuery_shouldKeepSingleBraces() throws Exception {
        assertEquals("\\{u.a.", SolrTools.cleanUpQuery("\\{u.a."));
    }
}
