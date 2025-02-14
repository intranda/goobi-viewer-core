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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.solr.SolrConstants;

class ComplexMetadataContainerTest extends AbstractTest {

    List<SolrDocument> metadataDocs;

    @Override
    @BeforeEach
    public void setUp() {

        metadataDocs = List.of(
                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_RELATIONSHIP_PERSON",
                        SolrConstants.MD_VALUE, List.of("Person X"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_DATESTART", List.of("2022-04-23"),
                        "MD_DATEEND", List.of("2022-05-01"),
                        "MD_TYPE", List.of("is daughter of"),
                        "MD_IDENTIFIER", List.of("1010"))),
                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_RELATIONSHIP_PERSON",
                        SolrConstants.MD_VALUE, List.of("Person Y"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_TYPE", List.of("is wife of"),
                        "MD_IDENTIFIER", List.of("1020"))),
                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_RELATIONSHIP_EVENT",
                        SolrConstants.MD_VALUE, List.of("Event Y"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_DATESTART", List.of("2022-04-23"),
                        "MD_DATEEND", List.of("2022-05-01"),
                        "MD_TYPE", List.of("participated in"),
                        "MD_IDENTIFIER", List.of("2010"))),

                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_BIOGRAPHY_EDUCATION_LANG_FR",
                        "MD_REFID", List.of("6-8-9"),
                        SolrConstants.MD_VALUE, List.of("école primaire"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_DATESTART", List.of("2001-09-01"),
                        "MD_LOCATION", List.of("Bech-Kleinmacher"),
                        "WKT_COORDS", ("6.355 49.53083"),
                        "BOOL_WKT_COORDS", true)),
                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_BIOGRAPHY_EDUCATION_LANG_DE",
                        "MD_REFID", List.of("6-8-9"),
                        SolrConstants.MD_VALUE, List.of("Grundschule"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_DATESTART", List.of("2001-09-01"),
                        "MD_LOCATION", List.of("Bech-Kleinmacher"),
                        "WKT_COORDS", ("6.355 49.53083"),
                        "BOOL_WKT_COORDS", true)),
                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_BIOGRAPHY_EDUCATION_LANG_EN",
                        "MD_REFID", List.of("6-8-9"),
                        SolrConstants.MD_VALUE, List.of("primary school"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_DATESTART", List.of("2001-09-01"),
                        "MD_LOCATION", List.of("Bech-Kleinmacher"),
                        "WKT_COORDS", ("6.355 49.53083"),
                        "BOOL_WKT_COORDS", true)),

                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_BIOGRAPHY_EDUCATION_LANG_FR",
                        "MD_REFID", List.of("1-4-6"),
                        SolrConstants.MD_VALUE, List.of("lycée"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_DATESTART", List.of("2005-09-01"),
                        "MD_DATEEND", List.of("2014-09-01"),
                        "MD_LOCATION", List.of("Bech-Kleinmacher"),
                        "WKT_COORDS", ("6.355 49.53083"),
                        "BOOL_WKT_COORDS", true)),
                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_BIOGRAPHY_EDUCATION_LANG_DE",
                        "MD_REFID", List.of("1-4-6"),
                        SolrConstants.MD_VALUE, List.of("Weiterführende Schule"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_DATESTART", List.of("2005-09-01"),
                        "MD_DATEEND", List.of("2014-09-01"),
                        "MD_LOCATION", List.of("Bech-Kleinmacher"),
                        "WKT_COORDS", ("6.355 49.53083"),
                        "BOOL_WKT_COORDS", true)),
                new SolrDocument(Map.of(
                        SolrConstants.DOCTYPE, "METADATA",
                        SolrConstants.LABEL, "MD_BIOGRAPHY_EDUCATION_LANG_EN",
                        "MD_REFID", List.of("1-4-6"),
                        SolrConstants.MD_VALUE, List.of("highschool"),
                        SolrConstants.PI_TOPSTRUCT, "1234",
                        "MD_DATESTART", List.of("2005-09-01"),
                        "MD_DATEEND", List.of("2014-09-01"),
                        "MD_LOCATION", List.of("Bech-Kleinmacher"),
                        "WKT_COORDS", ("6.355 49.53083"),
                        "BOOL_WKT_COORDS", true)));

    }

    @Test
    void testCreateMetadataContainer() {
        ComplexMetadataContainer container = new ComplexMetadataContainer(metadataDocs);

        assertEquals(2, container.getMetadata("MD_BIOGRAPHY_EDUCATION").size());
        assertEquals("lycée", container.getMetadata("MD_BIOGRAPHY_EDUCATION").get(1).getFirstValue(Locale.FRANCE));
        assertEquals(2, container.getMetadata("MD_RELATIONSHIP_PERSON").size());
    }

}
