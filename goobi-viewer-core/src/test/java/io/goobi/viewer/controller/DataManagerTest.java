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
package io.goobi.viewer.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.modules.interfaces.IURLBuilder;

class DataManagerTest {

    /**
     * @see DataManager#registerModule(IModule)
     * @verifies not add module if it's already registered
     */
    @Test
    void registerModule_shouldNotAddModuleIfItsAlreadyRegistered() throws Exception {

        class TestModule implements IModule {

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getId()
             */
            @Override
            public String getId() {
                return "viewer-module-test";
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getName()
             */
            @Override
            public String getName() {
                return "Test Module";
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getVersion()
             */
            @Override
            public String getVersion() {
                return "1.0";
            }

            @Override
            public String getVersionJson() {
                return "{\"version\": \"1.0\"}";
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#isLoaded()
             */
            @Override
            public boolean isLoaded() {
                return true;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getConfiguration()
             */
            @Override
            public AbstractConfiguration getConfiguration() {
                return null;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getCmsMenuContributions()
             */
            @Override
            public Map<String, String> getCmsMenuContributions() {
                return null;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getSidebarContributions()
             */
            @Override
            public List<String> getSidebarContributions() {
                return null;
            }

            @Override
            public List<String> getWidgetUsageContributions() {
                return null;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getAdminContributions()
             */
            @Override
            public List<String> getAdminContributions() {
                return null;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getLoginNavigationContributions()
             */
            @Override
            public List<String> getLoginNavigationContributions() {
                return null;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getWidgets(java.lang.String)
             */
            @Override
            public List<String> getWidgets(String type) {
                return null;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#augmentReIndexRecord(java.lang.String, java.lang.String, java.lang.String)
             */
            @Override
            public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) throws Exception {
                //
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#augmentReIndexPage(java.lang.String, int, org.apache.solr.common.SolrDocument, java.lang.String, java.lang.String)
             */
            @Override
            public boolean augmentReIndexPage(String pi, int page, SolrDocument doc, String dataRepository, String namingScheme) throws Exception {
                return false;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#augmentResetRecord()
             */
            @Override
            public boolean augmentResetRecord() {
                return false;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#getURLBuilder()
             */
            @Override
            public Optional<IURLBuilder> getURLBuilder() {
                return Optional.empty();
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#deleteUserContributions(io.goobi.viewer.model.security.user.User)
             */
            @Override
            public int deleteUserContributions(User user) {
                return 0;
            }

            /* (non-Javadoc)
             * @see io.goobi.viewer.modules.IModule#moveUserContributions(io.goobi.viewer.model.security.user.User, io.goobi.viewer.model.security.user.User)
             */
            @Override
            public int moveUserContributions(User user, User toUser) {
                return 0;
            }
        }
        Assertions.assertTrue(DataManager.getInstance().registerModule(new TestModule()));
        Assertions.assertFalse(DataManager.getInstance().registerModule(new TestModule()));
    }
}
