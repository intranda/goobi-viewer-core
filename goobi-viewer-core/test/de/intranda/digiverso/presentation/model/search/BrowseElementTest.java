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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

public class BrowseElementTest extends AbstractSolrEnabledTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractSolrEnabledTest.setUpClass();
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map,Locale)
     * @verifies add metadata fields that match search terms
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldAddMetadataFieldsThatMatchSearchTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "label", null, false, Locale.ENGLISH, null);

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR"));
        se.getMetadataFields().put("MD_YEARPUBLISH", Collections.singletonList("ca. 1984"));
        Assert.assertEquals(2, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        searchTerms.put("MD_YEARPUBLISH", new HashSet<>(Arrays.asList(new String[] { "1984" })));

        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, null);
        Assert.assertEquals(2, be.getAdditionalMetadataList().size());
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
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, false, Locale.ENGLISH, null);

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR")); // same value as the main label
        Assert.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));

        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, null);
        Assert.assertTrue(be.getMetadataList("MD_TITLE").isEmpty());
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map)
     * @verifies not add duplicates from explicit terms
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldNotAddDuplicatesFromExplicitTerms() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, false, Locale.ENGLISH, null);
        be.getMetadataList().add(new Metadata("MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_TITLE", Collections.singletonList("FROM FOO TO BAR")); // same value as the main label
        Assert.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put("MD_TITLE", new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));

        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, null);
        Assert.assertEquals(1, be.getMetadataList("MD_TITLE").size());
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map,Set)
     * @verifies not add ignored fields
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldNotAddIgnoredFields() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, false, Locale.ENGLISH, null);
        be.getMetadataList().add(new Metadata("MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put("MD_IGNOREME", Collections.singletonList("foo ignores bar"));
        Assert.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));

        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, new HashSet<>(Collections.singletonList("MD_IGNOREME")), null);
        Assert.assertEquals(0, be.getMetadataList("MD_IGNOREME").size());
    }

    /**
     * @see BrowseElement#addAdditionalMetadataContainingSearchTerms(StructElement,Map,Set,Set)
     * @verifies translate configured field values correctly
     */
    @Test
    public void addAdditionalMetadataContainingSearchTerms_shouldTranslateConfiguredFieldValuesCorrectly() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", null, false, Locale.ENGLISH, null);
        be.getMetadataList().add(new Metadata("MD_TITLE", "", "FROM FOO TO BAR"));

        StructElement se = new StructElement();
        se.getMetadataFields().put(SolrConstants.DC, Collections.singletonList("admin"));
        Assert.assertEquals(1, se.getMetadataFields().size());

        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })));
        searchTerms.put(SolrConstants.DC, new HashSet<>(Arrays.asList(new String[] { "admin" })));

        String[] translateFields = { SolrConstants.DC };
        be.addAdditionalMetadataContainingSearchTerms(se, searchTerms, null, new HashSet<>(Arrays.asList(translateFields)));
        Assert.assertEquals(1, be.getMetadataList(SolrConstants.DC).size());
    }

    /**
     * @see BrowseElement#generateDefaultLabel(StructElement)
     * @verifies translate docstruct label
     */
    @Test
    public void generateDefaultLabel_shouldTranslateDocstructLabel() throws Exception {
        BrowseElement be = new BrowseElement("PPN123", 1, null, null, false, Locale.GERMAN, null);
        StructElement se = new StructElement();
        se.setDocStructType("Monograph");
        String label = BrowseElement.generateDefaultLabel(se, Locale.GERMAN);
        Assert.assertEquals("Monographie", label);
    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any line breaks
     */
    @Test
    public void getFulltextForHtml_shouldRemoveAnyLineBreaks() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", "foo\nbar", false, Locale.ENGLISH, null);
        Assert.assertEquals("foo bar", be.getFulltextForHtml());

    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any JS
     */
    @Test
    public void getFulltextForHtml_shouldRemoveAnyJS() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR",
                "foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar", false, Locale.ENGLISH, null);
        Assert.assertEquals("foo  bar", be.getFulltextForHtml());
    }

}