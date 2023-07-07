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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataWrapper;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

public class SearchHitFactoryTest extends AbstractSolrEnabledTest {

    @Test
    public void createSearchHit_findWithUmlaut() throws PresentationException, IndexUnreachableException {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, Long.toString(1l));
        doc.setField("MD_CREATOR", "Norden");
        doc.setField("MD_PUBLISHER", "Nørre");
        Map<String, Set<String>> searchTerms = Collections.singletonMap(SolrConstants.DEFAULT, Collections.singleton("Nörde~1"));
        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN).createSearchHit(doc, null, null, null);
        assertEquals(1, hit.getFoundMetadata().size());
        assertEquals(1, hit.getFoundMetadata().size());
    }

    @Test
    public void createSearchHit_findUmlaute() throws PresentationException, IndexUnreachableException {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, Long.toString(1l));
        doc.setField("MD_CREATOR", "Nörden");
        doc.setField("MD_PUBLISHER", "Nørre");
        Map<String, Set<String>> searchTerms = Collections.singletonMap(SolrConstants.DEFAULT, Collections.singleton("Norde~1"));
        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN).createSearchHit(doc, null, null, null);
        assertEquals(1, hit.getFoundMetadata().size());
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies add metadata fields that match search terms
     */
    @Test
    public void findAdditionalMetadataFieldsContainingSearchTerms_shouldAddMetadataFieldsThatMatchSearchTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "label", null, Locale.ENGLISH, null, null);

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR"));
        se.getMetadataFields().put("MD_YEARPUBLISH", Collections.singletonList("ca. 1984"));
        Assert.assertEquals(2, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        searchTerms.put("MD_YEARPUBLISH", new HashSet<>(Arrays.asList(new String[] { "1984" })));

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);
        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());

        Assert.assertEquals(2, result.size());
        if (!result.isEmpty()) {
            for (MetadataWrapper mw : result) {
                be.getMetadataList().add(mw.getMetadata());
            }
        }

        Assert.assertNotNull(be.getMetadataList("MD_TITLE"));
        List<Metadata> mdList1 = be.getMetadataList("MD_TITLE");
        Assert.assertFalse(mdList1.get(0).getValues().isEmpty());
        Assert.assertEquals("FROM <span class=\"search-list--highlight\">FOO</span> TO <span class=\"search-list--highlight\">BAR</span>",
                mdList1.get(0).getValues().get(0).getComboValueShort(0));
        Assert.assertNotNull(be.getMetadataList("MD_YEARPUBLISH"));

        List<Metadata> mdList2 = be.getMetadataList("MD_YEARPUBLISH");
        Assert.assertFalse(mdList2.get(0).getValues().isEmpty());
        Assert.assertEquals("ca. <span class=\"search-list--highlight\">1984</span>", mdList2.get(0).getValues().get(0).getComboValueShort(0));
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies not add duplicates from default terms
     */
    @Test
    public void findAdditionalMetadataFieldsContainingSearchTerms_shouldNotAddDuplicatesFromDefaultTerms() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN);

        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR")); // same value as the main label
        Assert.assertEquals(1, se.getMetadataFields().size());

        List<MetadataWrapper> result =
                factory.findAdditionalMetadataFieldsContainingSearchTerms(se.getMetadataFields(), searchTerms, be.getMetadataFieldNames(),
                        String.valueOf(se.getLuceneId()), be.getLabel());

        Assert.assertTrue(result.isEmpty());
        Assert.assertTrue(be.getMetadataList("MD_TITLE").isEmpty());

    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies not add duplicates from explicit terms
     */
    @Test
    public void findAdditionalMetadataFieldsContainingSearchTerms_shouldNotAddDuplicatesFromExplicitTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR")); // same value as the main label
        Assert.assertEquals(1, se.getMetadataFields().size());

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

        Assert.assertEquals(1, be.getMetadataList("MD_TITLE").size());
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies not add ignored fields
     */
    @Test
    public void findAdditionalMetadataFieldsContainingSearchTerms_shouldNotAddIgnoredFields() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_IGNOREME", Collections.singletonList("foo ignores bar"));
        Assert.assertEquals(1, se.getMetadataFields().size());

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

        Assert.assertEquals(0, be.getMetadataList("MD_IGNOREME").size());
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies translate configured field values correctly
     */
    @Test
    public void findAdditionalMetadataFieldsContainingSearchTerms_shouldTranslateConfiguredFieldValuesCorrectly() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put(SolrConstants.DC, Collections.singletonList("admin"));
        Assert.assertEquals(1, se.getMetadataFields().size());

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

        Assert.assertEquals(1, be.getMetadataList(SolrConstants.DC).size());
    }

    /**
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies write one line fields into a single string
     */
    @Test
    public void findAdditionalMetadataFieldsContainingSearchTerms_shouldWriteOneLineFieldsIntoASingleString() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, Locale.ENGLISH, null, null);
        be.getMetadataList().add(new Metadata("", "MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_COUNT_EN", Arrays.asList(new String[] { "one", "two", "three" }));
        se.getMetadataFields().put("MD_COUNT_JP", Arrays.asList(new String[] { "ichi", "ni", "san" }));
        Assert.assertEquals(2, se.getMetadataFields().size());

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
     * @see SearchHitFactory#findAdditionalMetadataFieldsContainingSearchTerms(List,Map,String,String,Map)
     * @verifies truncate snippet fields correctly
     */
    @Test
    public void findAdditionalMetadataFieldsContainingSearchTerms_shouldTruncateSnippetFieldsCorrectly() throws Exception {
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
}