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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FuzzySearchTermTest {

    /**
     * @verifies handle long s character substitution correctly
     * @see FuzzySearchTerm#matches(String)
     */
    @Test
    void matches_shouldHandleLongSCharacterSubstitutionCorrectly() {
        String search = "wissenschaftlichen";
        String text = "wisſenſchaftlichen";
        FuzzySearchTerm fuzzy = new FuzzySearchTerm(search);
        assertTrue(fuzzy.matches(text));
    }

    /**
     * Verify that matches handles text containing special characters gracefully.
     * The cleanup method strips diacritical marks and replaces character variants,
     * so a term with special characters should still match the normalized equivalent.
     *
     * @see FuzzySearchTerm#matches(String)
     * @verifies special characters
     */
    @Test
    void matches_shouldSpecialCharacters() {
        // Exact match after diacritical mark normalization: "über" -> "uber"
        FuzzySearchTerm fuzzy = new FuzzySearchTerm("über");
        assertTrue(fuzzy.matches("über"));

        // Long s character variant replacement: "ſ" -> "s"
        FuzzySearchTerm fuzzy2 = new FuzzySearchTerm("strasse");
        assertTrue(fuzzy2.matches("ſtraſſe"));
    }

}
