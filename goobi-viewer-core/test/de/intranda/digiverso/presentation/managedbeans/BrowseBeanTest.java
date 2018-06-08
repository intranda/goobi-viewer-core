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

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;

public class BrowseBeanTest {

    @Before
    public void setUp() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see BrowseBean#getBrowsingMenuItems(String)
     * @verifies skip items for language-specific fields if no language was given
     */
    @Test
    public void getBrowsingMenuItems_shouldSkipItemsForLanguagespecificFieldsIfNoLanguageWasGiven() throws Exception {
        BrowseBean bb = new BrowseBean();
        List<String> result = bb.getBrowsingMenuItems(null);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("MD_AUTHOR_UNTOKENIZED", result.get(0));
        Assert.assertEquals("MD_SHELFMARK", result.get(1));
    }

    /**
     * @see BrowseBean#getBrowsingMenuItems(String)
     * @verifies skip items for language-specific fields if they don't match given language
     */
    @Test
    public void getBrowsingMenuItems_shouldSkipItemsForLanguagespecificFieldsIfTheyDontMatchGivenLanguage() throws Exception {
        BrowseBean bb = new BrowseBean();
        List<String> result = bb.getBrowsingMenuItems("en");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("MD_AUTHOR_UNTOKENIZED", result.get(0));
        Assert.assertEquals("MD_TITLE_LANG_EN_UNTOKENIZED", result.get(1));
        Assert.assertEquals("MD_SHELFMARK", result.get(2));
    }
}