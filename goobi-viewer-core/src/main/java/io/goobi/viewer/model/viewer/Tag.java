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
package io.goobi.viewer.model.viewer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.goobi.viewer.managedbeans.SearchBean;

/**
 * Tag in a tag cloud.
 */
public class Tag implements Comparable<Tag>, Serializable {

    private static final long serialVersionUID = -7786860958620179273L;

    private long size = 1;
    private String field;
    private String name;
    private String css;

    /**
     * Konstruktor.
     *
     * @param size relative weight of this tag in the cloud
     * @param name display label of the tag
     * @param field Solr field this tag is based on
     */
    public Tag(long size, String name, String field) {
        super();
        this.size = size;
        this.name = name;
        this.field = field;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Tag otherTag) {
        if (otherTag.getSize() > this.getSize()) {
            return 1;
        } else if (otherTag.getSize() < this.getSize()) {
            return -1;
        }
        return 0;
    }

    /**
     ********************************* Getter and Setter **************************************.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCss() {
        return css;
    }

    /**
     * Setter for the field <code>css</code>.
     *
     * @param css CSS class name to assign to this tag
     */
    public void setCss(String css) {
        this.css = css;
    }

    /**
     * Setter for the field <code>size</code>.
     *
     * @param size the relative size or weight of this tag in a tag cloud
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Getter for the field <code>size</code>.
     *

     */
    public long getSize() {
        return size;
    }

    /**
     * Setter for the field <code>name</code>.
     *
     * @param name the tag text value to display
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for the field <code>name</code>.
     *

     */
    public String getName() {
        return name;
    }

    /**
     * getEscapedName.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getEscapedName() {
        try {
            return URLEncoder.encode(name, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return name;
        }
    }

    /**
     * Getter for the field <code>field</code>.
     *

     */
    public String getField() {
        return field;
    }

    /**
     * Setter for the field <code>field</code>.
     *
     * @param field the Solr field name this tag is associated with
     */
    public void setField(String field) {
        this.field = field;
    }
}
