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
package io.goobi.viewer.model.job;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.modules.IModule;

public enum TaskType implements ITaskType {
    /** Send emails to all search owners if their searches have changed results */
    NOTIFY_SEARCH_UPDATE("0 42 8,12,17 * * ?"),
    /** Remove expired IIIF authorization bearer tokens from application scope. */
    PURGE_EXPIRED_BEARER_TOKENS("0 0 * * * ?"),
    /** Remove expired born digital content download tickets from the DB */
    PURGE_EXPIRED_DOWNLOAD_TICKETS("0 40 0 * * ?"),
    /** Handle asynchronous generation of excel sheets with search results */
    SEARCH_EXCEL_EXPORT(""),
    /** Update the application sitemap */
    UPDATE_SITEMAP("0 50 0 * * ?"),
    /** Update data repository names of a record */
    UPDATE_DATA_REPOSITORY_NAMES(""),
    /** Update uploaded processes status. */
    UPDATE_UPLOAD_JOBS("0 42 0 * * ?"),
    /** Move daily usage statistics to SOLR */
    INDEX_USAGE_STATISTICS("0 45 0 * * ?"),
    /** Create a PDF for a record or part of record to be offered as download **/
    DOWNLOAD_PDF(""),
    /** Write single page pdfs to storage to be used when creating a full record pdf **/
    PRERENDER_PDF("0 35 0 * * ?"),
    /** Fill all CMS-Geomaps with features from SOLR to avoid loading that data during page load */
    CACHE_GEOMAPS("0 0 * * * ?"),
    /** Download a zip archive from a url and extract it so its content may be offered for download */
    DOWNLOAD_EXTERNAL_RESOURCE(""),
    /** Delete a resource previously downloaded by {@link #DOWNLOAD_EXTERNAL_RESOURCE} */
    DELETE_RESOURCE(""),
    /** Pull the git repository of the viewer theme if it exists in the configured location */
    PULL_THEME("0 */1 * * * ?"),
    /** Unload archive trees if any associated records have been reindexed. */
    REFRESH_ARCHIVE_TREE("");

    private final String defaultCronExpression;

    private TaskType(String cronExpression) {
        this.defaultCronExpression = cronExpression;
    }

    /**
     * 
     * @param name
     * @return {@link ITaskType}
     */
    public static ITaskType getByName(String name) {
        for (TaskType val : values()) {
            if (val.name().equals(name)) {
                return val;
            }
        }
        for (IModule module : DataManager.getInstance().getModules()) {
            for (ITaskType type : module.getTaskTypes()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
        }

        return null;
    }

    @Override
    public String getDefaultCronExpression() {
        return defaultCronExpression;
    }
}
