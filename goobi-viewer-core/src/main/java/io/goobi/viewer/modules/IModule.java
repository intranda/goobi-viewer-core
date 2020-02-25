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
package io.goobi.viewer.modules;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.AbstractConfiguration;
import io.goobi.viewer.modules.interfaces.IURLBuilder;

/**
 * <p>
 * IModule interface.
 * </p>
 */
public interface IModule {

    /**
     * <p>
     * getId.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getId();

    /**
     * <p>
     * getName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName();

    /**
     * <p>
     * getVersion.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVersion();

    /**
     * <p>
     * isLoaded.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isLoaded();

    /**
     * Module configuration object.
     *
     * @return a {@link io.goobi.viewer.controller.AbstractConfiguration} object.
     */
    public AbstractConfiguration getConfiguration();

    /**
     * URLs for the CMS menu.
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, String> getCmsMenuContributions();

    /**
     * URLs to sidebar widgets.
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getSidebarContributions();

    /**
     * URLs to widgets containing admin menu links.
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getAdminContributions();

    /**
     * URLs to widgets containing navigation menu links.
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getLoginNavigationContributions();

    /**
     * Generic widget URLs than can be used from virtually anywhere. The URLs are configured in the config file.
     *
     * @param type a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<String> getWidgets(String type);

    /**
     * Any additional tasks this module needs to perform when re-indexing a record (e.g. putting additional files into the hotfolder).
     *
     * @param pi a {@link java.lang.String} object.
     * @param dataRepository a {@link java.lang.String} object.
     * @param namingScheme a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) throws Exception;

    /**
     * Any additional tasks this module needs to perform when re-indexing a page (e.g. putting additional files into the hotfolder).
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param dataRepository a {@link java.lang.String} object.
     * @param namingScheme a {@link java.lang.String} object.
     * @return true if successful; false otherwise
     * @throws java.lang.Exception if any.
     */
    public boolean augmentReIndexPage(String pi, int page, SolrDocument doc, String dataRepository, String namingScheme) throws Exception;

    /**
     * Any clean-up the module might want to do when resetting the currently loaded record.
     *
     * @return true if successful; false otherwise
     */
    public boolean augmentResetRecord();

    /**
     * <p>
     * getURLBuilder.
     * </p>
     *
     * @return the {@link io.goobi.viewer.modules.interfaces.IURLBuilder} for this module, if any. If this module should not alter url building, an
     *         empty optional should be returned
     */
    default Optional<IURLBuilder> getURLBuilder() {
        return Optional.empty();
    }
}
