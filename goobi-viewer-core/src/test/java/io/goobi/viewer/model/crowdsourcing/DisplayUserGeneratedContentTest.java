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
import io.goobi.viewer.solr.SolrConstants;

class DisplayUserGeneratedContentTest extends AbstractSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractSolrEnabledTest.setUpClass();
    }

    /**
     * @see DisplayUserGeneratedContent#buildFromSolrDoc(SolrDocument)
     * @verifies construct content correctly
     */
    @Test
    void buildFromSolrDoc_shouldConstructContentCorrectly() throws Exception {
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
        // Assertions.assertEquals(Long.valueOf(123), ugc.getId());
        Assertions.assertEquals(ContentType.PERSON, ugc.getType());
        Assertions.assertEquals(coords, ugc.getAreaString());
        Assertions.assertEquals(coords, ugc.getDisplayCoordinates());
        Assertions.assertEquals("Doe, John", ugc.getLabel());
        Assertions.assertEquals("restricted", ugc.getAccessCondition());
    }
}
