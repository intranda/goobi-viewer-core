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
package io.goobi.viewer.model.crowdsourcing;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent.ContentType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

class DisplayUserGeneratedContentTest extends AbstractSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractSolrEnabledTest.setUpClass();
    }

    /**
     * @see DisplayUserGeneratedContent#buildFromSolrDoc(SolrDocument)
     * @verifies populate type, coordinates, label, and access condition from SolrDocument fields
     */
    @Test
    void buildFromSolrDoc_shouldPopulateTypeCoordinatesLabelAndAccessConditionFromSolrDocumentFields() throws Exception {
        String coords = "1468.0, 2459.0, 1938.0, 2569.0";

        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "123");
        doc.setField(SolrConstants.UGCTYPE, ContentType.PERSON.name());
        doc.setField(SolrConstants.UGCCOORDS, coords);
        doc.setField("MD_FIRSTNAME", "John");
        doc.setField("MD_LASTNAME", "Doe");
        doc.setField(SolrConstants.ACCESSCONDITION, "restricted");

        DisplayUserGeneratedContent ugc = DisplayUserGeneratedContent.buildFromSolrDoc(doc);
        Assertions.assertNotNull(ugc);
        Assertions.assertEquals(ContentType.PERSON, ugc.getType());
        Assertions.assertEquals(coords, ugc.getAreaString());
        Assertions.assertEquals(coords, ugc.getDisplayCoordinates());
        Assertions.assertEquals("Doe, John", ugc.getLabel());
        Assertions.assertEquals("restricted", ugc.getAccessCondition());
    }

    /**
     * @see DisplayUserGeneratedContent#generateUgcLabel(StructElement)
     * @verifies generate person label correctly
     */
    @Test
    void generateUgcLabel_shouldGeneratePersonLabelCorrectly() throws Exception {
        // Build a SolrDocument with PERSON type and name fields
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "200");
        doc.setField(SolrConstants.UGCTYPE, "PERSON");
        doc.setField("MD_FIRSTNAME", "Jane");
        doc.setField("MD_LASTNAME", "Smith");

        StructElement se = new StructElement("200", doc);
        String label = DisplayUserGeneratedContent.generateUgcLabel(se);

        // Person label is "lastname, firstname"
        Assertions.assertEquals("Smith, Jane", label);
    }

    /**
     * @see DisplayUserGeneratedContent#generateUgcLabel(StructElement)
     * @verifies generate corporation label correctly
     */
    @Test
    void generateUgcLabel_shouldGenerateCorporationLabelCorrectly() throws Exception {
        // Build a SolrDocument with CORPORATION type and corporation fields
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "201");
        doc.setField(SolrConstants.UGCTYPE, "CORPORATION");
        doc.setField("MD_CORPORATION", "Acme Inc.");
        doc.setField("MD_ADDRESS", "123 Main St");

        StructElement se = new StructElement("201", doc);
        String label = DisplayUserGeneratedContent.generateUgcLabel(se);

        // Corporation label appends "corp (corp)" when address is present
        Assertions.assertEquals("Acme Inc. (Acme Inc.)", label);
    }

    /**
     * @see DisplayUserGeneratedContent#generateUgcLabel(StructElement)
     * @verifies generate address label correctly
     */
    @Test
    void generateUgcLabel_shouldGenerateAddressLabelCorrectly() throws Exception {
        // Build a SolrDocument with ADDRESS type and address fields
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "202");
        doc.setField(SolrConstants.UGCTYPE, "ADDRESS");
        doc.setField("MD_STREET", "Hauptstr.");
        doc.setField("MD_HOUSENUMBER", "42");
        doc.setField("MD_CITY", "Berlin");
        doc.setField("MD_COUNTRY", "Germany");

        StructElement se = new StructElement("202", doc);
        String label = DisplayUserGeneratedContent.generateUgcLabel(se);

        // Address label joins street+housenumber, city, country with comma separator
        Assertions.assertEquals("Hauptstr., 42, Berlin, Germany", label);
    }

    /**
     * @see DisplayUserGeneratedContent#generateUgcLabel(StructElement)
     * @verifies generate comment label correctly
     */
    @Test
    void generateUgcLabel_shouldGenerateCommentLabelCorrectly() throws Exception {
        // Build a SolrDocument with COMMENT type and text field
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "203");
        doc.setField(SolrConstants.UGCTYPE, "COMMENT");
        doc.setField("MD_TEXT", "This is a test comment");

        StructElement se = new StructElement("203", doc);
        String label = DisplayUserGeneratedContent.generateUgcLabel(se);

        // Comment label returns the escaped text value
        Assertions.assertEquals("This is a test comment", label);
    }

    /**
     * @see DisplayUserGeneratedContent#generateUgcLabel(StructElement)
     * @verifies return label field value if ugc type unknown
     */
    @Test
    void generateUgcLabel_shouldReturnLabelFieldValueIfUgcTypeUnknown() throws Exception {
        // Build a SolrDocument with an unknown UGC type that falls to default branch
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "204");
        doc.setField(SolrConstants.UGCTYPE, "UNKNOWNTYPE");
        doc.setField(SolrConstants.LABEL, "Fallback Label");

        StructElement se = new StructElement("204", doc);
        String label = DisplayUserGeneratedContent.generateUgcLabel(se);

        // Unknown type falls through to default branch which returns LABEL field
        Assertions.assertEquals("Fallback Label", label);
    }

    /**
     * @see DisplayUserGeneratedContent#generateUgcLabel(StructElement)
     * @verifies return text value for all types if no other fields exist
     */
    @Test
    void generateUgcLabel_shouldReturnTextValueForAllTypesIfNoOtherFieldsExist() throws Exception {
        // Build a SolrDocument with ADDRESS type but only MD_TEXT, no address-specific fields;
        // ADDRESS is the type whose label generation falls back to text when no address fields exist
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "205");
        doc.setField(SolrConstants.UGCTYPE, "ADDRESS");
        doc.setField("MD_TEXT", "Fallback text value");

        StructElement se = new StructElement("205", doc);
        String label = DisplayUserGeneratedContent.generateUgcLabel(se);

        // When no address-specific fields exist, the address label falls back to text
        Assertions.assertEquals("Fallback text value", label);
    }
}
