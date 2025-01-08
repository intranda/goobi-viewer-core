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
package io.goobi.viewer.api.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POST request parameters for IndexingResource (indexer ersion).
 */
public class IndexerDataRequestParameters {

    private String application;
    private String version;
    @JsonProperty("build-date")
    private String buildDate;
    @JsonProperty("git-revision")
    private String gitRevision;
    @JsonProperty("hotfolder-file-count")
    private int hotfolderFileCount;
    @JsonProperty("record-identifiers")
    private List<String> recordIdentifiers;

    /**
     * @return the application
     */
    public String getApplication() {
        return application;
    }

    /**
     * @param application the application to set
     */
    public void setApplication(String application) {
        this.application = application;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the buildDate
     */
    public String getBuildDate() {
        return buildDate;
    }

    /**
     * @param buildDate the buildDate to set
     */
    public void setBuildDate(String buildDate) {
        this.buildDate = buildDate;
    }

    /**
     * @return the gitRevision
     */
    public String getGitRevision() {
        return gitRevision;
    }

    /**
     * @param gitRevision the gitRevision to set
     */
    public void setGitRevision(String gitRevision) {
        this.gitRevision = gitRevision;
    }

    /**
     * @return the hotfolderFileCount
     */
    public int getHotfolderFileCount() {
        return hotfolderFileCount;
    }

    /**
     * @param hotfolderFileCount the hotfolderFileCount to set
     */
    public void setHotfolderFileCount(int hotfolderFileCount) {
        this.hotfolderFileCount = hotfolderFileCount;
    }

    /**
     * @return the recordIdentifiers
     */
    public List<String> getRecordIdentifiers() {
        return recordIdentifiers;
    }

    /**
     * @param recordIdentifiers the recordIdentifiers to set
     */
    public void setRecordIdentifiers(List<String> recordIdentifiers) {
        this.recordIdentifiers = recordIdentifiers;
    }
}
