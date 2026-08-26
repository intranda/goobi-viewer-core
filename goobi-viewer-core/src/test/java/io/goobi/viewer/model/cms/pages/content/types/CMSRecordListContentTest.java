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
package io.goobi.viewer.model.cms.pages.content.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;

class CMSRecordListContentTest extends AbstractTest {

    /**
     * @see CMSRecordListContent#resolveSortString(String,String)
     * @verifies return explicitly configured sort field
     */
    @Test
    void resolveSortString_shouldReturnExplicitlyConfiguredSortField() {
        CMSRecordListContent content = new CMSRecordListContent();
        content.setSortField("SORT_YEARPUBLISH");
        // explicit component sort wins over any current/URL sort
        assertEquals("SORT_YEARPUBLISH", content.resolveSortString("random_5", "de"));

        // language placeholder is resolved
        content.setSortField("SORT_TITLE_LANG_{}");
        assertEquals("SORT_TITLE_LANG_DE", content.resolveSortString("-", "de"));
    }

    /**
     * @see CMSRecordListContent#resolveSortString(String,String)
     * @verifies return current sort string if sort field blank
     */
    @Test
    void resolveSortString_shouldReturnCurrentSortStringIfSortFieldBlank() {
        CMSRecordListContent content = new CMSRecordListContent();
        content.setSortField("");
        assertEquals("!SORT_TITLE", content.resolveSortString("!SORT_TITLE", "de"));
    }

    /**
     * @see CMSRecordListContent#resolveSortString(String,String)
     * @verifies return configured default sort field if sort field and current sort blank
     */
    @Test
    void resolveSortString_shouldReturnConfiguredDefaultSortFieldIfSortFieldAndCurrentSortBlank() {
        Configuration configSpy = Mockito.spy(new Configuration(AbstractTest.TEST_CONFIG_PATH));
        Mockito.doReturn("RANDOM").when(configSpy).getDefaultSortField(Mockito.anyString());
        DataManager.getInstance().injectConfiguration(configSpy);

        CMSRecordListContent content = new CMSRecordListContent();
        content.setSortField("");
        assertEquals("RANDOM", content.resolveSortString("-", "de"));
        assertEquals("RANDOM", content.resolveSortString(null, "de"));
        assertEquals("RANDOM", content.resolveSortString("", "de"));
    }
}
