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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataWrapper;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

class SearchHitFactoryTest extends AbstractSolrEnabledTest {

    @Test
    void createSearchHit_findWithUmlaut() throws PresentationException, IndexUnreachableException {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, Long.toString(1l));
        doc.setField("MD_FOO", "Norden");
        doc.setField("MD_BAR", "Nørre");
        Map<String, Set<String>> searchTerms = Collections.singletonMap(SolrConstants.DEFAULT, Collections.singleton("Nörde~1"));
        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN).createSearchHit(doc, null, null, null);
        assertEquals(1, hit.getFoundMetadata().size());
    }

    @Test
    void createSearchHit_findUmlaute() throws PresentationException, IndexUnreachableException {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, Long.toString(1l));
        doc.setField("MD_FOO", "Nörden");
        doc.setField("MD_BAR", "Nørre");
        Map<String, Set<String>> searchTerms = Collections.singletonMap(SolrConstants.DEFAULT, Collections.singleton("Norde~1"));
        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN).createSearchHit(doc, null, null, null);
        assertEquals(1, hit.getFoundMetadata().size());
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies add metadata fields that match search terms
     */
    @Test
    void findAdditionalMetadataFieldsContainingSearchTerms_shouldAddMetadataFieldsThatMatchSearchTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "label", null, Locale.ENGLISH, null, null);

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR"));
        se.getMetadataFields().put("MD_YEARPUBLISH", Collections.singletonList("ca. 1984"));
        Assertions.assertEquals(2, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        searchTerms.put("MD_YEARPUBLISH", new HashSet<>(Arrays.asList(new String[] { "1984" })));

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);
        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());

        Assertions.assertEquals(2, result.size());
        if (!result.isEmpty()) {
            for (MetadataWrapper mw : result) {
                be.getMetadataList().add(mw.getMetadata());
            }
        }

        Assertions.assertNotNull(be.getMetadataList("MD_TITLE"));
        List<Metadata> mdList1 = be.getMetadataList("MD_TITLE");
        Assertions.assertFalse(mdList1.get(0).getValues().isEmpty());
        Assertions.assertEquals("FROM <span class=\"search-list--highlight\">FOO</span> TO <span class=\"search-list--highlight\">BAR</span>",
                mdList1.get(0).getValues().get(0).getComboValueShort(0));
        Assertions.assertNotNull(be.getMetadataList("MD_YEARPUBLISH"));

        List<Metadata> mdList2 = be.getMetadataList("MD_YEARPUBLISH");
        Assertions.assertFalse(mdList2.get(0).getValues().isEmpty());
        Assertions.assertEquals("ca. <span class=\"search-list--highlight\">1984</span>", mdList2.get(0).getValues().get(0).getComboValueShort(0));
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies not add duplicates from default terms
     */
    @Test
    void findAdditionalMetadataFieldsContainingSearchTerms_shouldNotAddDuplicatesFromDefaultTerms() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);

        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR")); // same value as the main label
        Assertions.assertEquals(1, se.getMetadataFields().size());

        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertTrue(be.getMetadataList("MD_TITLE").isEmpty());

    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies not add duplicates from explicit terms
     */
    @Test
    void findAdditionalMetadataFieldsContainingSearchTerms_shouldNotAddDuplicatesFromExplicitTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR")); // same value as the main label
        Assertions.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put("MD_TITLE", new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);
        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());
        if (!result.isEmpty()) {
            for (MetadataWrapper mw : result) {
                be.getMetadataList().add(mw.getMetadata());
            }
        }

        Assertions.assertEquals(1, be.getMetadataList("MD_TITLE").size());
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies not add ignored fields
     */
    @Test
    void findAdditionalMetadataFieldsContainingSearchTerms_shouldNotAddIgnoredFields() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_IGNOREME", Collections.singletonList("foo ignores bar"));
        Assertions.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);
        factory.getAdditionalMetadataIgnoreFields().add("MD_IGNOREME");
        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());
        if (!result.isEmpty()) {
            for (MetadataWrapper mw : result) {
                be.getMetadataList().add(mw.getMetadata());
            }
        }

        Assertions.assertEquals(0, be.getMetadataList("MD_IGNOREME").size());
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies translate configured field values correctly
     */
    @Test
    void findAdditionalMetadataFieldsContainingSearchTerms_shouldTranslateConfiguredFieldValuesCorrectly() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put(SolrConstants.DC, Collections.singletonList("admin"));
        Assertions.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        searchTerms.put(SolrConstants.DC, new HashSet<>(Arrays.asList(new String[] { "admin" })));

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);
        factory.getAdditionalMetadataTranslateFields().add(SolrConstants.DC);
        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());
        if (!result.isEmpty()) {
            for (MetadataWrapper mw : result) {
                be.getMetadataList().add(mw.getMetadata());
            }
        }

        Assertions.assertEquals(1, be.getMetadataList(SolrConstants.DC).size());
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies write one line fields into a single string
     */
    @Test
    void findAdditionalMetadataFieldsContainingSearchTerms_shouldWriteOneLineFieldsIntoASingleString() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_COUNT_EN", Arrays.asList(new String[] { "one", "two", "three" }));
        se.getMetadataFields().put("MD_COUNT_JP", Arrays.asList(new String[] { "ichi", "ni", "san" }));
        Assertions.assertEquals(2, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "ichi", "ni" })));
        searchTerms.put("MD_COUNT_EN", new HashSet<>(Arrays.asList(new String[] { "one", "three" })));

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);
        factory.getAdditionalMetadataOneLineFields().addAll(Arrays.asList("MD_COUNT_EN", "MD_COUNT_JP"));
        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());
        if (!result.isEmpty()) {
            for (MetadataWrapper mw : result) {
                be.getMetadataList().add(mw.getMetadata());
            }
        }

        // Via explicit term field
        Assertions.assertEquals(1, be.getMetadataList("MD_COUNT_EN").size());
        Assertions.assertEquals(
                "<span class=\"search-list--highlight\">one</span>, <span class=\"search-list--highlight\">three</span>",
                be.getMetadataList("MD_COUNT_EN").get(0).getValues().get(0).getComboValueShort(0));

        // Via DEFAULT
        Assertions.assertEquals(1, be.getMetadataList("MD_COUNT_JP").size());
        Assertions.assertEquals("<span class=\"search-list--highlight\">ichi</span>, <span class=\"search-list--highlight\">ni</span>",
                be.getMetadataList("MD_COUNT_JP").get(0).getValues().get(0).getComboValueShort(0));
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies truncate snippet fields correctly
     */
    @Test
    void findAdditionalMetadataFieldsContainingSearchTerms_shouldTruncateSnippetFieldsCorrectly() throws Exception {
        int maxLength = 50;
        DataManager.getInstance().getConfiguration().overrideValue("search.fulltextFragmentLength", maxLength);

        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_DESCRIPTION", Collections.singletonList(StringConstants.LOREM_IPSUM));
        se.getMetadataFields().put("MD_SOMETEXT", Collections.singletonList(StringConstants.LOREM_IPSUM.replace("labore", "foo")));
        Assertions.assertEquals(2, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, Collections.singleton("labore"));
        searchTerms.put("MD_SOMETEXT", Collections.singleton("ipsum"));

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);
        factory.getAdditionalMetadataSnippetFields().addAll(Arrays.asList("MD_DESCRIPTION", "MD_SOMETEXT"));
        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());
        if (!result.isEmpty()) {
            for (MetadataWrapper mw : result) {
                be.getMetadataList().add(mw.getMetadata());
            }
        }

        // Via DEFAULT
        Assertions.assertEquals(1, be.getMetadataList("MD_DESCRIPTION").size());
        Assertions.assertTrue(be.getMetadataList("MD_DESCRIPTION").get(0).getValues().get(0).getComboValueShort(0).length() <= maxLength + 56);
        // Truncated snippet is randomized, so cannot test the exact value
        Assertions.assertTrue(be.getMetadataList("MD_DESCRIPTION")
                .get(0)
                .getValues()
                .get(0)
                .getComboValueShort(0)
                .contains("ut <span class=\"search-list--highlight\">labore</span> et"));

        // Via explicit term field
        Assertions.assertEquals(1, be.getMetadataList("MD_SOMETEXT").size());
        Assertions.assertTrue(be.getMetadataList("MD_SOMETEXT").get(0).getValues().get(0).getComboValueShort(0).length() <= maxLength + 56);
        // Truncated snippet is randomized, so cannot test the exact value
        Assertions.assertTrue(be.getMetadataList("MD_SOMETEXT")
                .get(0)
                .getValues()
                .get(0)
                .getComboValueShort(0)
                .contains("<span class=\"search-list--highlight\">ipsum</span> dolor"));
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(Map,Map,Set,String,String)
     * @verifies not add highlighting to nohighlight fields
     */
    @Test
    void findAdditionalMetadataFieldsContainingSearchTerms_shouldNotAddHighlightingToNohighlightFields() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_IDENTIFIER", Collections.singletonList("id10T"));
        Assertions.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "id10T", })));
        searchTerms.put("MD_IDENTIFIER", new HashSet<>(Arrays.asList(new String[] { "id10T" })));

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);
        factory.getAdditionalMetadataNoHighlightFields().add("MD_IDENTIFIER");
        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());
        if (!result.isEmpty()) {
            for (MetadataWrapper mw : result) {
                be.getMetadataList().add(mw.getMetadata());
            }
        }

        Assertions.assertEquals(1, be.getMetadataList("MD_IDENTIFIER").size());
        Assertions.assertEquals("id10T", be.getMetadataList("MD_IDENTIFIER").get(0).getValues().get(0).getComboValueShort(0));
    }
}