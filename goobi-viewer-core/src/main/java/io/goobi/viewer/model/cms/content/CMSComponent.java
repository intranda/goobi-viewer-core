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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.goobi.viewer.model.jsf.JsfComponent;

public class CMSComponent {

    private final Long persistenceId;
    
    private final String templateFilename;
    
    private final JsfComponent jsfComponent;
    
    private final List<String> cssClasses = new ArrayList<>();
    
    private final List<CMSContentItem> contentItems = new ArrayList<>();
    
    private final String label;
    
    private final String description;
    
    private final String iconPath;
    
    private ContentItemPublicationState publicationState = ContentItemPublicationState.ADMINISTRATOR;
    
    private int order = 0;
    
    public CMSComponent(CMSComponent template, Optional<PersistentCMSComponent> jpa) {
        this(template.getJsfComponent(), template.getLabel(), template.getDescription(), template.getIconPath(), template.getTemplateFilename(), jpa.map(PersistentCMSComponent::getId).orElse(null));
        this.cssClasses.addAll(jpa.map(PersistentCMSComponent::getCssClasses).orElse(new ArrayList<>()));
        this.publicationState = jpa.map(PersistentCMSComponent::getPublicationState).orElse(this.publicationState);
        this.order = jpa.map(PersistentCMSComponent::getOrder).orElse(this.order);
        List<CMSContentItem> items = template.getContentItems().stream().map(item -> {
            CMSContent content = jpa.map(PersistentCMSComponent::getContentItems).orElse(Collections.emptyList()).stream().filter(i -> i.getComponentId().equals(item.getComponentId()))
                    .findAny().orElse(null);
            if(content != null) {                
                return new CMSContentItem(item.getComponentId(), content, item.getLabel(), item.getDescription(), item.getJsfComponent());
            } else {
                return new CMSContentItem(item);
            }
        }).collect(Collectors.toList());
        this.contentItems.addAll(items);
    }
    
    /**
     * Build from xml template
     * @param jsfComponent
     * @param label
     * @param description
     * @param iconPath
     */
    public CMSComponent(JsfComponent jsfComponent, String label, String description, String iconPath, String templateFilename) {
        this(jsfComponent, label, description, iconPath, templateFilename, null);
    }
    
    private CMSComponent(JsfComponent jsfComponent, String label, String description, String iconPath, String templateFilename, Long persistenceId) {
        this.persistenceId = null;
        this.jsfComponent = jsfComponent;
        this.label = label;
        this.description = description;
        this.iconPath = iconPath;
        this.templateFilename = templateFilename;
    }
    
    public void setPublicationState(ContentItemPublicationState publicationState) {
        this.publicationState = publicationState;
    }
    
    public ContentItemPublicationState getPublicationState() {
        return publicationState;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return order;
    }
    
    public boolean addCssClass(String className) {
        if(!this.cssClasses.contains(className)) {
            return this.cssClasses.add(className);
        } else {
            return false;
        }
    }
    
    public boolean removeClass(String className) {
        if(this.cssClasses.contains(className)) {
            return this.cssClasses.remove(className);
        } else {
            return false;
        }
    }
    
    public List<String> getCssClasses() {
        return cssClasses;
    }
    
    public boolean addContentItem(CMSContentItem item) {
        if(!this.contentItems.contains(item)) {
            return this.contentItems.add(item);
        } else {
            return false;
        }
    }
    
    public boolean removeContentItem(CMSContentItem item) {
        if(this.contentItems.contains(item)) {
            return this.contentItems.remove(item);
        } else {
            return false;
        }
    }
    
    public List<CMSContentItem> getContentItems() {
        return contentItems;
    }
    
    public CMSContentItem getFirstContentItem() {
        return this.contentItems.stream().findFirst().orElse(null);
    }
    
    public CMSContentItem getFirstContentItem(String className) {
        return this.contentItems.stream()
                .filter(item -> item.getClass().getSimpleName().equals(className))
                .findFirst().orElse(null);
    }
    
    public CMSContentItem getContentItem(String componentId) {
        return this.contentItems.stream()
                .filter(item -> item.getComponentId().equals(componentId))
                .findAny().orElse(null);
    }
    
    public JsfComponent getJsfComponent() {
        return jsfComponent;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getIconPath() {
        return iconPath;
    }
    
    public Long getPersistenceId() {
        return persistenceId;
    }
    
    public String getTemplateFilename() {
        return templateFilename;
    }
}
