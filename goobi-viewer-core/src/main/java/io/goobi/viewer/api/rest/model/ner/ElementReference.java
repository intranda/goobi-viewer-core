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
package io.goobi.viewer.api.rest.model.ner;

import java.awt.Rectangle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * ElementReference class.
 */
public class ElementReference {

    private final String id;
    private Rectangle coordinates;
    private String content;
    @JsonInclude(Include.NON_EMPTY)
    private String uri;
    private int page;

    /**
     * Creates a new ElementReference instance.
     */
    public ElementReference() {
        super();
        this.id = null;
        this.coordinates = null;
        this.content = null;
        this.uri = null;
    }

    /**
     * Creates a new ElementReference instance.
     *
     * @param id XML element ID of the referenced NER token
     * @param coordinates bounding box of the element on the page
     * @param content text content of the NER element
     * @param uri Value of the URI attribute
     */
    public ElementReference(String id, Rectangle coordinates, String content, String uri) {
        super();
        this.id = id;
        this.coordinates = coordinates;
        this.content = content;
        this.uri = uri;
    }

    /**
     * Getter for the field <code>id</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getId() {
        return id;
    }

    /**
     * Getter for the field <code>coordinates</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCoordinates() {
        return getAsString(coordinates);
    }

    /**
     * getCoordinatesAsRect.
     *
     * @return a {@link java.awt.Rectangle} object.
     */
    @JsonIgnore
    public Rectangle getCoordinatesAsRect() {
        return coordinates;
    }

    /**
     * Setter for the field <code>coordinates</code>.
     *
     * @param coordinates bounding box of the element on the page
     */
    public void setCoordinates(Rectangle coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Getter for the field <code>content</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getContent() {
        return content;
    }

    /**
     * Setter for the field <code>content</code>.
     *
     * @param content text content of the NER element
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**

     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Getter for the field <code>page</code>.
     *

     */
    public int getPage() {
        return page;
    }

    /**
     * Setter for the field <code>page</code>.
     *
     * @param pageNo 1-based page order number of the referenced element
     */
    public void setPage(int pageNo) {
        this.page = pageNo;
    }

    /**
     * @param rect rectangle to convert to string
     * @return A String representation of the rectangle in the form of x1,y1,x2,y2
     */
    private static String getAsString(Rectangle rect) {
        return rect.getMinX() + "," + rect.getMinY() + "," + rect.getMaxX() + "," + rect.getMaxY();
    }

}
