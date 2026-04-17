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
package io.goobi.viewer.model.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

class MetadataContainerTest extends AbstractTest {

    /**
     * @verifies translate fields from single document
     */
    @Test
    void getValues_shouldTranslateFieldsFromSingleDocument() {

        SolrDocument doc = new SolrDocument(Map.of(
                "MD_ROLE_LANG_FR", "Curateur.rice",
                "MD_ROLE_LANG_DE", "Luxembugrische_r Kurator_in",
                "MD_ROLE_LANG_EN", "Curator",
                "MD_ROLE", "curator"));

        MetadataContainer record = MetadataContainer.createMetadataEntity(doc);

        assertEquals(1, record.getValues("MD_ROLE", null).size());
        assertEquals("curator", record.getValues("MD_ROLE", null).get(0));
        assertEquals("curator", record.getValues("MD_ROLE").get(0));
        assertEquals("Curateur.rice", record.getValues("MD_ROLE", Locale.FRANCE).get(0));
    }

    /**
     * @verifies translate fields from multiple documents
     */
    @Test
    void getValues_shouldTranslateFieldsFromMultipleDocuments() {

        SolrDocument main = new SolrDocument(Map.of(
                "PI", "1234"));

        SolrDocument docFr = new SolrDocument(Map.of(
                "LABEL", "MD_ROLE_LANG_FR",
                "MD_VALUE", "Curateur.rice",
                "DOCTYPE", "METADATA",
                "MD_REFID", "a1"));

        SolrDocument docEn = new SolrDocument(Map.of(
                "LABEL", "MD_ROLE_LANG_EN",
                "MD_VALUE", "Curator",
                "DOCTYPE", "METADATA",
                "MD_REFID", "a1"));

        SolrDocument docDe = new SolrDocument(Map.of(
                "LABEL", "MD_ROLE_LANG_DE",
                "MD_VALUE", "Luxembugrische_r Kurator_in",
                "DOCTYPE", "METADATA",
                "MD_REFID", "a1"));

        SolrDocument docBase = new SolrDocument(Map.of(
                "LABEL", "MD_ROLE",
                "MD_VALUE", "curator",
                "DOCTYPE", "METADATA",
                "MD_REFID", "a1"));

        MetadataContainer record = MetadataContainer.createMetadataEntity(main, List.of(docFr, docEn, docDe, docBase), e -> true, e -> true);

        assertEquals(1, record.getValues("MD_ROLE", null).size());
        assertEquals("curator", record.getValues("MD_ROLE", null).get(0));
        assertEquals("curator", record.getValues("MD_ROLE").get(0));
        assertEquals("Curateur.rice", record.getValues("MD_ROLE", Locale.FRANCE).get(0));
    }

    /**
     * Verify that {@code createMetadataEntity(StructElement)} strips the {@code _UNTOKENIZED}
     * suffix and merges the base field and its tokenized variant under a single map key, so that
     * the resulting container contains exactly one entry per logical field name.
     * @verifies merge untokenized fields into base field key
     */
    @Test
    void getMetadata_shouldMergeUntokenizedFieldsIntoBaseFieldKey() throws IndexUnreachableException {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "1");
        doc.setField(SolrConstants.ISWORK, "true");
        doc.setField("MD_TITLE", "My Title");
        doc.setField("MD_TITLE_UNTOKENIZED", "My Title");
        doc.setField("MD_AUTHOR", "Some Author");
        // MD_AUTHOR has no UNTOKENIZED variant, so it appears exactly once
        StructElement element = new StructElement("1", doc);

        MetadataContainer container = MetadataContainer.createMetadataEntity(element);

        // MD_TITLE_UNTOKENIZED must not survive as an independent key
        assertFalse(container.getMetadata().containsKey("MD_TITLE_UNTOKENIZED"),
                "MD_TITLE_UNTOKENIZED must not appear as a separate key after createMetadataEntity");
        // The base key MD_TITLE must be present and contain values from both source fields
        assertTrue(container.getMetadata().containsKey("MD_TITLE"),
                "MD_TITLE must be present after deduplication");
        List<IMetadataValue> titleValues = container.getMetadata().get("MD_TITLE");
        assertEquals(2, titleValues.size(),
                "MD_TITLE must accumulate values from both MD_TITLE and MD_TITLE_UNTOKENIZED");
        // MD_AUTHOR has no UNTOKENIZED sibling: exactly one value
        assertEquals(1, container.getMetadata().get("MD_AUTHOR").size(),
                "MD_AUTHOR must appear with exactly one value when no UNTOKENIZED variant exists");
    }

    /**
     * @verifies get partially translated values
     */
    @Test
    void getFirstValue_shouldGetPartiallyTranslatedValues() {
        SolrDocument main = new SolrDocument(Map.of(
                "PI", "1234",
                "NORM_ALTNAME_LANG_EN", List.of("Saltbourg")));

        SolrDocument doc = new SolrDocument(Map.of(
                "MD_LOCATION", List.of("Salzburg Stadt"),
                "NORM_OFFICIALNAME", List.of("Salzburg"),
                "NORM_ALTNAME", List.of("Stadt Salzburg"),
                "NORM_ALTNAME_LANG_FR", List.of("Salzbourg"),
                "NORM_ALTNAME_LANG_IT", List.of("Salisburgo"),
                "NORM_ALTNAME_LANG_PL", List.of("Salzburgu")));

        MetadataContainer record = MetadataContainer.createMetadataEntity(main, List.of(doc), e -> true, e -> true);
        assertEquals("Salzbourg", record.getFirstValue("NORM_ALTNAME", Locale.FRANCE));
        assertEquals("Stadt Salzburg", record.getFirstValue("NORM_ALTNAME", Locale.ENGLISH));

        IMetadataValue md = record.getFirst("NORM_ALTNAME");
    }

}
