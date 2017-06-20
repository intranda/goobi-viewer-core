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
package de.intranda.digiverso.presentation.modules;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import de.intranda.digiverso.presentation.controller.AbstractConfiguration;

public interface IModule {

    public String getId();

    public String getName();

    public String getVersion();

    public boolean isLoaded();

    /**
     * Module configuration object.
     * 
     * @return
     */
    public AbstractConfiguration getConfiguration();

    /**
     * URLs for the CMS menu.
     * 
     * @return
     */
    public Map<String, String> getCmsMenuContributions();

    /**
     * URLs to sidebar widgets.
     * 
     * @return
     */
    public List<String> getSidebarContributions();

    /**
     * URLs to widgets containing admin menu links.
     * 
     * @return
     */
    public List<String> getAdminContributions();

    /**
     * Generic widget URLs than can be used from virtually anywhere. The URLs are configured in the config file.
     * 
     * @param type
     * @return
     */
    public List<String> getWidgets(String type);

    /**
     * Any additional tasks this module needs to perform when re-indexing a record (e.g. putting additional files into the hotfolder).
     * 
     * @param pi
     * @param dataRepository
     * @param namingScheme
     */
    public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) throws Exception;

    /**
     * Any additional tasks this module needs to perform when re-indexing a page (e.g. putting additional files into the hotfolder).
     * 
     * @param pi
     * @param page
     * @param doc
     * @param recordType
     * @param dataRepository
     * @param namingScheme
     * @return true if successful; false otherwise
     */
    public boolean augmentReIndexPage(String pi, int page, SolrDocument doc, String recordType, String dataRepository, String namingScheme)
            throws Exception;

    /**
     * Any clean-up the module might want to do when resetting the currently loaded record.
     * 
     * @return true if successful; false otherwise
     */
    public boolean augmentResetRecord();
}
