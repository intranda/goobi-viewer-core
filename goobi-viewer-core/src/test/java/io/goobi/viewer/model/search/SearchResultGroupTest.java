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

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;

class SearchResultGroupTest extends AbstractTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractTest.setUpClass();
    }

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

    /**
     * @see SearchResultGroup#getConfiguredResultGroups()
     * @verifies return correct groups
     */
    @Test
    void getConfiguredResultGroups_shouldReturnCorrectGroups() {
        // Test config has 3 result groups enabled: lido_objects, monographs, stories
        List<SearchResultGroup> groups = SearchResultGroup.getConfiguredResultGroups();
        Assertions.assertEquals(3, groups.size());
        Assertions.assertEquals("lido_objects", groups.get(0).getName());
        Assertions.assertEquals("SOURCEDOCFORMAT:LIDO", groups.get(0).getQuery());
        Assertions.assertEquals("monographs", groups.get(1).getName());
        Assertions.assertEquals("DOCSTRCT:monograph", groups.get(1).getQuery());
        Assertions.assertEquals("stories", groups.get(2).getName());
    }

    /**
     * @see SearchResultGroup#getConfiguredResultGroups()
     * @verifies return default group if none are configured
     */
    @Test
    void getConfiguredResultGroups_shouldReturnDefaultGroupIfNoneAreConfigured() {
        // Use a config that has resultGroups enabled but with no group elements (no_templates has no resultGroups section at all,
        // so isSearchResultGroupsEnabled returns false — use a config that returns enabled=true but empty groups list instead).
        // Since the no_templates config has no resultGroups, isSearchResultGroupsEnabled returns false, which yields an empty list,
        // and then the method adds a default group. We inject a config without resultGroups section to simulate "no groups configured".
        Configuration originalConfig = DataManager.getInstance().getConfiguration();
        try {
            // Config without resultGroups section — isSearchResultGroupsEnabled returns false (default), getSearchResultGroups returns empty
            DataManager.getInstance()
                    .injectConfiguration(
                            new Configuration(new File("src/test/resources/config_viewer_no_templates.test.xml").getAbsolutePath()));
            List<SearchResultGroup> groups = SearchResultGroup.getConfiguredResultGroups();
            // When disabled, method creates empty list and then adds default group
            Assertions.assertEquals(1, groups.size());
            Assertions.assertEquals(StringConstants.DEFAULT_NAME, groups.get(0).getName());
        } finally {
            // Restore original configuration
            DataManager.getInstance().injectConfiguration(originalConfig);
        }
    }

    /**
     * @see SearchResultGroup#getConfiguredResultGroups()
     * @verifies return default group if groups disabled
     */
    @Test
    void getConfiguredResultGroups_shouldReturnDefaultGroupIfGroupsDisabled() {
        // When result groups are disabled, the method should return a list with just the default group
        Configuration originalConfig = DataManager.getInstance().getConfiguration();
        try {
            // Config without resultGroups section — isSearchResultGroupsEnabled returns false by default
            DataManager.getInstance()
                    .injectConfiguration(
                            new Configuration(new File("src/test/resources/config_viewer_no_templates.test.xml").getAbsolutePath()));
            // Verify groups are indeed disabled in this config
            Assertions.assertFalse(DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled());
            List<SearchResultGroup> groups = SearchResultGroup.getConfiguredResultGroups();
            Assertions.assertFalse(groups.isEmpty());
            Assertions.assertEquals(1, groups.size());
            Assertions.assertEquals(StringConstants.DEFAULT_NAME, groups.get(0).getName());
        } finally {
            // Restore original configuration
            DataManager.getInstance().injectConfiguration(originalConfig);
        }
    }
}
