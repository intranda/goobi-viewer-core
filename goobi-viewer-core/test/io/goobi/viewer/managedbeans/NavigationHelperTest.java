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
package io.goobi.viewer.managedbeans;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.model.viewer.LabeledLink;

public class NavigationHelperTest extends AbstractDatabaseEnabledTest {

    /**
     * @see NavigationHelper#getActivePartnerId()
     * @verifies return value correctly
     */
    @Test
    public void getActivePartnerId_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE, NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE + "_value");
        Assert.assertEquals(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE + "_value", nh.getActivePartnerId());
    }

    /**
     * @see NavigationHelper#getCurrentPartnerPage()
     * @verifies return value correctly
     */
    @Test
    public void getCurrentPartnerPage_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_CURRENT_PARTNER_PAGE, NavigationHelper.KEY_CURRENT_PARTNER_PAGE + "_value");
        Assert.assertEquals(NavigationHelper.KEY_CURRENT_PARTNER_PAGE + "_value", nh.getCurrentPartnerPage());
    }

    /**
     * @see NavigationHelper#getCurrentView()
     * @verifies return value correctly
     */
    @Test
    public void getCurrentView_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_CURRENT_VIEW, NavigationHelper.KEY_CURRENT_VIEW + "_value");
        Assert.assertEquals(NavigationHelper.KEY_CURRENT_VIEW + "_value", nh.getCurrentView());
    }

    /**
     * @see NavigationHelper#getMenuPage()
     * @verifies return value correctly
     */
    @Test
    public void getMenuPage_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_MENU_PAGE, NavigationHelper.KEY_MENU_PAGE + "_value");
        Assert.assertEquals(NavigationHelper.KEY_MENU_PAGE + "_value", nh.getMenuPage());
    }

    /**
     * @see NavigationHelper#getPreferredView()
     * @verifies return value correctly
     */
    @Test
    public void getPreferredView_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_PREFERRED_VIEW, NavigationHelper.KEY_PREFERRED_VIEW + "_value");
        Assert.assertEquals(NavigationHelper.KEY_PREFERRED_VIEW + "_value", nh.getPreferredView());
    }

    /**
     * @see NavigationHelper#getSelectedNewsArticle()
     * @verifies return value correctly
     */
    @Test
    public void getSelectedNewsArticle_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE, NavigationHelper.KEY_SELECTED_NEWS_ARTICLE + "_value");
        Assert.assertEquals(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE + "_value", nh.getSelectedNewsArticle());
    }

    /**
     * @see NavigationHelper#getStatusMapValue(String)
     * @verifies return value correctly
     */
    @Test
    public void getStatusMapValue_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put("new_key", "new_value");
        Assert.assertEquals("new_value", nh.getStatusMapValue("new_key"));
    }

    /**
     * @see NavigationHelper#resetActivePartnerId()
     * @verifies reset value correctly
     */
    @Test
    public void resetActivePartnerId_shouldResetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE, NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE + "_value");
        nh.resetActivePartnerId();
        Assert.assertEquals("", nh.statusMap.get(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE));

    }

    /**
     * @see NavigationHelper#setActivePartnerId(String)
     * @verifies reset current partner page
     */
    @Test
    public void setActivePartnerId_shouldResetCurrentPartnerPage() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_CURRENT_PARTNER_PAGE, NavigationHelper.KEY_CURRENT_PARTNER_PAGE + "_value");
        nh.setActivePartnerId(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE + "_value");
        Assert.assertEquals("", nh.statusMap.get(NavigationHelper.KEY_CURRENT_PARTNER_PAGE));
    }

    /**
     * @see NavigationHelper#setActivePartnerId(String)
     * @verifies set value correctly
     */
    @Test
    public void setActivePartnerId_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setActivePartnerId(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE + "_value");
        Assert.assertEquals(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE + "_value",
                nh.statusMap.get(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE));
    }

    /**
     * @see NavigationHelper#setCurrentPartnerPage(String)
     * @verifies set value correctly
     */
    @Test
    public void setCurrentPartnerPage_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setCurrentPartnerPage(NavigationHelper.KEY_CURRENT_PARTNER_PAGE + "_value");
        Assert.assertEquals(NavigationHelper.KEY_CURRENT_PARTNER_PAGE + "_value", nh.statusMap.get(NavigationHelper.KEY_CURRENT_PARTNER_PAGE));
    }

    /**
     * @see NavigationHelper#setCurrentView(String)
     * @verifies set value correctly
     */
    @Test
    public void setCurrentView_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setCurrentView(NavigationHelper.KEY_CURRENT_VIEW + "_value");
        Assert.assertEquals(NavigationHelper.KEY_CURRENT_VIEW + "_value", nh.statusMap.get(NavigationHelper.KEY_CURRENT_VIEW));
    }

    /**
     * @see NavigationHelper#setMenuPage(String)
     * @verifies set value correctly
     */
    @Test
    public void setMenuPage_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setMenuPage(NavigationHelper.KEY_MENU_PAGE + "_value");
        Assert.assertEquals(NavigationHelper.KEY_MENU_PAGE + "_value", nh.statusMap.get(NavigationHelper.KEY_MENU_PAGE));
    }

    /**
     * @see NavigationHelper#setPreferredView(String)
     * @verifies set value correctly
     */
    @Test
    public void setPreferredView_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setPreferredView(NavigationHelper.KEY_PREFERRED_VIEW + "_value");
        Assert.assertEquals(NavigationHelper.KEY_PREFERRED_VIEW + "_value", nh.statusMap.get(NavigationHelper.KEY_PREFERRED_VIEW));
    }

    /**
     * @see NavigationHelper#setSelectedNewsArticle(String)
     * @verifies set value correctly
     */
    @Test
    public void setSelectedNewsArticle_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setSelectedNewsArticle(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE + "_value");
        Assert.assertEquals(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE + "_value", nh.statusMap.get(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE));
    }

    /**
     * @see NavigationHelper#setStatusMapValue(String,String)
     * @verifies set value correctly
     */
    @Test
    public void setStatusMapValue_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setStatusMapValue("new_key", "new_value");
        Assert.assertEquals("new_value", nh.statusMap.get("new_key"));
    }

    /**
     * @see NavigationHelper#setSubThemeDiscriminatorValue(String)
     * @verifies set value correctly
     */
    @Test
    public void setSubThemeDiscriminatorValue_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setSubThemeDiscriminatorValue("dValue");
        Assert.assertEquals("dValue", nh.getStatusMapValue(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE));
    }

    /**
     * @see NavigationHelper#addCollectionHierarchyToBreadcrumb(String,int)
     * @verifies create breadcrumbs correctly
     */
    @Test
    public void addCollectionHierarchyToBreadcrumb_shouldCreateBreadcrumbsCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        Assert.assertEquals(0, nh.getBreadcrumbs().size());
        nh.addCollectionHierarchyToBreadcrumb("a.b.c.d", "FOO", ".");

        Assert.assertEquals(6, nh.getBreadcrumbs().size());

        Assert.assertEquals("browseCollection", nh.getBreadcrumbs().get(1).getName());
        Assert.assertEquals("a", nh.getBreadcrumbs().get(2).getName());
        Assert.assertEquals("a.b", nh.getBreadcrumbs().get(3).getName());
        Assert.assertEquals("a.b.c", nh.getBreadcrumbs().get(4).getName());
        Assert.assertEquals("a.b.c.d", nh.getBreadcrumbs().get(5).getName());

        Assert.assertTrue(nh.getBreadcrumbs().get(2).getUrl().contains("/FOO:a/"));
        Assert.assertTrue(nh.getBreadcrumbs().get(3).getUrl().contains("/FOO:a.b/"));
        Assert.assertTrue(nh.getBreadcrumbs().get(4).getUrl().contains("/FOO:a.b.c/"));
        Assert.assertTrue(nh.getBreadcrumbs().get(5).getUrl().contains("/FOO:a.b.c.d/"));
    }

    /**
     * @see NavigationHelper#updateBreadcrumbs(LabeledLink)
     * @verifies always remove bookmarks coming after the proposed bookmark
     */
    @Test
    public void updateBreadcrumbs_shouldAlwaysRemoveBookmarksComingAfterTheProposedBookmark() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        Assert.assertEquals(0, nh.getBreadcrumbs().size());
        nh.updateBreadcrumbs(new LabeledLink("one", "https://example.com/one", 1));
        nh.updateBreadcrumbs(new LabeledLink("two", "https://example.com/one/two", 2));
        nh.updateBreadcrumbs(new LabeledLink("three", "https://example.com/one/two/three", 3));
        Assert.assertEquals(4, nh.getBreadcrumbs().size());
        
        // Insert new breadcrumb at 2
        nh.updateBreadcrumbs(new LabeledLink("two-too", "https://example.com/one/two-too", 2));
        Assert.assertEquals(3, nh.getBreadcrumbs().size());
        Assert.assertEquals("two-too", nh.getBreadcrumbs().get(2).getName());
        
        // Insert duplicate at 1
        nh.updateBreadcrumbs(new LabeledLink("one", "https://example.com/one", 1));
        Assert.assertEquals(2, nh.getBreadcrumbs().size());
        Assert.assertEquals("one", nh.getBreadcrumbs().get(1).getName());
    }
}
