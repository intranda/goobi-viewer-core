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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.goobi.viewer.controller.AbstractConfiguration;
import io.goobi.viewer.model.job.ITaskType;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.modules.interfaces.IURLBuilder;
import io.goobi.viewer.modules.interfaces.IndexAugmenter;

/**
 * IModule interface.
 */
public interface IModule extends IndexAugmenter {

    /**
     * getId.
     *
     * @return the unique identifier of this module
     */
    public String getId();

    /**
     * getName.
     *
     * @return the display name of this module
     */
    public String getName();

    /**
     * getVersion.
     *
     * @return the version string of this module
     */
    public String getVersion();

    /**
     * @return JSON object with version data
     */
    public String getVersionJson();

    /**
     * isLoaded.
     *
     * @return true if this module has been successfully loaded and is active, false otherwise
     */
    public boolean isLoaded();

    /**
     * Module configuration object.
     *
     * @return the configuration object for this module
     */
    public AbstractConfiguration getConfiguration();

    /**
     * URLs for the CMS menu.
     *
     * @return a map of display labels to URLs for this module's CMS menu contributions
     */
    public Map<String, String> getCmsMenuContributions();

    /**
     * URLs to sidebar widgets.
     *
     * @return a list of sidebar widget URLs contributed by this module
     */
    public List<String> getSidebarContributions();

    /**
     * Contributions widget_usage.xhtml.
     *
     * @return List of HTML component URLs.
     */
    public List<String> getWidgetUsageContributions();

    /**
     * URLs to widgets containing admin menu links.
     *
     * @return a list of admin widget URLs contributed by this module
     */
    public List<String> getAdminContributions();

    /**
     * URLs to widgets containing navigation menu links.
     *
     * @return a list of login navigation widget URLs contributed by this module
     */
    public List<String> getLoginNavigationContributions();

    /**
     * Generic widget URLs than can be used from virtually anywhere. The URLs are configured in the config file.
     *
     * @param type widget type identifier from configuration
     * @return a list of widget URLs of the given type contributed by this module
     */
    public List<String> getWidgets(String type);

    /**
     * Returns available quartz types.
     * 
     * @return List<ITaskType>
     */
    public List<ITaskType> getTaskTypes();

    /**
     * Any clean-up the module might want to do when resetting the currently loaded record.
     *
     * @return true if successful; false otherwise
     */
    public boolean augmentResetRecord();

    /**
     * Removes content created by the given user.
     *
     * @param user User whose content to delete
     * @return Number of deleted contributions
     */
    public int deleteUserContributions(User user);

    /**
     * Moves all content created by the given user to a different user.
     *
     * @param fromUser Source user
     * @param toUser Destination user
     * @return Number of updated rows
     */
    public int moveUserContributions(User fromUser, User toUser);

    /**
     * getURLBuilder.
     *
     * @return the {@link io.goobi.viewer.modules.interfaces.IURLBuilder} for this module, if any. If this module should not alter url building, an
     *         empty optional should be returned
     */
    default Optional<IURLBuilder> getURLBuilder() {
        return Optional.empty();
    }
}
