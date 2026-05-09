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
package io.goobi.viewer.modules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class IModuleCmsComponentTest {

    /**
     * @see IModule#getCmsComponentFolderUrl()
     * @verifies return empty optional by default
     */
    @Test
    void getCmsComponentFolderUrl_shouldReturnEmptyOptionalByDefault() {
        IModule module = new TestModule();
        Optional<URL> folder = module.getCmsComponentFolderUrl();
        assertTrue(folder.isEmpty());
    }

    /**
     * Minimal stub implementing only mandatory members; default methods are exercised by the test.
     *
     * <p>FQNs are concentrated here intentionally to keep the file's import block focused on the actual
     * symbols under test. These types are only used inside this throwaway stub, so importing each one
     * separately would add noise without aiding readability. The project-wide "no FQN inline" rule
     * documents this exception ("Ausnahme nur wenn der Klassen-Kurzname … kollidiert" — here the
     * exception is "kein Mehrwert vom Import"). Do not refactor without preserving the rationale.</p>
     */
    private static class TestModule implements IModule {
        @Override public String getId() { return "test"; }
        @Override public String getName() { return "test"; }
        @Override public String getVersion() { return "0.0.0"; }
        @Override public String getVersionJson() { return "{}"; }
        @Override public boolean isLoaded() { return true; }
        @Override public io.goobi.viewer.controller.AbstractConfiguration getConfiguration() { return null; }
        @Override public java.util.Map<String, String> getCmsMenuContributions() { return java.util.Collections.emptyMap(); }
        @Override public java.util.List<String> getSidebarContributions() { return java.util.Collections.emptyList(); }
        @Override public java.util.List<String> getAdminContributions() { return java.util.Collections.emptyList(); }
        @Override public java.util.List<String> getLoginNavigationContributions() { return java.util.Collections.emptyList(); }
        @Override public java.util.List<String> getWidgets(String type) { return java.util.Collections.emptyList(); }
        @Override public java.util.List<io.goobi.viewer.model.job.ITaskType> getTaskTypes() { return java.util.Collections.emptyList(); }
        @Override public boolean augmentResetRecord() { return true; }
        @Override public int deleteUserContributions(io.goobi.viewer.model.security.user.User user) { return 0; }
        @Override public int moveUserContributions(io.goobi.viewer.model.security.user.User from, io.goobi.viewer.model.security.user.User to) { return 0; }
        // IndexAugmenter parent-interface members
        @Override public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) { /* no-op stub */ }
        @Override public boolean augmentReIndexPage(String pi, int page, org.apache.solr.common.SolrDocument doc, String dataRepository,
                String namingScheme) {
            return true;
        }
    }
}
