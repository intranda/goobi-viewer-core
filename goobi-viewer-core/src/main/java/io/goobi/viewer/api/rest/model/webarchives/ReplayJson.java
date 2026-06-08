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
package io.goobi.viewer.api.rest.model.webarchives;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response model for replaywebpage json")
@JsonInclude(Include.NON_NULL)
public class ReplayJson {

    private final String id;

    private final String name;

    private final List<WebArchiveResource> resources;

    public ReplayJson(String id, String name, List<WebArchiveResource> resources) {
        super();
        this.id = id;
        this.name = name;
        this.resources = resources;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<WebArchiveResource> getResources() {
        return resources;
    }

}
