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

import io.goobi.viewer.controller.StringConstants;

class SearchResultGroupTest {

    /**
     * @see SearchResultGroup#isDisplayExpandUrl()
     * @verifies return false if default group
     */
    @Test
    void isDisplayExpandUrl_shouldReturnFalseIfDefaultGroup() {
        // The default group should never show the expand URL, even if hitsCount exceeds previewHitCount
        SearchResultGroup group = new SearchResultGroup(StringConstants.DEFAULT_NAME, "", 5);
        group.setHitsCount(100);
        Assertions.assertFalse(group.isDisplayExpandUrl());
    }

    /**
     * @see SearchResultGroup#isDisplayExpandUrl()
     * @verifies return false if hitsCount not higher than previewHitCount
     */
    @Test
    void isDisplayExpandUrl_shouldReturnFalseIfHitsCountNotHigherThanPreviewHitCount() {
        // Non-default group with hitsCount equal to previewHitCount should not show expand URL
        SearchResultGroup group = new SearchResultGroup("customGroup", "", 10);
        group.setHitsCount(10);
        Assertions.assertFalse(group.isDisplayExpandUrl());
    }

    /**
     * @see SearchResultGroup#isDisplayExpandUrl()
     * @verifies return true if hitsCount higher than previewHitCount
     */
    @Test
    void isDisplayExpandUrl_shouldReturnTrueIfHitsCountHigherThanPreviewHitCount() {
        // Non-default group with hitsCount exceeding previewHitCount should show expand URL
        SearchResultGroup group = new SearchResultGroup("customGroup", "", 5);
        group.setHitsCount(10);
        Assertions.assertTrue(group.isDisplayExpandUrl());
    }
}
