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
package de.intranda.digiverso.presentation.servlets.rest.collections;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Part of the IIIF presentation api
 * 
 * Represents a link to another resource, for example in the "related" property 
 * 
 * @author Florian Alpers
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"@id", "label", "format"})
public class CollectionLink {

    private URL link;
    private String label;
    private String format = "text/html";
    
    /**
     * 
     */
    public CollectionLink(URL link, String label) {
        this.link = link;
        this.label = label;
    }
    
    public CollectionLink(URL link, String label, String format) {
        this.link = link;
        this.label = label;
        this.format = format;
    }
    
    /**
     * @return the link
     */
    @JsonProperty("@id")
    public URL getLink() {
        return link;
    }
    
    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }
    
    
}
