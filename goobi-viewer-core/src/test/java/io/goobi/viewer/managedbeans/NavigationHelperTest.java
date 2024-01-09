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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.LabeledLink;
import io.goobi.viewer.model.viewer.PageType;

public class NavigationHelperTest extends AbstractDatabaseEnabledTest {

    /**
     * @see NavigationHelper#getCurrentView()
     * @verifies return value correctly
     */
    @Test
    void getCurrentView_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_CURRENT_VIEW, NavigationHelper.KEY_CURRENT_VIEW + "_value");
        Assertions.assertEquals(NavigationHelper.KEY_CURRENT_VIEW + "_value", nh.getCurrentView());
    }

    /**
     * @see NavigationHelper#getMenuPage()
     * @verifies return value correctly
     */
    @Test
    void getMenuPage_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_MENU_PAGE, NavigationHelper.KEY_MENU_PAGE + "_value");
        Assertions.assertEquals(NavigationHelper.KEY_MENU_PAGE + "_value", nh.getMenuPage());
    }

    /**
     * @see NavigationHelper#getPreferredView()
     * @verifies return value correctly
     */
    @Test
    void getPreferredView_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_PREFERRED_VIEW, NavigationHelper.KEY_PREFERRED_VIEW + "_value");
        Assertions.assertEquals(NavigationHelper.KEY_PREFERRED_VIEW + "_value", nh.getPreferredView());
    }

    /**
     * @see NavigationHelper#getSelectedNewsArticle()
     * @verifies return value correctly
     */
    @Test
    void getSelectedNewsArticle_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE, NavigationHelper.KEY_SELECTED_NEWS_ARTICLE + "_value");
        Assertions.assertEquals(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE + "_value", nh.getSelectedNewsArticle());
    }

    /**
     * @see NavigationHelper#getStatusMapValue(String)
     * @verifies return value correctly
     */
    @Test
    void getStatusMapValue_shouldReturnValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.statusMap.put("new_key", "new_value");
        Assertions.assertEquals("new_value", nh.getStatusMapValue("new_key"));
    }

    /**
     * @see NavigationHelper#setCurrentView(String)
     * @verifies set value correctly
     */
    @Test
    void setCurrentView_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setCurrentView(NavigationHelper.KEY_CURRENT_VIEW + "_value");
        Assertions.assertEquals(NavigationHelper.KEY_CURRENT_VIEW + "_value", nh.statusMap.get(NavigationHelper.KEY_CURRENT_VIEW));
    }

    /**
     * @see NavigationHelper#setMenuPage(String)
     * @verifies set value correctly
     */
    @Test
    void setMenuPage_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setMenuPage(NavigationHelper.KEY_MENU_PAGE + "_value");
        Assertions.assertEquals(NavigationHelper.KEY_MENU_PAGE + "_value", nh.statusMap.get(NavigationHelper.KEY_MENU_PAGE));
    }

    /**
     * @see NavigationHelper#setPreferredView(String)
     * @verifies set value correctly
     */
    @Test
    void setPreferredView_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setPreferredView(NavigationHelper.KEY_PREFERRED_VIEW + "_value");
        Assertions.assertEquals(NavigationHelper.KEY_PREFERRED_VIEW + "_value", nh.statusMap.get(NavigationHelper.KEY_PREFERRED_VIEW));
    }

    /**
     * @see NavigationHelper#setSelectedNewsArticle(String)
     * @verifies set value correctly
     */
    @Test
    void setSelectedNewsArticle_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setSelectedNewsArticle(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE + "_value");
        Assertions.assertEquals(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE + "_value", nh.statusMap.get(NavigationHelper.KEY_SELECTED_NEWS_ARTICLE));
    }

    /**
     * @see NavigationHelper#setStatusMapValue(String,String)
     * @verifies set value correctly
     */
    @Test
    void setStatusMapValue_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setStatusMapValue("new_key", "new_value");
        Assertions.assertEquals("new_value", nh.statusMap.get("new_key"));
    }

    /**
     * @see NavigationHelper#setSubThemeDiscriminatorValue(String)
     * @verifies set value correctly
     */
    @Test
    void setSubThemeDiscriminatorValue_shouldSetValueCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setSubThemeDiscriminatorValue("dValue");
        Assertions.assertEquals("dValue", nh.getStatusMapValue(NavigationHelper.KEY_SUBTHEME_DISCRIMINATOR_VALUE));
    }

    @Test
    void test_createAdminBreadcrumbs() {
        NavigationHelper nh = new NavigationHelper();
        PageType pageType = PageType.getByName("adminTranslationsEdit");
        List<List<String>> labels = List.of(List.of("adminTranslationsEdit", "Sammlungsnamen"), List.of("foobar"));
        List<LabeledLink> breadcrumbs = nh.createAdminBreadcrumbs(pageType, labels);

        assertEquals(3, breadcrumbs.size());

        LabeledLink first = breadcrumbs.get(0);
        assertEquals(ViewerResourceBundle.getTranslation(PageType.adminDashboard.getLabel(), nh.getLocale()), first.getName());
        assertEquals("/" + PageType.adminDashboard.getName(), first.getUrl());

        LabeledLink second = breadcrumbs.get(1);
        assertEquals("/" + PageType.adminTranslations.getName(), second.getUrl());
        assertEquals("foobar", second.getName());

        LabeledLink third = breadcrumbs.get(2);
        String label = ViewerResourceBundle.getTranslationWithParameters("adminTranslationsEdit", nh.getLocale(), true, "Sammlungsnamen");
        assertEquals(label, third.getName());
    }
}
