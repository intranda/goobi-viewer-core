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
package de.intranda.digiverso.presentation.model.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;

public class SearchHitTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument)
     * @verifies add field values pairs that match search terms
     */
    @Test
    public void populateFoundMetadata_shouldAddFieldValuesPairsThatMatchSearchTerms() throws Exception {
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
        doc.addField("MD_TITLE", "FROM FOO TO BAR");
        doc.addField("MD_SUBTITLE", "FROM BAR TO FOO");
        doc.addField("MD_2", "bla blup");
        doc.addField("MD_3", "none of the above");

        SearchHit hit = SearchHit.createSearchHit(doc, null, Locale.ENGLISH, null, searchTerms, null, false, null, null, null);
        Assert.assertNotNull(hit);
        Assert.assertEquals(2, hit.getFoundMetadata().size());
        Assert.assertEquals("Subtitle", hit.getFoundMetadata().get(0).getOne());
        Assert.assertEquals("FROM <span class=\"search-list--highlight\">BAR</span> TO <span class=\"search-list--highlight\">FOO</span>", hit
                .getFoundMetadata().get(0).getTwo());
        Assert.assertEquals("MD_2", hit.getFoundMetadata().get(1).getOne());
        Assert.assertEquals("bla <span class=\"search-list--highlight\">blup</span>", hit.getFoundMetadata().get(1).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument)
     * @verifies add MD fields that contain terms from DEFAULT
     */
    @Test
    public void populateFoundMetadata_shouldAddMDFieldsThatContainTermsFromDEFAULT() throws Exception {
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
        doc.addField("MD_SUBTITLE", "FROM FOO TO BAR"); // do not use MD_TITLE because values == label will be skipped
        doc.addField("MD_2", "bla blup");

        SearchHit hit = SearchHit.createSearchHit(doc, null, Locale.ENGLISH, null, searchTerms, null, false, null, null, null);
        Assert.assertNotNull(hit);
        Assert.assertEquals(2, hit.getFoundMetadata().size());
        Assert.assertEquals("Subtitle", hit.getFoundMetadata().get(0).getOne());
        Assert.assertEquals("FROM <span class=\"search-list--highlight\">FOO</span> TO <span class=\"search-list--highlight\">BAR</span>", hit
                .getFoundMetadata().get(0).getTwo());
        Assert.assertEquals("MD_2", hit.getFoundMetadata().get(1).getOne());
        Assert.assertEquals("bla <span class=\"search-list--highlight\">blup</span>", hit.getFoundMetadata().get(1).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument)
     * @verifies not add duplicate values
     */
    @Test
    public void populateFoundMetadata_shouldNotAddDuplicateValues() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DEFAULT, terms);
            terms.add("john");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField("MD_AUTHOR", "Doe, John");
        doc.addField("MD_AUTHOR" + SolrConstants._UNTOKENIZED, "Doe, John");

        SearchHit hit = SearchHit.createSearchHit(doc, null, Locale.ENGLISH, null, searchTerms, null, false, null, null, null);
        Assert.assertNotNull(hit);
        Assert.assertEquals(1, hit.getFoundMetadata().size());
        Assert.assertEquals("Author", hit.getFoundMetadata().get(0).getOne());
        Assert.assertEquals("Doe, <span class=\"search-list--highlight\">John</span>", hit.getFoundMetadata().get(0).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument,Set)
     * @verifies not add ignored fields
     */
    @Test
    public void populateFoundMetadata_shouldNotAddIgnoredFields() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DEFAULT, terms);
            terms.add("john");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField("MD_AUTHOR", "Doe, John");
        doc.addField("MD_AUTHOR" + SolrConstants._UNTOKENIZED, "Doe, John");
        doc.addField("T-1000", "Call to John now.");

        SearchHit hit = SearchHit.createSearchHit(doc, null, Locale.ENGLISH, null, searchTerms, null, false, new HashSet<>(Collections.singletonList(
                "T-1000")), null, null);
        Assert.assertNotNull(hit);
        Assert.assertEquals(1, hit.getFoundMetadata().size());
        Assert.assertEquals("Author", hit.getFoundMetadata().get(0).getOne());
        Assert.assertEquals("Doe, <span class=\"search-list--highlight\">John</span>", hit.getFoundMetadata().get(0).getTwo());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument,Set,Set)
     * @verifies not add field values that equal the label
     */
    @Test
    public void populateFoundMetadata_shouldNotAddFieldValuesThatEqualTheLabel() throws Exception {
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
        doc.addField("MD_TITLE", "FROM FOO TO BAR"); // do not use MD_TITLE because values == label will be skipped
        doc.addField("MD_2", "bla blup");

        SearchHit hit = SearchHit.createSearchHit(doc, null, Locale.ENGLISH, null, searchTerms, null, false, null, null, null);
        Assert.assertNotNull(hit);
        Assert.assertEquals(1, hit.getFoundMetadata().size());
        Assert.assertEquals("MD_2", hit.getFoundMetadata().get(0).getOne());
    }

    /**
     * @see SearchHit#populateFoundMetadata(SolrDocument,Set,Set)
     * @verifies translate configured field values correctly
     */
    @Test
    public void populateFoundMetadata_shouldTranslateConfiguredFieldValuesCorrectly() throws Exception {
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
        doc.addField(SolrConstants.TITLE, "title for label");
        doc.addField(SolrConstants.DC, "admin");
        doc.addField(SolrConstants.DOCSTRCT, "monograph");

        String[] translateFields = { SolrConstants.DC, SolrConstants.DOCSTRCT };
        SearchHit hit = SearchHit.createSearchHit(doc, null, Locale.ENGLISH, null, searchTerms, null, false, null, new HashSet<>(Arrays.asList(
                translateFields)), null);
        Assert.assertNotNull(hit);
        Assert.assertEquals(2, hit.getFoundMetadata().size());
        Assert.assertEquals("Structure type", hit.getFoundMetadata().get(0).getOne());
        Assert.assertEquals("<span class=\"search-list--highlight\">Monograph</span>", hit.getFoundMetadata().get(0).getTwo());
        Assert.assertEquals("Collection", hit.getFoundMetadata().get(1).getOne());
        Assert.assertEquals("<span class=\"search-list--highlight\">Administration</span>", hit.getFoundMetadata().get(1).getTwo());
    }

    /**
     * @see SearchHit#addLabelHighlighting()
     * @verifies modify label correctly
     */
    @Test
    public void addLabelHighlighting_shouldModifyLabelCorrectly() throws Exception {
        Map<String, Set<String>> searchTerms = new HashMap<>();
        {
            Set<String> terms = new HashSet<>();
            searchTerms.put(SolrConstants.DEFAULT, terms);
            terms.add("ipsum");
        }

        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.IDDOC, "1");
        doc.addField("MD_TITLE", SearchHelperTest.LOREM_IPSUM);

        SearchHit hit = SearchHit.createSearchHit(doc, null, Locale.ENGLISH, null, searchTerms, null, false, null, null, null);
        Assert.assertNotNull(hit);
        hit.addLabelHighlighting();
        Assert.assertTrue(hit.getBrowseElement().getLabelShort().startsWith(
                "Lorem <span class=\"search-list--highlight\">ipsum</span> dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore"));
    }
}