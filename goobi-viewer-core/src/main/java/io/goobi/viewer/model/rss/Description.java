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
package io.goobi.viewer.model.rss;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Description for an RSS feed object.
 *
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Description {

    private String image;
    private String text;
    private List<RssMetadata> metadata = new ArrayList<>();

    /**
     * Creates a new Description instance.
     */
    public Description() {
        text = null;
    }

    /**
     * Creates a new Description instance.
     *
     * @param value initial text content of the description
     */
    public Description(String value) {
        text = value;
    }

    /**
     * Getter for the field <code>image</code>.
     *
     * @return the URL of the thumbnail image for this RSS item description
     */
    public String getImage() {
        return image;
    }

    /**
     * Setter for the field <code>image</code>.
     *
     * @param image the URL of the thumbnail image for this RSS item description
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Getter for the field <code>text</code>.
     *
     * @return the HTML or plain-text body of this RSS item description
     */
    public String getText() {
        return text;
    }

    /**
     * Setter for the field <code>text</code>.
     *
     * @param description the HTML or plain-text body of this RSS item description
     */
    public void setText(String description) {
        this.text = description;
    }

    /**
     * Getter for the field <code>metadata</code>.
     *
     * @return all rss metadata of this object
     */
    public List<RssMetadata> getMetadata() {
        return this.metadata;
    }

    /**
     * Adds rss metadata to this object.
     *
     * @param metadata RSS metadata entry to append
     */
    public void addMetadata(RssMetadata metadata) {
        this.metadata.add(metadata);
    }

}
