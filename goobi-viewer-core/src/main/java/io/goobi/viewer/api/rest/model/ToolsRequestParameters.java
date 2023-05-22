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
 * POST request parameters for RecordsResource.
 */
@Schema(name = "UpdateRepositoryParameter", description = "Parameters for updating the cached respository name of a record")
public class ToolsRequestParameters extends TaskParameter {

    @Schema(description = "Persistent identifier of the record to update", example = "PPN123456")
    private String pi;
    @Schema(description = "Value of the SOLR field 'DATAREPOSITORY' of the given record", example = "/opt/digiverso/viewer/data/1")
    private String dataRepositoryName;

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
     * @return the dataRepositoryName
     */
    public String getDataRepositoryName() {
        return dataRepositoryName;
    }

    /**
     * @param dataRepositoryName the dataRepositoryName to set
     */
    public void setDataRepositoryName(String dataRepositoryName) {
        this.dataRepositoryName = dataRepositoryName;
    };

}
