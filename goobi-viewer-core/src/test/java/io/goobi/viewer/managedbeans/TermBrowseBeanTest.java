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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;

class TermBrowseBeanTest extends AbstractTest {

    /**
     * @see TermBrowseBean#getBrowsingMenuItems(String)
     * @verifies skip items that have skipInWidget true
     */
    @Test
    void getBrowsingMenuItems_shouldSkipItemsThatHaveSkipInWidgetTrue() {
        TermBrowseBean bb = new TermBrowseBean();
        List<String> result = bb.getBrowsingMenuItems(null);
        assertEquals(2, result.size());
        assertEquals("MD_AUTHOR_UNTOKENIZED", result.get(0));
        assertEquals("MD_SHELFMARK", result.get(1));
    }

    /**
     * @see TermBrowseBean#getBrowsingMenuItems(String)
     * @verifies skip items for languagespecific fields if no language was given
     */
    @Test
    void getBrowsingMenuItems_shouldSkipItemsForLanguagespecificFieldsIfNoLanguageWasGiven() {
        TermBrowseBean bb = new TermBrowseBean();
        List<String> result = bb.getBrowsingMenuItems(null);
        assertEquals(2, result.size());
        assertEquals("MD_AUTHOR_UNTOKENIZED", result.get(0));
        assertEquals("MD_SHELFMARK", result.get(1));
    }

    /**
     * @see TermBrowseBean#getBrowsingMenuItems(String)
     * @verifies skip items for languagespecific fields if they dont match given language
     */
    @Test
    void getBrowsingMenuItems_shouldSkipItemsForLanguagespecificFieldsIfTheyDontMatchGivenLanguage() {
        TermBrowseBean bb = new TermBrowseBean();
        List<String> result = bb.getBrowsingMenuItems("en");
        assertEquals(4, result.size());
        assertEquals("MD_AUTHOR_UNTOKENIZED", result.get(0));
        assertEquals("MD_TITLE_LANG_EN_UNTOKENIZED", result.get(2));
        assertEquals("MD_SHELFMARK", result.get(3));
    }

    /**
     * @see TermBrowseBean#getBrowsingMenuItems(String)
     * @verifies return languagespecific fields with placeholder
     */
    @Test
    void getBrowsingMenuItems_shouldReturnLanguagespecificFieldsWithPlaceholder() {
        TermBrowseBean bb = new TermBrowseBean();
        List<String> result = bb.getBrowsingMenuItems("en");
        assertEquals(4, result.size());
        assertEquals("MD_AUTHOR_UNTOKENIZED", result.get(0));
        assertEquals("MD_ARTIST_LANG_{}", result.get(1));
        assertEquals("MD_TITLE_LANG_EN_UNTOKENIZED", result.get(2));
        assertEquals("MD_SHELFMARK", result.get(3));
    }

    /**
     * @see TermBrowseBean#selectRedirectFilter()
     * @verifies return first available alphabetical filter if available
     */
    @Test
    void selectRedirectFilter_shouldReturnFirstAvailableAlphabeticalFilterIfAvailable() {
        TermBrowseBean bb = new TermBrowseBean();
        bb.setBrowsingMenuField("foo");
        bb.getAvailableStringFiltersMap().put("FOO", new ArrayList<>(4));
        bb.getAvailableStringFilters().add("!");
        bb.getAvailableStringFilters().add("0-9");
        bb.getAvailableStringFilters().add("A");
        bb.getAvailableStringFilters().add("B");
        assertEquals("A", bb.selectRedirectFilter());
    }

    /**
     * @see TermBrowseBean#selectRedirectFilter()
     * @verifies return numerical filter if available
     */
    @Test
    void selectRedirectFilter_shouldReturnNumericalFilterIfAvailable() {
        TermBrowseBean bb = new TermBrowseBean();
        bb.setBrowsingMenuField("foo");
        bb.getAvailableStringFiltersMap().put("FOO", new ArrayList<>(2));
        bb.getAvailableStringFilters().add("!");
        bb.getAvailableStringFilters().add("2");
        assertEquals("2", bb.selectRedirectFilter());
    }

    /**
     * @see TermBrowseBean#selectRedirectFilter()
     * @verifies return first filter if no other available
     */
    @Test
    void selectRedirectFilter_shouldReturnFirstFilterIfNoOtherAvailable() {
        TermBrowseBean bb = new TermBrowseBean();
        bb.setBrowsingMenuField("foo");
        bb.getAvailableStringFiltersMap().put("FOO", new ArrayList<>(2));
        bb.getAvailableStringFilters().add("!");
        bb.getAvailableStringFilters().add("?");
        assertEquals("!", bb.selectRedirectFilter());
    }

    /**
     * @see TermBrowseBean#getBrowsingMenuFieldForLanguage(String)
     * @verifies return field for given language if placeholder found
     */
    @Test
    void getBrowsingMenuFieldForLanguage_shouldReturnFieldForGivenLanguageIfPlaceholderFound() {
        TermBrowseBean bb = new TermBrowseBean();
        bb.setBrowsingMenuField("MD_FOO_LANG_{}");
        assertEquals("MD_FOO_LANG_EN", bb.getBrowsingMenuFieldForLanguage("en"));
    }

    /**
     * @see TermBrowseBean#getBrowsingMenuFieldForLanguage(String)
     * @verifies return browsingMenuField if no language placeholder
     */
    @Test
    void getBrowsingMenuFieldForLanguage_shouldReturnBrowsingMenuFieldIfNoLanguagePlaceholder() {
        TermBrowseBean bb = new TermBrowseBean();
        bb.setBrowsingMenuField("MD_FOO");
        assertEquals("MD_FOO", bb.getBrowsingMenuFieldForLanguage("en"));
    }

    /**
     * @see TermBrowseBean#setBrowsingMenuField(String)
     * @verifies normalize field name to uppercase
     */
    @Test
    void setBrowsingMenuField_shouldNormalizeFieldNameToUppercase() {
        TermBrowseBean bb = new TermBrowseBean();
        bb.setBrowsingMenuField("md_allpersons_untokenized");
        assertEquals("MD_ALLPERSONS_UNTOKENIZED", bb.getBrowsingMenuField());
    }

    /**
     * @see TermBrowseBean#generateFilterQuery(List)
     * @verifies return empty string if no filterQuery or result groups available
     */
    @Test
    void generateFilterQuery_shouldReturnEmptyStringIfNoFilterQueryOrResultGroupsAvailable() {
        TermBrowseBean bb = new TermBrowseBean();
        assertEquals("", bb.generateFilterQuery(Collections.emptyList()));
    }

    /**
     * @verifies generate filter query correctly
     */
    @Test
    void generateFilterQuery_shouldGenerateFilterQueryCorrectly() {
        TermBrowseBean bb = new TermBrowseBean();
        assertEquals("+(+( (SOURCEDOCFORMAT:LIDO) (DOCSTRCT:monograph) (+DOCSTRCT:\"cms_page\" +MD_CATEGORY:\"story\")))",
                bb.generateFilterQuery(DataManager.getInstance().getConfiguration().getSearchResultGroups()));

        bb.setFilterQuery("FOO:bar");
        assertEquals("+(+(FOO:bar))", bb.generateFilterQuery(Collections.emptyList()));

        assertEquals("+(+(FOO:bar) +( (SOURCEDOCFORMAT:LIDO) (DOCSTRCT:monograph) (+DOCSTRCT:\"cms_page\" +MD_CATEGORY:\"story\")))",
                bb.generateFilterQuery(DataManager.getInstance().getConfiguration().getSearchResultGroups()));
    }
}
