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
package de.intranda.digiverso.presentation.model.viewer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import de.intranda.digiverso.presentation.managedbeans.SearchBean;

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
     * @param size {@link Integer}
     * @param name {@link String}
     */
    public Tag(long size, String name, String field) {
        super();
        this.size = size;
        this.name = name;
        this.field = field;
    }

    @Override
    public int compareTo(Tag otherTag) {
        if (otherTag.getSize() > this.getSize()) {
            return 1;
        } else if (otherTag.getSize() < this.getSize()) {
            return -1;
        }
        return 0;
    }

    /*********************************** Getter and Setter ***************************************/

    public String getCss() {
        return css;
    }

    public void setCss(String css) {
        this.css = css;
    }

    /**
     * @param size the size to set
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public String getEscapedName() {
        try {
            return URLEncoder.encode(name, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return name;
        }
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @param field the field to set
     */
    public void setField(String field) {
        this.field = field;
    }
}
