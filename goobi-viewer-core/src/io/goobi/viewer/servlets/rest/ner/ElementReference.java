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
package io.goobi.viewer.servlets.rest.ner;

import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * <p>ElementReference class.</p>
 *
 */
@XmlRootElement
@XmlType(propOrder={"id", "content", "coordinates"})
public class ElementReference {

    private final String id;
    private Rectangle coordinates;
    private String content;
    private int page;

    
    
    /**
     * <p>Constructor for ElementReference.</p>
     */
    public ElementReference() {
        super();
        this.id = null;
        this.coordinates = null;
        this.content = null;
    }



    /**
     * <p>Constructor for ElementReference.</p>
     *
     * @param id a {@link java.lang.String} object.
     * @param coordinates a {@link java.awt.Rectangle} object.
     * @param content a {@link java.lang.String} object.
     */
    public ElementReference(String id, Rectangle coordinates, String content) {
        super();
        this.id = id;
        this.coordinates = coordinates;
        this.content = content;
    }

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @XmlElement
    public String getId() {
        return id;
    }

    /**
     * <p>Getter for the field <code>coordinates</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @XmlElement
    public String getCoordinates() {
        return getAsString(coordinates);
    }

    /**
     * <p>getCoordinatesAsRect.</p>
     *
     * @return a {@link java.awt.Rectangle} object.
     */
    @JsonIgnore
    public Rectangle getCoordinatesAsRect() {
        return coordinates;
    }


    /**
     * <p>Setter for the field <code>coordinates</code>.</p>
     *
     * @param coordinates a {@link java.awt.Rectangle} object.
     */
    public void setCoordinates(Rectangle coordinates) {
        this.coordinates = coordinates;
    }


    /**
     * <p>Getter for the field <code>content</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @XmlElement
    public String getContent() {
        return content;
    }



    /**
     * <p>Setter for the field <code>content</code>.</p>
     *
     * @param content a {@link java.lang.String} object.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * <p>Getter for the field <code>page</code>.</p>
     *
     * @return the pageNo
     */
    public int getPage() {
        return page;
    }
    
    /**
     * <p>Setter for the field <code>page</code>.</p>
     *
     * @param pageNo the pageNo to set
     */
    public void setPage(int pageNo) {
        this.page = pageNo;
    }


    /**
     * @param rect
     * @return A String representation of the rectangle in the form of x1,y1,x2,y2
     */
    private static String getAsString(Rectangle rect) {
        return rect.getMinX() + ","  + rect.getMinY() + "," + rect.getMaxX() + ","  +rect.getMaxY();
    }

}
