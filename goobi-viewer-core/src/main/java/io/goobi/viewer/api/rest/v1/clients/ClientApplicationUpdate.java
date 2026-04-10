/*
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
package io.goobi.viewer.api.rest.v1.clients;

import io.goobi.viewer.model.security.clients.ClientApplication.AccessStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body DTO for PUT /clients/{id}.
 *
 * <p>Used only for OpenAPI schema generation. The actual deserialization still targets
 * {@link io.goobi.viewer.model.security.clients.ClientApplication} because Jackson
 * ignores unknown fields and all writable fields are also present there.
 *
 * <p>Keeping this class separate (without readOnly properties) avoids a schemathesis 4.x
 * incompatibility: the examples phase fails when a schema mixes readOnly properties
 * with writable properties that carry example values.
 */
public class ClientApplicationUpdate {

    @Schema(description = "The name to be displayed for the client", example = "Windows Desktop 1")
    private String name;

    @Schema(description = "A description of the client", example = "second workplace, right aisle")
    private String description;

    @Schema(description = "An IP Subnet mask. If present, the client may only log in if its current IP matches the mask",
            example = "192.168.0.1/16") //NOSONAR, example IP for documentation
    private String subnetMask;

    @Schema(description = "The access status of the client. Only clients with access status 'GRANTED' benefit from client privileges",
            example = "GRANTED")
    private AccessStatus accessStatus;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }

    public void setAccessStatus(AccessStatus accessStatus) {
        this.accessStatus = accessStatus;
    }
}
