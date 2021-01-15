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
package io.goobi.viewer.api.rest.model.jobs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.goobi.viewer.api.rest.model.SitemapRequestParameters;
import io.goobi.viewer.api.rest.model.ToolsRequestParameters;

/**
 * Object to create job. Used as rest parameter
 * 
 * @author florian
 *
 */
@JsonTypeInfo(
        use = Id.NAME, 
        include = As.PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = SimpleJobParameter.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value=SitemapRequestParameters.class, name = "UPDATE_SITEMAP"),
    @JsonSubTypes.Type(value=ToolsRequestParameters.class, name = "UPDATE_DATA_REPOSITORIES")
}) 
public class SimpleJobParameter {

    public Job.JobType type;

    public SimpleJobParameter() {
    }
    
    public SimpleJobParameter(Job.JobType type) {
        this.type = type;
    }

    
}
