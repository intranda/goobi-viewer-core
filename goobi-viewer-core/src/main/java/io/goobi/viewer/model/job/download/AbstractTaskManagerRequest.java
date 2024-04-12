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
package io.goobi.viewer.model.job.download;

/**
 * @author florian
 *
 */
public class AbstractTaskManagerRequest {

    private final String jobtype;
    private final String type;
    private String goobiId;
    private String sourceDir;
    private String targetDir;
    private String pi;
    private String logId;
    private String language;

    public AbstractTaskManagerRequest(String type) {
        this.jobtype = "VIEWERDOWNLOAD";
        this.type = type;
    }

    public String getJobtype() {
        return jobtype;
    }

    public String getType() {
        return type;
    }

    /**
     * @return the goobiId
     */
    public String getGoobiId() {
        return goobiId;
    }

    /**
     * @param goobiId the goobiId to set
     */
    public void setGoobiId(String goobiId) {
        this.goobiId = goobiId;
    }

    /**
     * @return the sourceDir
     */
    public String getSourceDir() {
        return sourceDir;
    }

    /**
     * @param sourceDir the sourceDir to set
     */
    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    /**
     * @return the targetDir
     */
    public String getTargetDir() {
        return targetDir;
    }

    /**
     * @param targetDir the targetDir to set
     */
    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the logId
     */
    public String getLogId() {
        return logId;
    }

    /**
     * @param logId the logId to set
     */
    public void setLogId(String logId) {
        this.logId = logId;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }
}
