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
package io.goobi.viewer.model.cms.content;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps a {@link CMSContent} within a {@link CMSPage}
 * @author florian
 *
 */
public class CMSContentItem {
    
    /**
     * Local identifier within the component. Used to reference this item within the component xhtml
     */
    private final String componentId;
    
    /**
     * The actual {@link CMSContent} wrapped in this item
     */
    private final CMSContent content;
    
    private final Map<String, Object> attributes = new HashMap<>(); 

    /**
     * @param componentId
     * @param content
     */
    public CMSContentItem(String componentId, CMSContent content) {
        super();
        this.componentId = componentId;
        this.content = content;
    }
    
    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }
    
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }
    
    public String getComponentId() {
        return componentId;
    }
    
    public CMSContent getContent() {
        return content;
    }

}
