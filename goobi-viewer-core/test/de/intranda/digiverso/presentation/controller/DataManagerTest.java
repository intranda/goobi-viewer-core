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
package de.intranda.digiverso.presentation.controller;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.modules.IModule;

public class DataManagerTest {

    /**
     * @see DataManager#registerModule(IModule)
     * @verifies not add module if it's already registered
     */
    @Test
    public void registerModule_shouldNotAddModuleIfItsAlreadyRegistered() throws Exception {

        class TestModule implements IModule {

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#getId()
             */
            @Override
            public String getId() {
                return "viewer-module-test";
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#getName()
             */
            @Override
            public String getName() {
                return "Test Module";
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#getVersion()
             */
            @Override
            public String getVersion() {
                return "1.0";
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#isLoaded()
             */
            @Override
            public boolean isLoaded() {
                return true;
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#getConfiguration()
             */
            @Override
            public AbstractConfiguration getConfiguration() {
                return null;
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#getCmsMenuContributions()
             */
            @Override
            public Map<String, String> getCmsMenuContributions() {
                return null;
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#getSidebarContributions()
             */
            @Override
            public List<String> getSidebarContributions() {
                return null;
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#getAdminContributions()
             */
            @Override
            public List<String> getAdminContributions() {
                return null;
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#getWidgets(java.lang.String)
             */
            @Override
            public List<String> getWidgets(String type) {
                return null;
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#augmentReIndexRecord(java.lang.String, java.lang.String, java.lang.String)
             */
            @Override
            public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) throws Exception {
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#augmentReIndexPage(java.lang.String, int, org.apache.solr.common.SolrDocument, java.lang.String, java.lang.String, java.lang.String)
             */
            @Override
            public boolean augmentReIndexPage(String pi, int page, SolrDocument doc, String recordType, String dataRepository, String namingScheme)
                    throws Exception {
                return false;
            }

            /* (non-Javadoc)
             * @see de.intranda.digiverso.presentation.modules.IModule#augmentResetRecord()
             */
            @Override
            public boolean augmentResetRecord() {
                return false;
            }

        }
        Assert.assertTrue(DataManager.getInstance().registerModule(new TestModule()));
        Assert.assertFalse(DataManager.getInstance().registerModule(new TestModule()));
    }
}