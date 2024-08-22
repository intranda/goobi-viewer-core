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
package io.goobi.viewer.model.citation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;

class CitationLinkTest extends AbstractSolrEnabledTest {

    /**
     * @see CitationLink#getUrl(ViewManager)
     * @verifies construct internal record url correctly
     */
    @Test
    void getUrl_shouldConstructInternalRecordUrlCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV, true);
        Assertions.assertNotNull(viewManager);
        Assertions.assertEquals(viewManager.getTopStructElementIddoc(), viewManager.getCurrentStructElementIddoc());

        CitationLink link = new CitationLink("internal", "record", "clipboard", "foo");
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/1/", link.getUrl(viewManager));
    }

    /**
     * @see CitationLink#getUrl(ViewManager)
     * @verifies construct internal docstruct url correctly
     */
    @Test
    void getUrl_shouldConstructInternalDocstructUrlCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV, true);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(10);
        Assertions.assertNotEquals(viewManager.getTopStructElementIddoc(), viewManager.getCurrentStructElementIddoc());

        CitationLink link = new CitationLink("internal", "docstruct", "clipboard", "foo").setField(SolrConstants.PI_TOPSTRUCT);
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/5/LOG_0003/", link.getUrl(viewManager));
    }

    /**
     * @see CitationLink#getUrl(ViewManager)
     * @verifies construct internal image url correctly
     */
    @Test
    void getUrl_shouldConstructInternalImageUrlCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV, true);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(2);

        CitationLink link = new CitationLink("internal", "image", "clipboard", "foo");
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/2/", link.getUrl(viewManager));
    }
}
