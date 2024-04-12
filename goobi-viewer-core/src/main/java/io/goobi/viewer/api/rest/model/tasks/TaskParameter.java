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
package io.goobi.viewer.api.rest.model.tasks;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.goobi.viewer.api.rest.model.PrerenderPdfsRequestParameters;
import io.goobi.viewer.api.rest.model.SitemapRequestParameters;
import io.goobi.viewer.api.rest.model.ToolsRequestParameters;
import io.goobi.viewer.api.rest.v1.tasks.TasksResource;
import io.goobi.viewer.model.job.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Object to create a {@link Task}. Used as rest parameter for the POST call to {@link TasksResource}. Must always pass a valid {@link #type}
 * parameter. Depending on the type, a subclass of TaskParameter may contain additional properties. These properties are interpreted in
 * {@link TaskManager#createTask(TaskType)}
 *
 * @author florian
 *
 */
@JsonTypeInfo(
        use = Id.NAME,
        include = As.PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = TaskParameter.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SitemapRequestParameters.class, name = "UPDATE_SITEMAP"),
        @JsonSubTypes.Type(value = ToolsRequestParameters.class, name = "UPDATE_DATA_REPOSITORY_NAMES"),
        @JsonSubTypes.Type(value = PrerenderPdfsRequestParameters.class, name = "PRERENDER_PDF")
})
@Schema(name = "ViewerTaskParameter",
        description = "Contains the type of the task to execute as well as possible additional parameters depending on the type of the task",
        requiredProperties = { "type" })
public class TaskParameter {

    @Schema(description = "The type of the task to execute")
    private TaskType type;

    public TaskParameter() {
    }

    public TaskParameter(TaskType type) {
        this.type = type;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

}
