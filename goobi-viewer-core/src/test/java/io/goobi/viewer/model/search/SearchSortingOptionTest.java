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
package io.goobi.viewer.model.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.solr.SolrConstants;

class SearchSortingOptionTest extends AbstractTest {

    /**
     * @see SearchSortingOption#SearchSortingOption(String)
     * @verifies set ascending field correctly
     */
    @Test
    void SearchSortingOption_shouldSetAscendingFieldCorrectly() throws Exception {
        SearchSortingOption option = new SearchSortingOption("SORT_TITLE");
        Assertions.assertTrue(option.isAscending());
        Assertions.assertFalse(option.isDescending());
        Assertions.assertEquals("SORT_TITLE", option.getField());
    }

    /**
     * @see SearchSortingOption#SearchSortingOption(String)
     * @verifies set descending field correctly
     */
    @Test
    void SearchSortingOption_shouldSetDescendingFieldCorrectly() throws Exception {
        SearchSortingOption option = new SearchSortingOption("!SORT_TITLE");
        Assertions.assertFalse(option.isAscending());
        Assertions.assertTrue(option.isDescending());
        Assertions.assertEquals("SORT_TITLE", option.getField());
    }

    /**
     * @see SearchSortingOption#getLabel()
     * @verifies return translation of RANDOM if field RANDOM
     */
    @Test
    void getLabel_shouldReturnTranslationOfRANDOMIfFieldRANDOM() throws Exception {
        SearchSortingOption option = new SearchSortingOption(SolrConstants.SORT_RANDOM);
        Assertions.assertEquals(ViewerResourceBundle.getTranslation(SearchSortingOption.RANDOM_SORT_FIELD_LABEL, null), option.getLabel());
    }

    /**
     * @see SearchSortingOption#getLabel()
     * @verifies return translation of RANDOM if field random seed
     */
    @Test
    void getLabel_shouldReturnTranslationOfRANDOMIfFieldRandomSeed() throws Exception {
        SearchSortingOption option = new SearchSortingOption("random_12345");
        Assertions.assertEquals(ViewerResourceBundle.getTranslation(SearchSortingOption.RANDOM_SORT_FIELD_LABEL, null), option.getLabel());
    }

    /**
     * @see SearchSortingOption#getLabel()
     * @verifies return translation of DEFAULT_SORT_FIELD_LABEL if field RELEVANCE
     */
    @Test
    void getLabel_shouldReturnTranslationOfDEFAULT_SORT_FIELD_LABELIfFieldRELEVANCE() throws Exception {
        SearchSortingOption option = new SearchSortingOption(SolrConstants.SORT_RELEVANCE);
        Assertions.assertEquals(ViewerResourceBundle.getTranslation(SearchSortingOption.RELEVANCE_SORT_FIELD_LABEL, null), option.getLabel());
    }

    /**
     * @see SearchSortingOption#getSortString()
     * @verifies add exclamation mark prefix if descending
     */
    @Test
    void getSortString_shouldAddExclamationMarkPrefixIfDescending() throws Exception {
        SearchSortingOption option = new SearchSortingOption("!SORT_TITLE");
        Assertions.assertEquals("!SORT_TITLE", option.getSortString());
    }

    /**
     * @see SearchSortingOption#getSortString()
     * @verifies not add exclamation mark prefix is ascending
     */
    @Test
    void getSortString_shouldNotAddExclamationMarkPrefixIsAscending() throws Exception {
        {
            SearchSortingOption option = new SearchSortingOption("SORT_TITLE");
            Assertions.assertEquals("SORT_TITLE", option.getSortString());
        }
        {
            SearchSortingOption option = new SearchSortingOption("random_12345");
            Assertions.assertEquals("random_12345", option.getSortString());
        }
    }

    /**
     * @see SearchSortingOption#getSortString()
     * @verifies return empty string if field blank
     */
    @Test
    void getSortString_shouldReturnEmptyStringIfFieldBlank() throws Exception {
        SearchSortingOption option = new SearchSortingOption("");
        Assertions.assertEquals("", option.getSortString());
    }
}
