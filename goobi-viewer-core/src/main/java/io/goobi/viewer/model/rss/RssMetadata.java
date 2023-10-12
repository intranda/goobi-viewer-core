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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents metadata for an RSS feed object
 *
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RssMetadata {

    private final String link;
    private final String label;
    private final String value;

    public RssMetadata() {
        link = null;
        label = null;
        value = null;
    }

    /**
     * <p>
     * Constructor for RssMetadata.
     * </p>
     *
     * @param label a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param link a {@link java.lang.String} object.
     */
    public RssMetadata(String label, String value, String link) {
        super();
        this.link = link;
        this.label = label;
        this.value = value;
    }

    /**
     * <p>
     * Constructor for RssMetadata.
     * </p>
     *
     * @param label a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public RssMetadata(String label, String value) {
        super();
        this.link = null;
        this.label = label;
        this.value = value;
    }

    /**
     * <p>
     * Getter for the field <code>link</code>.
     * </p>
     *
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

}
