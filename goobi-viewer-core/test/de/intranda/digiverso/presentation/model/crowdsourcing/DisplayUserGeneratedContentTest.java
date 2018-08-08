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
package de.intranda.digiverso.presentation.model.crowdsourcing;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.crowdsourcing.DisplayUserGeneratedContent.ContentType;

public class DisplayUserGeneratedContentTest {
    
    /**
     * @see DisplayUserGeneratedContent#buildFromSolrDoc(SolrDocument)
     * @verifies construct content correctly
     */
    @Test
    public void buildFromSolrDoc_shouldConstructContentCorrectly() throws Exception {
        String coords = "1468.0, 2459.0, 1938.0, 2569.0";

        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "123");
        doc.setField(SolrConstants.UGCTYPE, ContentType.PERSON.name());
        doc.setField(SolrConstants.UGCCOORDS, coords);
        doc.setField("MD_FIRSTNAME", "John");
        doc.setField("MD_LASTNAME", "Doe");

        DisplayUserGeneratedContent ugc = DisplayUserGeneratedContent.buildFromSolrDoc(doc);
        Assert.assertNotNull(ugc);
        Assert.assertEquals(Long.valueOf(123), ugc.getId());
        Assert.assertEquals(ContentType.PERSON, ugc.getType());
        Assert.assertEquals(coords, ugc.getAreaString());
        Assert.assertEquals(coords, ugc.getDisplayCoordinates());
        Assert.assertEquals("Doe, John", ugc.getLabel());
    }
}