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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;

public class BrowseElementTest extends AbstractDatabaseAndSolrEnabledTest {

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
        Assertions.assertEquals(1, be.getMetadataList().size());
        Assertions.assertEquals("SORT_FOO", be.getMetadataList().get(0).getLabel());
        Assertions.assertEquals(1, be.getMetadataList().get(0).getValues().size());
        Assertions.assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().size());
        Assertions.assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).size());
        Assertions.assertEquals("bar", be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).get(0));
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
        Assertions.assertEquals(0, be.getMetadataList().size());
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
        Assertions.assertEquals(1, be.getMetadataList().size());
        Assertions.assertEquals("MD_FOO", be.getMetadataList().get(0).getLabel());
        Assertions.assertEquals(1, be.getMetadataList().get(0).getValues().size());
        Assertions.assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().size());
        Assertions.assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).size());
        Assertions.assertEquals("old value", be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).get(0));
    }

    /**
     * @see BrowseElement#generateDefaultLabel(StructElement)
     * @verifies translate docstruct label
     */
    @Test
    void generateDefaultLabel_shouldTranslateDocstructLabel() throws Exception {
        //        BrowseElement be = new BrowseElement("PPN123", 1, null, null, Locale.GERMAN, null, null);
        StructElement se = new StructElement();
        se.setDocStructType("Monograph");
        String label = BrowseElement.generateDefaultLabel(se, Locale.GERMAN);
        Assertions.assertEquals("Monografie", label);
    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any line breaks
     */
    @Test
    void getFulltextForHtml_shouldRemoveAnyLineBreaks() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", "foo\nbar", Locale.ENGLISH, null, null);
        Assertions.assertEquals("foo bar", be.getFulltextForHtml());

    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any JS
     */
    @Test
    void getFulltextForHtml_shouldRemoveAnyJS() throws Exception {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR",
                "foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar", Locale.ENGLISH, null, null);
        Assertions.assertEquals("foo  bar", be.getFulltextForHtml());
    }
}
