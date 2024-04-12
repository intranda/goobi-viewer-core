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

import io.goobi.viewer.api.rest.model.tasks.TaskParameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST request parameters for SitemapResource.
 */
@Schema(name = "UpdateSitemapTaskParameter", description = "Parameters for creating a sitemap file")
public class SitemapRequestParameters extends TaskParameter {

    @Schema(description = "The directory path in which to write the sitemap file", example = "/opt/digiverso/viewer/sitemap")
    private String outputPath;

    /**
     * <p>
     * Getter for the field <code>outputPath</code>.
     * </p>
     *
     * @return the outputPath
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * <p>
     * Setter for the field <code>outputPath</code>.
     * </p>
     *
     * @param outputPath the outputPath to set
     */
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

}
