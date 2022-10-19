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
package io.goobi.viewer.model.cms.pages.content;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.weld.exceptions.IllegalArgumentException;

import de.intranda.monitoring.timer.Timer;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.JsfComponent;

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
        
    private final String label;
    
    private final String description;
    
    private final JsfComponent jsfComponent;
    
    private final boolean required;

    private UIComponent uiComponent;
    
    public CMSContentItem(CMSContentItem orig) {
        this.componentId = orig.componentId;
        this.content = orig.content.copy();
        this.label = orig.label;
        this.description = orig.description;
        this.jsfComponent = orig.jsfComponent;
        this.required = orig.required;
    }
    
    /**
     * 
     * @param componentId
     * @param content
     */
    public CMSContentItem(String componentId, CMSContent content, String label, String description, JsfComponent jsfComponent, boolean required) {
        if(StringUtils.isNotBlank(componentId)) {
            this.componentId = componentId;            
        } else {
            throw new IllegalArgumentException("ComponentId of CMSContentItem may not be blank");
        }
        if(content != null) {            
            this.content = content;
            this.content.setComponentId(this.getComponentId());
        } else {
            throw new IllegalArgumentException("CMSContent of COMSContentItem may not be null");
        }
        this.label = label;
        this.description = description;
        this.jsfComponent = jsfComponent;
        this.required = required;
    }
    
    public boolean isRequired() {
        return required;
    }

    public String getComponentId() {
        return componentId;
    }
    
    public CMSContent getContent() {
        return content;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getDescription() {
        return description;
    }
    
    public JsfComponent getJsfComponent() {
        return jsfComponent;
    }
    
    @Override
    public int hashCode() {
        return componentId.hashCode();
    }
    
    public boolean isMandatory() {
        return this.required;
    }

    /**
     * Two CMSContentItems are equal if their {@link #componentId}s are equal
     */
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass().equals(this.getClass())) {
            return ((CMSContentItem)obj).componentId.equals(this.componentId);
        } else {
            return false;
        }
    }
    
    public UIComponent getUiComponent() {
        
        if(this.uiComponent == null) {
            DynamicContentBuilder builder = new DynamicContentBuilder();
            String id = FilenameUtils.getBaseName("content_" + this.getJsfComponent().getName()) + "_" + System.nanoTime();
            this.uiComponent = new HtmlPanelGroup();
            this.uiComponent.setId(id);
            UIComponent wrapper = builder.createTag("div", Collections.emptyMap());
            wrapper.setId(id + "_wrapper");
            this.uiComponent.getChildren().add(wrapper);
            UIComponent component = builder.build(this.getJsfComponent(), wrapper, Collections.emptyMap());
            component.getAttributes().put("contentItem", this);
            component.setId(id + "_content");
        }
        return uiComponent;
    }
    
    public void setUiComponent(UIComponent uiComponent) {
        this.uiComponent = uiComponent;
    }

}
