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
package de.intranda.digiverso.presentation.managedbeans;

import org.junit.Assert;
import org.junit.Test;

public class NavigationHelperTest {

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
        Assert.assertEquals(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE + "_value", nh.statusMap.get(
                NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE));
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
}