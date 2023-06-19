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
package io.goobi.viewer.model.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

public class BrowseElementTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List)
     * @verifies add sort fields correctly
     */
    @Test
    public void addSortFieldsToMetadata_shouldAddSortFieldsCorrectly() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), null);
        Assert.assertEquals(1, be.getMetadataList().size());
        Assert.assertEquals("SORT_FOO", be.getMetadataList().get(0).getLabel());
        Assert.assertEquals(1, be.getMetadataList().get(0).getValues().size());
        Assert.assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().size());
        Assert.assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).size());
        Assert.assertEquals("bar", be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).get(0));
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List,Set)
     * @verifies not add fields on ignore list
     */
    @Test
    public void addSortFieldsToMetadata_shouldNotAddFieldsOnIgnoreList() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), Collections.singleton("SORT_FOO"));
        Assert.assertEquals(0, be.getMetadataList().size());
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List)
     * @verifies not add fields already in the list
     */
    @Test
    public void addSortFieldsToMetadata_shouldNotAddFieldsAlreadyInTheList() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));

        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.getMetadataList().add(new Metadata(String.valueOf(se.getLuceneId()), "MD_FOO", "", "old value"));

        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), null);
        Assert.assertEquals(1, be.getMetadataList().size());
        Assert.assertEquals("MD_FOO", be.getMetadataList().get(0).getLabel());
        Assert.assertEquals(1, be.getMetadataList().get(0).getValues().size());
        Assert.assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().size());
        Assert.assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).size());
        Assert.assertEquals("old value", be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).get(0));
    }

    /**
     * @see BrowseElement#addFoundMetadataContainingSearchTerms(StructElement,Map,Locale)
     * @verifies add metadata fields that match search terms
     */
    @Test
    public void addFoundMetadataContainingSearchTerms_shouldAddMetadataFieldsThatMatchSearchTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "label", null, Locale.ENGLISH, null, null);

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR"));
        se.getMetadataFields().put("MD_YEARPUBLISH", Collections.singletonList("ca. 1984"));
        Assert.assertEquals(2, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        searchTerms.put("MD_YEARPUBLISH", new HashSet<>(Arrays.asList(new String[] { "1984" })));

        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, null, null, null, 0);
        Assert.assertEquals(2, be.getFoundMetadataList().size());
        {
            String field = "MD_TITLE";
            Assert.assertNotNull(be.getMetadataList(field));
            List<Metadata> mdList = be.getMetadataList(field);
            Assert.assertFalse(mdList.get(0).getValues().isEmpty());
            Assert.assertEquals("FROM <span class=\"search-list--highlight\">FOO</span> TO <span class=\"search-list--highlight\">BAR</span>",
                    mdList.get(0).getValues().get(0).getComboValueShort(0));
        }
        {
            String field = "MD_YEARPUBLISH";
            Assert.assertNotNull(be.getMetadataList(field));
            List<Metadata> mdList = be.getMetadataList(field);
            Assert.assertFalse(mdList.get(0).getValues().isEmpty());
            Assert.assertEquals("ca. <span class=\"search-list--highlight\">1984</span>", mdList.get(0).getValues().get(0).getComboValueShort(0));
        }
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map)
     * @verifies not add duplicates from default terms
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldNotAddDuplicatesFromDefaultTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR")); // same value as the main label
        Assert.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));

        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, null, null, null, 0);
        Assert.assertTrue(be.getMetadataList("MD_TITLE").isEmpty());
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map)
     * @verifies not add duplicates from explicit terms
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldNotAddDuplicatesFromExplicitTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR")); // same value as the main label
        Assert.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put("MD_TITLE", new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));

        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, null, null, null, 0);
        Assert.assertEquals(1, be.getMetadataList("MD_TITLE").size());
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map,Set)
     * @verifies not add ignored fields
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldNotAddIgnoredFields() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_IGNOREME", Collections.singletonList("foo ignores bar"));
        Assert.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));

        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, new HashSet<>(Collections.singletonList("MD_IGNOREME")), null, null, null, 0);
        Assert.assertEquals(0, be.getMetadataList("MD_IGNOREME").size());
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map,Set,Set)
     * @verifies translate configured field values correctly
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldTranslateConfiguredFieldValuesCorrectly() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put(SolrConstants.DC, Collections.singletonList("admin"));
        Assert.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        searchTerms.put(SolrConstants.DC, new HashSet<>(Arrays.asList(new String[] { "admin" })));

        String[] translateFields = { SolrConstants.DC };
        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, new HashSet<>(Arrays.asList(translateFields)), null, null, 0);
        Assert.assertEquals(1, be.getMetadataList(SolrConstants.DC).size());
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map,Set,Set,Set)
     * @verifies write one line fields into a single string
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldWriteOneLineFieldsIntoASingleString() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_COUNT_EN", Arrays.asList(new String[] { "one", "two", "three" }));
        se.getMetadataFields().put("MD_COUNT_JP", Arrays.asList(new String[] { "ichi", "ni", "san" }));
        Assert.assertEquals(2, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "ichi", "ni" })));
        searchTerms.put("MD_COUNT_EN", new HashSet<>(Arrays.asList(new String[] { "one", "three" })));

        String[] oneLineFields = { "MD_COUNT_EN", "MD_COUNT_JP" };
        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, null, new HashSet<>(Arrays.asList(oneLineFields)), null, 0);

        // Via explicit term field
        Assert.assertEquals(1, be.getMetadataList("MD_COUNT_EN").size());
        Assert.assertEquals(
                "<span class=\"search-list--highlight\">one</span>, <span class=\"search-list--highlight\">three</span>",
                be.getMetadataList("MD_COUNT_EN").get(0).getValues().get(0).getComboValueShort(0));

        // Via DEFAULT
        Assert.assertEquals(1, be.getMetadataList("MD_COUNT_JP").size());
        Assert.assertEquals("<span class=\"search-list--highlight\">ichi</span>, <span class=\"search-list--highlight\">ni</span>",
                be.getMetadataList("MD_COUNT_JP").get(0).getValues().get(0).getComboValueShort(0));
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map,Set,Set,Set,Set)
     * @verifies truncate snippet fields correctly
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldTruncateSnippetFieldsCorrectly() throws Exception {
        int maxLength = 50;
        DataManager.getInstance().getConfiguration().overrideValue("search.fulltextFragmentLength", maxLength);

        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_DESCRIPTION", Collections.singletonList(StringConstants.LOREM_IPSUM));
        se.getMetadataFields().put("MD_SOMETEXT", Collections.singletonList(StringConstants.LOREM_IPSUM.replace("labore", "foo")));
        Assert.assertEquals(2, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, Collections.singleton("labore"));
        searchTerms.put("MD_SOMETEXT", Collections.singleton("ipsum"));

        String[] snippetFields = { "MD_DESCRIPTION", "MD_SOMETEXT" };
        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, null, null, new HashSet<>(Arrays.asList(snippetFields)), 0);

        // Via DEFAULT
        Assert.assertEquals(1, be.getMetadataList("MD_DESCRIPTION").size());
        Assert.assertTrue(be.getMetadataList("MD_DESCRIPTION").get(0).getValues().get(0).getComboValueShort(0).length() <= maxLength + 56);
        // Truncated snippet is randomized, so cannot test the exact value
        Assert.assertTrue(be.getMetadataList("MD_DESCRIPTION")
                .get(0)
                .getValues()
                .get(0)
                .getComboValueShort(0)
                .contains("ut <span class=\"search-list--highlight\">labore</span> et"));

        // Via explicit term field
        Assert.assertEquals(1, be.getMetadataList("MD_SOMETEXT").size());
        Assert.assertTrue(be.getMetadataList("MD_SOMETEXT").get(0).getValues().get(0).getComboValueShort(0).length() <= maxLength + 56);
        // Truncated snippet is randomized, so cannot test the exact value
        Assert.assertTrue(be.getMetadataList("MD_SOMETEXT")
                .get(0)
                .getValues()
                .get(0)
                .getComboValueShort(0)
                .contains("<span class=\"search-list--highlight\">ipsum</span> dolor"));
    }

    /**
     * @see BrowseElement#generateDefaultLabel(StructElement)
     * @verifies translate docstruct label
     */
    @Test
    public void generateDefaultLabel_shouldTranslateDocstructLabel() throws Exception {
        //        BrowseElement be = new BrowseElement("PPN123", 1, null, null, Locale.GERMAN, null, null);
        StructElement se = new StructElement();
        se.setDocStructType("Monograph");
        String label = BrowseElement.generateDefaultLabel(se, Locale.GERMAN);
        Assert.assertEquals("Monografie", label);
    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any line breaks
     */
    @Test
    public void getFulltextForHtml_shouldRemoveAnyLineBreaks() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", "foo\nbar", Locale.ENGLISH, null, null);
        Assert.assertEquals("foo bar", be.getFulltextForHtml());

    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any JS
     */
    @Test
    public void getFulltextForHtml_shouldRemoveAnyJS() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR",
                "foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar", Locale.ENGLISH, null, null);
        Assert.assertEquals("foo  bar", be.getFulltextForHtml());
    }
}
