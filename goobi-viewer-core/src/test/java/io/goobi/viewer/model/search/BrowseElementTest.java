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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

class BrowseElementTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List)
     * @verifies add sort fields correctly
     */
    @Test
    void addSortFieldsToMetadata_shouldAddSortFieldsCorrectly() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), null);
        assertEquals(1, be.getMetadataList().size());
        assertEquals("SORT_FOO", be.getMetadataList().get(0).getLabel());
        assertEquals(1, be.getMetadataList().get(0).getValues().size());
        assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().size());
        assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).size());
        assertEquals("bar", be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).get(0));
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List,Set)
     * @verifies not add fields on ignore list
     */
    @Test
    void addSortFieldsToMetadata_shouldNotAddFieldsOnIgnoreList() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), Collections.singleton("SORT_FOO"));
        assertEquals(0, be.getMetadataList().size());
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List)
     * @verifies not add fields already in the list
     */
    @Test
    void addSortFieldsToMetadata_shouldNotAddFieldsAlreadyInTheList() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));

        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.getMetadataList().add(new Metadata(String.valueOf(se.getLuceneId()), "MD_FOO", "", "old value"));

        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), null);
        assertEquals(1, be.getMetadataList().size());
        assertEquals("MD_FOO", be.getMetadataList().get(0).getLabel());
        assertEquals(1, be.getMetadataList().get(0).getValues().size());
        assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().size());
        assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).size());
        assertEquals("old value", be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).get(0));
    }

    /**
     * @see BrowseElement#getMimeTypeFromExtension(String)
     * @verifies return empty string for unknown file extensions
     */
    @Test
    void getMimeTypeFromExtension_shouldReturnEmptyStringForUnknownFileExtensions() {
        assertEquals("", BrowseElement.getMimeTypeFromExtension("file:///opt/digiverso/foo.bar"));
    }

    /**
     * @see BrowseElement#generateDefaultLabel(StructElement)
     * @verifies translate docstruct label
     */
    @Test
    void generateDefaultLabel_shouldTranslateDocstructLabel() {
        //        BrowseElement be = new BrowseElement("PPN123", 1, null, null, Locale.GERMAN, null, null);
        StructElement se = new StructElement();
        se.setDocStructType("Monograph");
        String label = BrowseElement.generateDefaultLabel(se, Locale.GERMAN);
        assertEquals("Monografie", label);
    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any line breaks
     */
    @Test
    void getFulltextForHtml_shouldRemoveAnyLineBreaks() {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", "foo\nbar", Locale.ENGLISH, null, null);
        assertEquals("foo bar", be.getFulltextForHtml());
    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any JS
     */
    @Test
    void getFulltextForHtml_shouldRemoveAnyJS() {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR",
                "foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar", Locale.ENGLISH, null, null);
        assertEquals("foo  bar", be.getFulltextForHtml());
    }

    @Test
    void test_createMultiLanguageLabel() throws IndexUnreachableException {
        BrowseElement browseElement = new BrowseElement("PI", 0, "bla", "text", Locale.ENGLISH, "/data/1", "url");
        StructElement structElement = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, Long.valueOf(12345),
                "MD_TITLE_LANG_DE", "Mein Titel",
                "MD_TITLE_LANG_EN", "My title",
                "MD_TITLE_LANG_FR", "Mon titre")));
        IMetadataValue label = browseElement.createMultiLanguageLabel(structElement);
        assertEquals("Mein Titel", label.getValueOrFallback(Locale.GERMAN));
        assertEquals("My title", label.getValueOrFallback(Locale.ENGLISH));
        assertEquals("Mein Titel", label.getValueOrFallback(Locale.FRENCH)); // French is not among the Faces languages
    }
}
