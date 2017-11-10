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
package de.intranda.digiverso.presentation.model.rss;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Description for an RSS feed object
 * 
 * @author Florian Alpers
 *
 */
@XmlRootElement
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Description {
    
    private String image;
    private String text;
    private List<RssMetadata> metadata = new ArrayList<>();
    
    public Description() {
        text = null;
    }
    /**
     * @param value
     */
    public Description(String value) {
        text = value;
    }
    /**
     * @return the image
     */
    public String getImage() {
        return image;
    }
    /**
     * @param image the image to set
     */
    public void setImage(String image) {
        this.image = image;
    }
    /**
     * @return the description
     */
    public String getText() {
        return text;
    }
    /**
     * @param description the description to set
     */
    public void setText(String description) {
        this.text = description;
    }
    
    /**
     * 
     * @return all rss metadata of this object
     */
    public List<RssMetadata> getMetadata() {
        return this.metadata;
    }
    
    /**
     * Add rss metadata to this object
     * 
     * @param metadata
     */
    public void addMetadata(RssMetadata metadata) {
        this.metadata.add(metadata);
    }
    
    

}
