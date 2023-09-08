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

@Schema(name = "PrerenderPdfsRequestParameters", description = "additional parameters to prerender single page pdfs", requiredProperties = { "pi" })
public class PrerenderPdfsRequestParameters extends TaskParameter {

    @Schema(description = "Record persistent identifier", example = "PPN12345")
    private String pi;
    @Schema(description = "ContentServer config variant to use when creating the pdfs", example = "default")
    private String variant;
    @Schema(description = "Set to true if existing pdf files should be overwritten", example = "false")
    private Boolean force;

    public String getPi() {
        return pi;
    }

    public void setPi(String pi) {
        this.pi = pi;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

}
