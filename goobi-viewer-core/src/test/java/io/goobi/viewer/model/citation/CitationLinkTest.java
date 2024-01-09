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

public class CitationLinkTest extends AbstractSolrEnabledTest {

    /**
     * @see CitationLink#getUrl(ViewManager)
     * @verifies construct internal record url correctly
     */
    @Test
    public void getUrl_shouldConstructInternalRecordUrlCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        Assertions.assertTrue(viewManager.getTopStructElementIddoc() == viewManager.getCurrentStructElementIddoc());

        CitationLink link = new CitationLink("internal", "record", "foo");
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/1/", link.getUrl(viewManager));
    }

    /**
     * @see CitationLink#getUrl(ViewManager)
     * @verifies construct internal docstruct url correctly
     */
    @Test
    public void getUrl_shouldConstructInternalDocstructUrlCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(10);
        Assertions.assertFalse(viewManager.getTopStructElementIddoc() == viewManager.getCurrentStructElementIddoc());

        CitationLink link = new CitationLink("internal", "docstruct", "foo").setField(SolrConstants.PI_TOPSTRUCT);
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/5/LOG_0003/", link.getUrl(viewManager));
    }

    /**
     * @see CitationLink#getUrl(ViewManager)
     * @verifies construct internal image url correctly
     */
    @Test
    public void getUrl_shouldConstructInternalImageUrlCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(2);

        CitationLink link = new CitationLink("internal", "image", "foo");
        Assertions.assertEquals("/object/" + PI_KLEIUNIV + "/2/", link.getUrl(viewManager));
    }

    /**
     * @see CitationLink#getUrl(ViewManager)
     * @verifies construct external url correctly
     */
    @Test
    public void getUrl_shouldConstructExternalUrlCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(2);

        CitationLink link = new CitationLink("url", "image", "foo").setField(SolrConstants.PI_TOPSTRUCT)
                .setPattern("https://viewer.goobi.io/resolver?id={value}&page={page}");
        Assertions.assertEquals("https://viewer.goobi.io/resolver?id=" + PI_KLEIUNIV + "&page=2", link.getUrl(viewManager));
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies return correct value for record type
     */
    @Test
    public void getValue_shouldReturnCorrectValueForRecordType() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);

        CitationLink link = new CitationLink("url", "record", "foo").setField(SolrConstants.PI);
        Assertions.assertEquals(PI_KLEIUNIV, link.getValue(viewManager));
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies return correct value for docstruct type
     */
    @Test
    public void getValue_shouldReturnCorrectValueForDocstructType() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);

        CitationLink link = new CitationLink("url", "docstruct", "foo").setField(SolrConstants.PI);
        Assertions.assertEquals(PI_KLEIUNIV, link.getValue(viewManager));
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies return correct value for image type
     */
    @Test
    public void getValue_shouldReturnCorrectValueForImageType() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(10);

        CitationLink link = new CitationLink("url", "image", "foo").setField(SolrConstants.ORDER);
        Assertions.assertEquals("10", link.getValue(viewManager));
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies fall back to topstruct value correctly
     */
    @Test
    public void getValue_shouldFallBackToTopstructValueCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(10);

        CitationLink link = new CitationLink("url", "image", "foo").setField(SolrConstants.PI).setTopstructValueFallback(true);
        Assertions.assertEquals(PI_KLEIUNIV, link.getValue(viewManager));
    }
}
