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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.imaging.IIIFUrlHandler;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrTools;

class SearchHitTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument)
     * @verifies add field values pairs that match search terms
     */
    @Test
    void populateFoundMetadata_shouldAddFieldValuesPairsThatMatchSearchTerms() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put("MD_SUBTITLE", terms);
            terms.add("foo");
            terms.add("bar");
        }
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put("MD_2", terms);
            terms.add("blup");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField("MD_TITLE", "FROM FOO TO BAR");
        doc.addField("MD_SUBTITLE", "FROM BAR TO FOO");
        doc.addField("MD_2", "bla blup");
        doc.addField("MD_3", "none of the above");

        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(2, hit.getFoundMetadata().size());
        Assertions.assertEquals("Subtitle", hit.getFoundMetadata().get(0).getOne());
        Assertions.assertEquals("FROM <mark class=\"search-list--highlight\">BAR</mark> TO <mark class=\"search-list--highlight\">FOO</mark>",
                hit.getFoundMetadata().get(0).getTwo());
        Assertions.assertEquals("MD_2", hit.getFoundMetadata().get(1).getOne());
        Assertions.assertEquals("bla <mark class=\"search-list--highlight\">blup</mark>", hit.getFoundMetadata().get(1).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument)
     * @verifies add MD fields that contain terms from DEFAULT
     */
    @Test
    void populateFoundMetadata_shouldAddMDFieldsThatContainTermsFromDEFAULT() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DEFAULT, terms);
            terms.add("foo");
            terms.add("bar");
            terms.add("blup");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField("MD_TITLE", "Any title");
        doc.addField("MD_SUBTITLE", "FROM FOO TO BAR"); // do not use MD_TITLE because values == label will be skipped
        doc.addField("MD_2", "bla blup");

        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(2, hit.getFoundMetadata().size());
        Assertions.assertEquals("Subtitle", hit.getFoundMetadata().get(0).getOne());
        Assertions.assertEquals("FROM <span class=\"search-list--highlight\">FOO</span> TO <span class=\"search-list--highlight\">BAR</span>",
                hit.getFoundMetadata().get(0).getTwo());
        Assertions.assertEquals("MD_2", hit.getFoundMetadata().get(1).getOne());
        Assertions.assertEquals("bla <span class=\"search-list--highlight\">blup</span>", hit.getFoundMetadata().get(1).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument)
     * @verifies not add duplicate values
     */
    @Test
    void populateFoundMetadata_shouldNotAddDuplicateValues() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DEFAULT, terms);
            terms.add("john");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField("MD_TITLE", "Any title");
        doc.addField("MD_AUTHOR", "Doe, John");
        doc.addField("MD_AUTHOR" + SolrConstants.SUFFIX_UNTOKENIZED, "Doe, John");

        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(1, hit.getFoundMetadata().size());
        Assertions.assertEquals("Author", hit.getFoundMetadata().get(0).getOne());
        Assertions.assertEquals("Doe, <span class=\"search-list--highlight\">John</span>", hit.getFoundMetadata().get(0).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument,Set)
     * @verifies not add ignored fields
     */
    @Test
    void populateFoundMetadata_shouldNotAddIgnoredFields() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DEFAULT, terms);
            terms.add("john");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField("MD_TITLE", "Any title");
        doc.addField("MD_AUTHOR", "Doe, John");
        doc.addField("MD_AUTHOR" + SolrConstants.SUFFIX_UNTOKENIZED, "Doe, John");
        doc.addField("T-1000", "Call to John now.");

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH);
        factory.getAdditionalMetadataIgnoreFields().add("T-1000");
        SearchHit hit = factory.createSearchHit(doc, null, null, null);
        new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(1, hit.getFoundMetadata().size());
        Assertions.assertEquals("Author", hit.getFoundMetadata().get(0).getOne());
        Assertions.assertEquals("Doe, <span class=\"search-list--highlight\">John</span>", hit.getFoundMetadata().get(0).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument,Set,Set)
     * @verifies not add field values that equal the label
     */
    @Test
    void populateFoundMetadata_shouldNotAddFieldValuesThatEqualTheLabel() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DEFAULT, terms);
            terms.add("foo");
            terms.add("bar");
            terms.add("blup");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField("MD_TITLE", "FROM FOO TO BAR"); // do not use MD_TITLE because values == label will be skipped
        doc.addField("MD_2", "bla blup");

        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(1, hit.getFoundMetadata().size());
        Assertions.assertEquals("MD_2", hit.getFoundMetadata().get(0).getOne());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument,Set,Set)
     * @verifies translate configured field values correctly
     */
    @Test
    void populateFoundMetadata_shouldTranslateConfiguredFieldValuesCorrectly() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DC, terms);
            terms.add("admin");
        }
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DOCSTRCT, terms);
            terms.add("monograph");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField(SolrConstants.TITLE, "title for label");
        doc.addField(SolrConstants.DC, "admin");
        doc.addField(SolrConstants.DOCSTRCT, "monograph");

        String[] translateFields = { SolrConstants.DC, SolrConstants.DOCSTRCT };
        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH);
        factory.getAdditionalMetadataTranslateFields().addAll(Arrays.asList(translateFields));
        SearchHit hit = factory.createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(2, hit.getFoundMetadata().size());
        Assertions.assertEquals("Structure type", hit.getFoundMetadata().get(0).getOne());
        Assertions.assertEquals("<span class=\"search-list--highlight\">Monograph</span>", hit.getFoundMetadata().get(0).getTwo());
        Assertions.assertEquals("Collection", hit.getFoundMetadata().get(1).getOne());
        Assertions.assertEquals("<span class=\"search-list--highlight\">Administration</span>", hit.getFoundMetadata().get(1).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument,Set,Set,Set,Set)
     * @verifies write one line fields into a single string
     */
    @Test
    void populateFoundMetadata_shouldWriteOneLineFieldsIntoASingleString() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "bat", "hiru" })));
        searchTerms.put("MD_COUNT_SE", new HashSet<>(Arrays.asList(new String[] { "ett", "två" })));

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField("MD_TITLE", "Any title");
        doc.addField("MD_COUNT_EU", "bat");
        doc.addField("MD_COUNT_EU", "bi");
        doc.addField("MD_COUNT_EU", "hiru");
        doc.addField("MD_COUNT_SE", "ett");
        doc.addField("MD_COUNT_SE", "två");
        doc.addField("MD_COUNT_SE", "tre");

        String[] oneLineFields = { "MD_COUNT_EU", "MD_COUNT_SE" };
        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH);
        factory.getAdditionalMetadataOneLineFields().addAll(Arrays.asList(oneLineFields));
        SearchHit hit = factory.createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(2, hit.getFoundMetadata().size());

        // Via explicit term field
        Assertions.assertEquals("MD_COUNT_SE", hit.getFoundMetadata().get(0).getOne());
        Assertions.assertEquals("<span class=\"search-list--highlight\">ett</span>, <span class=\"search-list--highlight\">två</span>",
                hit.getFoundMetadata().get(0).getTwo());
        // Via DEFAULT
        Assertions.assertEquals("MD_COUNT_EU", hit.getFoundMetadata().get(1).getOne());
        Assertions.assertEquals("<span class=\"search-list--highlight\">bat</span>, <span class=\"search-list--highlight\">hiru</span>",
                hit.getFoundMetadata().get(1).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument,Set,Set,Set,Set)
     * @verifies truncate snippet fields correctly
     */
    @Test
    void populateFoundMetadata_shouldTruncateSnippetFieldsCorrectly() throws Exception {
        int maxLength = 50;
        DataManager.getInstance().getConfiguration().overrideValue("search.fulltextFragmentLength", maxLength);

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "labore" })));
        searchTerms.put("MD_SOMETEXT", new HashSet<>(Arrays.asList(new String[] { "ipsum" })));

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField("MD_TITLE", "Any title");
        doc.addField("MD_DESCRIPTION", StringConstants.LOREM_IPSUM);
        doc.addField("MD_SOMETEXT", StringConstants.LOREM_IPSUM.replace("labore", "foo")); // prevent matches via DEFAULT

        SearchHitFactory factory = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH);
        factory.getAdditionalMetadataSnippetFields().addAll(Collections.singletonList("MD_SOMETEXT"));
        Assertions.assertEquals(2, factory.getAdditionalMetadataSnippetFields().size());
        SearchHit hit = factory.createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(2, hit.getFoundMetadata().size());

        // Via DEFAULT
        Assertions.assertEquals("MD_DESCRIPTION", hit.getFoundMetadata().get(0).getOne());
        Assertions.assertTrue(hit.getFoundMetadata().get(0).getTwo().length() <= maxLength + 56);
        // Truncated snippet is randomized, so cannot test the exact value
        Assertions.assertTrue(hit.getFoundMetadata().get(0).getTwo().contains("ut <span class=\"search-list--highlight\">labore</span> et"));

        // Via explicit term field
        Assertions.assertEquals("MD_SOMETEXT", hit.getFoundMetadata().get(1).getOne());
        Assertions.assertTrue(hit.getFoundMetadata().get(1).getTwo().length() <= maxLength + 56);
        // Truncated snippet is randomized, so cannot test the exact value
        Assertions.assertTrue(hit.getFoundMetadata().get(1).getTwo().contains("<span class=\"search-list--highlight\">ipsum</span> dolor"));
    }

    /**
     * @see SearchHit#addLabelHighlighting()
     * @verifies modify label correctly from default
     */
    @Test
    void addLabelHighlighting_shouldModifyLabelCorrectlyFromDefault() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DEFAULT, terms);
            terms.add("ipsum");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField(SolrConstants.TITLE, StringConstants.LOREM_IPSUM);

        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        hit.addLabelHighlighting();
        Assertions.assertTrue(hit.getBrowseElement()
                .getLabelShort()
                .startsWith(
                        "Lorem <span class=\"search-list--highlight\">ipsum</span> dolor sit amet, consectetur adipisicing elit,"
                                + " sed do eiusmod tempor incididunt ut labore"),
                "label: " + hit.getBrowseElement().getLabelShort());
    }

    /**
     * @see SearchHit#addLabelHighlighting()
     * @verifies modify label correctly from title
     */
    @Test
    void addLabelHighlighting_shouldModifyLabelCorrectlyFromTitle() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.TITLE, terms);
            terms.add("ipsum");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField(SolrConstants.TITLE, StringConstants.LOREM_IPSUM);

        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        hit.addLabelHighlighting();
        Assertions.assertTrue(hit.getBrowseElement()
                .getLabelShort()
                .startsWith(
                        "Lorem <span class=\"search-list--highlight\">ipsum</span> dolor sit amet, consectetur adipisicing elit,"
                                + " sed do eiusmod tempor incididunt ut labore"),
                "label: " + hit.getBrowseElement().getLabelShort());
    }

    /**
     * @see SearchHit#addLabelHighlighting()
     * @verifies do nothing if searchTerms null
     */
    @Test
    void addLabelHighlighting_shouldDoNothingIfSearchTermsNull() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField(SolrConstants.TITLE, StringConstants.LOREM_IPSUM);

        SearchHit hit = new SearchHitFactory(null, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        hit.addLabelHighlighting();
        Assertions.assertEquals(StringConstants.LOREM_IPSUM, hit.getBrowseElement().getLabelShort(),
                "label: " + hit.getBrowseElement().getLabelShort());
    }

    /**
     * @see SearchHit#addCMSPageChildren()
     * @verifies do nothing if searchTerms do not contain key
     */
    @Test
    void addCMSPageChildren_shouldDoNothingIfSearchTermsDoNotContainKey() throws Exception {
        SearchHit hit = new SearchHit(HitType.DOCSTRCT, null, null, null, null, null);
        Assertions.assertEquals(0, hit.getChildren().size());
        hit.addCMSPageChildren();
        Assertions.assertEquals(0, hit.getChildren().size());
    }

    /**
     * @see SearchHit#addCMSPageChildren()
     * @verifies do nothing if no cms pages for record found
     */
    @Test
    void addCMSPageChildren_shouldDoNothingIfNoCmsPagesForRecordFound() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.CMS_TEXT_ALL, terms);
            terms.add("ipsum");
        }
        SearchHit hit =
                new SearchHit(HitType.DOCSTRCT, new BrowseElement("PPN123", 1, "Hello World", null, null, null, null), null, searchTerms, null, null);
        Assertions.assertEquals(0, hit.getChildren().size());
        hit.addCMSPageChildren();
        Assertions.assertEquals(0, hit.getChildren().size());
    }

    /**
     * @see SearchHit#addFulltextChild(SolrDocument,String)
     * @verifies throw IllegalArgumentException if doc null
     */
    @Test
    void addFulltextChild_shouldThrowIllegalArgumentExceptionIfDocNull() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField(SolrConstants.TITLE, StringConstants.LOREM_IPSUM);

        SearchHit hit = new SearchHitFactory(null, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertThrows(IllegalArgumentException.class, () -> hit.addFulltextChild(null, "en"));
    }

    /**
     * @see SearchHit#addFulltextChild(SolrDocument,String)
     * @verifies do nothing if searchTerms does not contain fulltext
     */
    @Test
    void addFulltextChild_shouldDoNothingIfSearchTermsDoesNotContainFulltext() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.TITLE, terms);
            terms.add("ipsum");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField(SolrConstants.TITLE, StringConstants.LOREM_IPSUM);

        SearchHit hit =
                // SearchHit.createSearchHit(doc, null, null, Locale.ENGLISH, null, searchTerms, null, null, null, null, null, null, 0, null);
                new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(0, hit.getChildren().size());

        SolrDocument pageDoc = new SolrDocument();
        pageDoc.addField(SolrConstants.IDDOC, "2");
        doc.addField(SolrConstants.DOCTYPE, DocType.PAGE);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");

        hit.addFulltextChild(pageDoc, "en");
        Assertions.assertEquals(0, hit.getChildren().size());
    }

    /**
     * @see SearchHit#addFulltextChild(SolrDocument,String)
     * @verifies do nothing if tei file name not found
     */
    @Test
    void addFulltextChild_shouldDoNothingIfTeiFileNameNotFound() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.FULLTEXT, terms);
            terms.add("ipsum");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");
        doc.addField(SolrConstants.TITLE, StringConstants.LOREM_IPSUM);

        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(0, hit.getChildren().size());

        SolrDocument pageDoc = new SolrDocument();
        pageDoc.addField(SolrConstants.IDDOC, "2");
        doc.addField(SolrConstants.DOCTYPE, DocType.PAGE);
        doc.addField(SolrConstants.PI_TOPSTRUCT, "PPN123");

        hit.addFulltextChild(pageDoc, "en");
        Assertions.assertEquals(0, hit.getChildren().size());
    }

    /**
     * @see SearchHit#generateNotificationFragment(int)
     * @verifies generate fragment correctly
     */
    @Test
    void generateNotificationFragment_shouldGenerateFragmentCorrectly() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + AbstractSolrEnabledTest.PI_KLEIUNIV, null);
        Assertions.assertNotNull(doc);
        String title = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
        Assertions.assertNotNull(title);

        SearchHit hit =
                new SearchHitFactory(null, null, null, 0, new ThumbnailHandler(new IIIFUrlHandler(new ApiUrls(ApiUrls.API)), "/foo/bar/"),
                        Locale.ENGLISH).createSearchHit(doc, null, null, null);
        Assertions.assertNotNull(hit);

        int count = 3;
        String fragment = hit.generateNotificationFragment(count);
        Assertions.assertEquals("<tr><td>" + count + ".</td><td><img src=\""
                + "/api/v1/records/PPN517154005/files/images/00000001.tif/full/!10,11/0/default.jpg" + "\" alt=\"" + title
                + "\" /></td><td>" + title
                + "</td></tr>", fragment);
    }
}
