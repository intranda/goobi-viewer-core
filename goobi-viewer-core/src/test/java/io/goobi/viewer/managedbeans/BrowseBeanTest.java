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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class BrowseBeanTest extends AbstractTest {

    /**
     * @see BrowseBean#getBrowsingMenuItems(String)
     * @verifies skip items for language-specific fields if no language was given
     */
    @Test
    public void getBrowsingMenuItems_shouldSkipItemsForLanguagespecificFieldsIfNoLanguageWasGiven() throws Exception {
        BrowseBean bb = new BrowseBean();
        List<String> result = bb.getBrowsingMenuItems(null);
        assertEquals(2, result.size());
        assertEquals("MD_AUTHOR_UNTOKENIZED", result.get(0));
        assertEquals("MD_SHELFMARK", result.get(1));
    }

    /**
     * @see BrowseBean#getBrowsingMenuItems(String)
     * @verifies skip items for language-specific fields if they don't match given language
     */
    @Test
    public void getBrowsingMenuItems_shouldSkipItemsForLanguagespecificFieldsIfTheyDontMatchGivenLanguage() throws Exception {
        BrowseBean bb = new BrowseBean();
        List<String> result = bb.getBrowsingMenuItems("en");
        assertEquals(3, result.size());
        assertEquals("MD_AUTHOR_UNTOKENIZED", result.get(0));
        assertEquals("MD_TITLE_LANG_EN_UNTOKENIZED", result.get(1));
        assertEquals("MD_SHELFMARK", result.get(2));
    }

    /**
     * @see BrowseBean#getCollectionHierarchy(String,String)
     * @verifies return hierarchy correctly
     */
    @Test
    public void getCollectionHierarchy_shouldReturnHierarchyCorrectly() throws Exception {
        BrowseBean bb = new BrowseBean();
        assertEquals("foo", bb.getCollectionHierarchy("x", "foo"));
        assertEquals("foo / foo.bar", bb.getCollectionHierarchy("x", "foo.bar"));
    }

    /**
     * @see BrowseBean#selectRedirectFilter()
     * @verifies return first available alphabetical filter if available
     */
    @Test
    public void selectRedirectFilter_shouldReturnFirstAvailableAlphabeticalFilterIfAvailable() throws Exception {
        BrowseBean bb = new BrowseBean();
        bb.setBrowsingMenuField("foo");
        bb.availableStringFilters.put("foo", new ArrayList<>(4));
        bb.getAvailableStringFilters().add("!");
        bb.getAvailableStringFilters().add("0-9");
        bb.getAvailableStringFilters().add("A");
        bb.getAvailableStringFilters().add("B");
        assertEquals("A", bb.selectRedirectFilter());
    }

    /**
     * @see BrowseBean#selectRedirectFilter()
     * @verifies return numerical filter if available
     */
    @Test
    public void selectRedirectFilter_shouldReturnNumericalFilterIfAvailable() throws Exception {
        BrowseBean bb = new BrowseBean();
        bb.setBrowsingMenuField("foo");
        bb.availableStringFilters.put("foo", new ArrayList<>(2));
        bb.getAvailableStringFilters().add("!");
        bb.getAvailableStringFilters().add("0-9");
       assertEquals("0-9", bb.selectRedirectFilter());
    }

    /**
     * @see BrowseBean#selectRedirectFilter()
     * @verifies return first filter if no other available
     */
    @Test
    public void selectRedirectFilter_shouldReturnFirstFilterIfNoOtherAvailable() throws Exception {
        BrowseBean bb = new BrowseBean();
        bb.setBrowsingMenuField("foo");
        bb.availableStringFilters.put("foo", new ArrayList<>(2));
        bb.getAvailableStringFilters().add("!");
        bb.getAvailableStringFilters().add("?");
       assertEquals("!", bb.selectRedirectFilter());
    }

    /**
     * @see BrowseBean#getBrowsingMenuFieldForLanguage(String)
     * @verifies return field for given language if placeholder found
     */
    @Test
    public void getBrowsingMenuFieldForLanguage_shouldReturnFieldForGivenLanguageIfPlaceholderFound() throws Exception {
        BrowseBean bb = new BrowseBean();
        bb.setBrowsingMenuField("MD_FOO_LANG_{}");
        assertEquals("MD_FOO_LANG_EN", bb.getBrowsingMenuFieldForLanguage("en"));
    }

    /**
     * @see BrowseBean#getBrowsingMenuFieldForLanguage(String)
     * @verifies return browsingMenuField if no language placeholder
     */
    @Test
    public void getBrowsingMenuFieldForLanguage_shouldReturnBrowsingMenuFieldIfNoLanguagePlaceholder() throws Exception {
        BrowseBean bb = new BrowseBean();
        bb.setBrowsingMenuField("MD_FOO");
        assertEquals("MD_FOO", bb.getBrowsingMenuFieldForLanguage("en"));
    }
}
