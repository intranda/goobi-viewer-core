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
package io.goobi.viewer.model.jsf;

/**
 * @author florian
 *
 */
public class DynamicContent {

    private final DynamicContentType type;
    private String id;
    private String[] attributes;
    
    /**
     * 
     */
    public DynamicContent(DynamicContentType type) {
        this.type = type;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the attributes
     */
    public String[] getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
    }

    /**
     * @return the type
     */
    public DynamicContentType getType() {
        return type;
    }

    /**
     * @param i
     * @return
     */
    public String getAttribute(int index) {
        if(attributes == null || index < 0 || index >= attributes.length) {
            return null;
        } else {
            return attributes[index];
        }
    }
    
    
}
