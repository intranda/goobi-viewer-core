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
package io.goobi.viewer.model.cms;

import java.util.ArrayList;
import java.util.List;

import io.goobi.viewer.model.cms.content.CMSComponent;
import io.goobi.viewer.model.cms.content.CMSHtmlText;
import io.goobi.viewer.model.cms.content.CMSImage;
import io.goobi.viewer.model.cms.content.CMSText;

/**
 * Loads {@link CMSComponent components} to include in a {@link CMSPage}
 * 
 * @author florian
 *
 */
public class CMSPageContentManager {
    
    private final List<CMSComponent> components;

    public CMSPageContentManager() {
        this.components = loadDefaultComponents();
    }

    private List<CMSComponent> loadDefaultComponents() {
        List<CMSComponent> list = new ArrayList<>();
        
        list.add(new CMSComponent("htmltext", new CMSHtmlText()));
        list.add(new CMSComponent("text", new CMSText()));
        list.add(new CMSComponent("image", new CMSImage()));
        
        return list;
    }
    
    public List<CMSComponent> getComponents() {
        return components;
    }
}


