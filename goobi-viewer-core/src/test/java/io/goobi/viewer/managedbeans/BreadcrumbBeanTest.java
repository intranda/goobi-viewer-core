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
package io.goobi.viewer.managedbeans;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.model.viewer.LabeledLink;

public class BreadcrumbBeanTest extends AbstractDatabaseEnabledTest {

    /**
     * @see BreadcrumbBean#addCollectionHierarchyToBreadcrumb(String,String,String)
     * @verifies create breadcrumbs correctly
     */
    @Test
    public void addCollectionHierarchyToBreadcrumb_shouldCreateBreadcrumbsCorrectly() throws Exception {
        BreadcrumbBean bb = new BreadcrumbBean();
        Assert.assertEquals(0, bb.getBreadcrumbs().size());
        bb.addCollectionHierarchyToBreadcrumb("a.b.c.d", "FOO", ".");

        Assert.assertEquals(6, bb.getBreadcrumbs().size());

        Assert.assertEquals("browseCollection", bb.getBreadcrumbs().get(1).getName());
        Assert.assertEquals("a", bb.getBreadcrumbs().get(2).getName());
        Assert.assertEquals("a.b", bb.getBreadcrumbs().get(3).getName());
        Assert.assertEquals("a.b.c", bb.getBreadcrumbs().get(4).getName());
        Assert.assertEquals("a.b.c.d", bb.getBreadcrumbs().get(5).getName());

        Assert.assertTrue(bb.getBreadcrumbs().get(2).getUrl().contains("/FOO%3Aa/"));
        Assert.assertTrue(bb.getBreadcrumbs().get(3).getUrl().contains("/FOO%3Aa.b/"));
        Assert.assertTrue(bb.getBreadcrumbs().get(4).getUrl().contains("/FOO%3Aa.b.c/"));
        Assert.assertTrue(bb.getBreadcrumbs().get(5).getUrl().contains("/FOO%3Aa.b.c.d/"));
    }

    /**
     * @see BreadcrumbBean#updateBreadcrumbs(LabeledLink)
     * @verifies always remove breadcrumbs coming after the proposed breadcrumb
     */
    @Test
    public void updateBreadcrumbs_shouldAlwaysRemoveBreadcrumbsComingAfterTheProposedBreadcrumb() throws Exception {
        BreadcrumbBean bb = new BreadcrumbBean();
        Assert.assertEquals(0, bb.getBreadcrumbs().size());
        bb.updateBreadcrumbs(new LabeledLink("one", "https://example.com/one", 1));
        bb.updateBreadcrumbs(new LabeledLink("two", "https://example.com/one/two", 2));
        bb.updateBreadcrumbs(new LabeledLink("three", "https://example.com/one/two/three", 3));
        Assert.assertEquals(4, bb.getBreadcrumbs().size());

        // Insert new breadcrumb at 2
        bb.updateBreadcrumbs(new LabeledLink("two-too", "https://example.com/one/two-too", 2));
        Assert.assertEquals(3, bb.getBreadcrumbs().size());
        Assert.assertEquals("two-too", bb.getBreadcrumbs().get(2).getName());

        // Insert duplicate at 1
        bb.updateBreadcrumbs(new LabeledLink("one", "https://example.com/one", 1));
        Assert.assertEquals(2, bb.getBreadcrumbs().size());
        Assert.assertEquals("one", bb.getBreadcrumbs().get(1).getName());
    }
}
