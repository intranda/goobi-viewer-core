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
package io.goobi.viewer.model.jsf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author florian
 *
 */
public class DynamicContent implements Serializable {

    private static final long serialVersionUID = -7672936016976558853L;

    private final DynamicContentType type;
    private final String componentFilename;
    private String id;
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 
     * @param type
     * @param componentFilename
     */
    public DynamicContent(DynamicContentType type, String componentFilename) {
        this.type = type;
        this.componentFilename = componentFilename;
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
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return the type
     */
    public DynamicContentType getType() {
        return type;
    }

    public String getComponentFilename() {
        return componentFilename;
    }

}
