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

public enum TaskType {
    /** Send emails to all search owners if their searches have changed results */
    NOTIFY_SEARCH_UPDATE("0 42 8,12,17 * * ?"),
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
    /** Create a pdf for a record or part of record to be offered as download **/
    DOWNLOAD_PDF(""),
    /** Write single page pdfs to storage to be used when creating a full record pdf **/
    PRERENDER_PDF("0 35 0 * * ?"),
    /** Fill all CMS-Geomaps with features from SOLR to avoid loading that data during page load */
    CACHE_GEOMAPS("0 /5 * * * ?");

    private final String defaultCronExpression;

    private TaskType(String cronExpression) {
        this.defaultCronExpression = cronExpression;
    }

    public String getDefaultCronExpression() {
        return defaultCronExpression;
    }
}
