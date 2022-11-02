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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.el.ELException;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.JsfComponent;
import io.goobi.viewer.model.security.user.User;

public class CMSComponent implements Comparable<CMSComponent> {
    
    private final String templateFilename;
    
    private final JsfComponent jsfComponent;
        
    private final List<CMSContentItem> contentItems = new ArrayList<>();
    
    private final String label;
    
    private final String description;
    
    private final String iconPath;
    
    private final Map<String, CMSComponentAttribute> attributes;
    
    private final PersistentCMSComponent persistentComponent;

    private int listPage = 1;
    
    private UIComponent uiComponent;
    private UIComponent backendUiComponent;
    
    public CMSComponent(CMSComponent template, Optional<PersistentCMSComponent> jpa) {
        this(template.getJsfComponent(), template.getLabel(), template.getDescription(), template.getIconPath(), template.getTemplateFilename(), 
                CMSComponent.initializeAttributes(template.getAttributes(), jpa.map(PersistentCMSComponent::getAttributes).orElse(Collections.emptyMap())), jpa);
        List<CMSContent> contentData = jpa.map(j -> j.getContentItems()).orElse(Collections.emptyList());
        List<CMSContentItem> items = template.getContentItems().stream().map(item -> populateContentItem(item, contentData)).collect(Collectors.toList());
        this.contentItems.addAll(items);
    }


    
    public CMSComponent(JsfComponent jsfComponent, String label, String description, String iconPath, String templateFilename, Map<String, CMSComponentAttribute> attributes) {
        this(jsfComponent, label, description, iconPath, templateFilename, attributes, Optional.empty());
    }
    
    private CMSComponent(JsfComponent jsfComponent, String label, String description, String iconPath, String templateFilename, Map<String, CMSComponentAttribute> attributes, Optional<PersistentCMSComponent> jpa) {
        this.jsfComponent = jsfComponent;
        this.label = label;
        this.description = description;
        this.iconPath = iconPath;
        this.templateFilename = templateFilename;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
        this.persistentComponent = jpa.orElse(null);
    }
    
    public PersistentCMSComponent getPersistentComponent() {
        return persistentComponent;
    }
    
    public void setPublicationState(ContentItemPublicationState publicationState) {
        Optional.ofNullable(persistentComponent).ifPresent(p -> p.setPublicationState(publicationState));
    }
    
    public ContentItemPublicationState getPublicationState() {
        return Optional.ofNullable(persistentComponent).map(PersistentCMSComponent::getPublicationState).orElse(ContentItemPublicationState.PUBLISHED);
    }
    
    public void setOrder(int order) {
        Optional.ofNullable(persistentComponent).ifPresent(p -> p.setOrder(order));
    }
    
    public int getOrder() {
        return Optional.ofNullable(persistentComponent).map(PersistentCMSComponent::getOrder).orElse(0);
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
                .filter(item -> item.getItemId().equals(componentId))
                .findAny().orElse(null);
    }
    
    public List<CMSContentItem> getTranslatableContentItems() {
        return this.contentItems.stream()
                .filter(item -> item.getContent() instanceof TranslatableCMSContent)
                .collect(Collectors.toList());
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
    
    public String getTemplateFilename() {
        return templateFilename;
    }
    
    public int getListPage() {
        return listPage;
    }
    
    public void setListPage(int listPage) {
        this.listPage = listPage;
    }

    @Override
    public int compareTo(CMSComponent o) {
        return Integer.compare(this.getOrder(), o.getOrder());
    }
    
    public UIComponent getUiComponent() throws PresentationException {

        if(this.uiComponent == null) {
            DynamicContentBuilder builder = new DynamicContentBuilder();
            String id = FilenameUtils.getBaseName(this.getJsfComponent().getName()) + "_" + System.nanoTime();
            this.uiComponent = new HtmlPanelGroup();
            this.uiComponent.setId(id);
            UIComponent component = builder.build(this.getJsfComponent(), this.uiComponent, Collections.emptyMap());
            component.getAttributes().put("component", this);
            for (CMSComponentAttribute attribute : this.getAttributes().values()) {
                component.getAttributes().put(attribute.getName(), attribute.isBooleanValue() ? attribute.getBooleanValue() : attribute.getValue());
            }
            component.setId(id + "_component");

        }
        return uiComponent;
    }
    
    public void setUiComponent(UIComponent uiComponent) {
        this.uiComponent = uiComponent;
    }
    
    public UIComponent getBackendUiComponent() throws PresentationException {
        if(this.backendUiComponent == null) {
            DynamicContentBuilder builder = new DynamicContentBuilder();
            String id = FilenameUtils.getBaseName("component_" + this.getJsfComponent().getName()) + "_" + System.nanoTime();
            this.backendUiComponent = new HtmlPanelGroup();
            this.backendUiComponent.setId(id);
            for (CMSContentItem cmsContentItem : contentItems) {
                UIComponent itemComponent = cmsContentItem.getUiComponent();
                this.backendUiComponent.getChildren().add(itemComponent);
            }
        }
        return backendUiComponent;
    }
    
    public void setBackendUiComponent(UIComponent backendUiComponent) {
        this.backendUiComponent = backendUiComponent;
    }
    
    public CMSComponentAttribute getAttribute(String key) {
        return this.attributes.get(key);
    }
    
    public String getAttributeValue(String key) {
        return this.attributes.get(key).getValue();
    }
    
    public void setAttribute(String key, String value) {
        CMSComponentAttribute attr = this.attributes.get(key);
        if(attr != null) {
            this.attributes.put(key, new CMSComponentAttribute(attr, value));
            Optional.ofNullable(persistentComponent).ifPresent(p -> p.setAttribute(key, value));
        } else {
            throw new IllegalArgumentException("Attempting to change the not configured attribute " + key);
        }
    }
    
    
    public void toggleAttribute(String key, String value) {
        String oldValue = getAttribute(key).getValue();
        if(StringUtils.isBlank(oldValue)) {
            setAttribute( key, value);
        } else if(oldValue.equals(value)){
            setAttribute( key, null);
        } else {
            setAttribute( key, value);
        }
    }
    
    public Map<String, CMSComponentAttribute> getAttributes() {
        return attributes;
    }
    
    public boolean isPublished() {
        return ContentItemPublicationState.PUBLISHED.equals(this.getPublicationState());
    }
    
    public void setPublished(boolean published) {
        setPublicationState(published ? ContentItemPublicationState.PUBLISHED : ContentItemPublicationState.ADMINISTRATOR);
    }
    
    public boolean isPrivate() {
        return !isPublished();
    }
    
    public void setPrivate(boolean privat) {
        setPublished(!privat);
    }
    
    public void togglePrivate() {
        setPrivate(!this.isPrivate());
    }
    
    public void togglePublished() {
        setPublished(!isPublished());
    }
    
    /**
     * Create a new CMSContentItem based on the given item and populated with a CMSContent from the given contentData list 
     * with a {@link CMSContent#getItemId()} equals to the item's {@link CMSContentItem#getItemId()}
     * @param item
     * @param contentData
     * @return
     */
    private CMSContentItem populateContentItem(CMSContentItem item, List<CMSContent> contentData) {
        CMSContent content = contentData
                .stream().filter(i -> i.getItemId().equals(item.getItemId()))
                .findAny().orElse(null);
        if(content != null) {                
            return new CMSContentItem(item.getItemId(), content, item.getLabel(), item.getDescription(), item.getJsfComponent(), item.isRequired());
        } else {
            return new CMSContentItem(item);
        }
    }
    
    private static Map<String, CMSComponentAttribute> initializeAttributes(Map<String, CMSComponentAttribute> attrs,
            Map<String, String> initialValues) {
        Map<String, CMSComponentAttribute> newAttrs = new HashMap<>(attrs.size());
        for (CMSComponentAttribute attr : attrs.values()) {
            String key = attr.getName();
            String value = Optional.ofNullable(initialValues.get(key)).orElse(attr.getValue());
            CMSComponentAttribute newAttr = new CMSComponentAttribute(attr, value);
            newAttrs.put(key, newAttr);
        }
        return newAttrs;
    }
    
    public boolean hasAccess(User user) {
        if(isPublished()) {
            return true;
        } else {
            return user != null && user.isCmsAdmin();
        }
    }
    
    public String getContentData(String itemId) {
        return getContentData(itemId, null, null);
    }
    
    public String getContentData(String itemId, Integer width, Integer height) {
        return this.contentItems.stream().filter(item -> Objects.equals(item.getItemId(), itemId))
        .findAny().map(CMSContentItem::getContent).map(c -> c.getData(width, height)).orElse("");
    }
    
    /**
     * Check whether a contentItem with the given itemId exists and is not empty
     * @param itemId
     * @return  true if the contentItem with the given itemId exists and its {@link CMSContent#isEmpty()} method returns false
     */
    public boolean hasContent(String itemId) {
        return this.contentItems.stream().anyMatch(item -> Objects.equals(item.getItemId(), itemId) && !item.isEmpty());
    }
}
