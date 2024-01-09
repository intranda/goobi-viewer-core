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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;

public class MetadataContainerTest {

    @Test
    void test_translatedFieldsFromSingleDocument() {

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

    @Test
    void test_translatedFieldsFromMultipleDocuments() {

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

}
