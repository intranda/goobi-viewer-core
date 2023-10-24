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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.JsfComponent;
import io.goobi.viewer.model.security.user.User;

public class CMSComponent implements Comparable<CMSComponent>, Serializable {

    private static final long serialVersionUID = -6820973601918866139L;

    private final String templateFilename;

    private final JsfComponent jsfComponent;

    private final List<CMSContentItem> contentItems = new ArrayList<>();

    private final String label;

    private final String description;

    private final String iconPath;

    private Integer order = null;

    private final Map<String, CMSComponentAttribute> attributes;

    private final PersistentCMSComponent persistentComponent;

    private transient UIComponent uiComponent;
    private transient UIComponent backendUiComponent;

    private CMSComponentScope scope = CMSComponentScope.PAGEVIEW;

    public CMSComponent(CMSComponent template, List<CMSContentItem> items) {
        this(template.getJsfComponent(), template.getLabel(), template.getDescription(), template.getIconPath(), template.getTemplateFilename(),
                template.getScope(), template.getAttributes(), template.getOrder(), Optional.ofNullable(template.getPersistentComponent()));
        List<CMSContentItem> newItems = items.stream().map(CMSContentItem::new).collect(Collectors.toList());
        this.contentItems.addAll(newItems);
    }

    public CMSComponent(CMSComponent template, Optional<PersistentCMSComponent> jpa) {
        this(template.getJsfComponent(), template.getLabel(), template.getDescription(), template.getIconPath(), template.getTemplateFilename(),
                template.getScope(),
                CMSComponent.initializeAttributes(template.getAttributes(),
                        jpa.map(PersistentCMSComponent::getAttributes).orElse(Collections.emptyMap())),
                template.getOrder(),
                jpa);
        List<CMSContent> contentData = jpa.map(PersistentCMSComponent::getContentItems).orElse(Collections.emptyList());
        List<CMSContentItem> items =
                template.getContentItems().stream().map(item -> populateContentItem(item, contentData)).collect(Collectors.toList());
        this.contentItems.addAll(items);
    }

    public CMSComponent(JsfComponent jsfComponent, String label, String description, String iconPath, String templateFilename,
            CMSComponentScope scope, Map<String, CMSComponentAttribute> attributes, Integer order) {
        this(jsfComponent, label, description, iconPath, templateFilename, scope, attributes, order, Optional.empty());
    }

    private CMSComponent(JsfComponent jsfComponent, String label, String description, String iconPath, String templateFilename,
            CMSComponentScope scope, Map<String, CMSComponentAttribute> attributes, Integer order, Optional<PersistentCMSComponent> jpa) {
        this.jsfComponent = jsfComponent;
        this.label = label;
        this.description = description;
        this.iconPath = iconPath;
        this.templateFilename = templateFilename;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
        this.scope = scope;
        this.persistentComponent = jpa.orElse(null);
        this.order = Optional.ofNullable(order).orElse(jpa.map(PersistentCMSComponent::getOrder).orElse(0));
    }

    public PersistentCMSComponent getPersistentComponent() {
        return persistentComponent;
    }

    public void setPublicationState(ContentItemPublicationState publicationState) {
        Optional.ofNullable(persistentComponent).ifPresent(p -> p.setPublicationState(publicationState));
    }

    public ContentItemPublicationState getPublicationState() {
        return Optional.ofNullable(persistentComponent)
                .map(PersistentCMSComponent::getPublicationState)
                .orElse(ContentItemPublicationState.PUBLISHED);
    }

    public void setOrder(int order) {
        Optional.ofNullable(persistentComponent).ifPresent(p -> p.setOrder(order));
        this.order = order;
    }

    public Integer getOrder() {
        return Optional.ofNullable(persistentComponent).map(PersistentCMSComponent::getOrder).orElse(this.order);
    }

    public boolean addContentItem(CMSContentItem item) {
        if (!this.contentItems.contains(item)) {
            return this.contentItems.add(item);
        }
        return false;
    }

    public boolean removeContentItem(CMSContentItem item) {
        if (this.contentItems.contains(item)) {
            return this.contentItems.remove(item);
        }
        return false;
    }

    public List<CMSContentItem> getContentItems() {
        return contentItems;
    }

    public CMSContentItem getFirstContentItem() {
        return this.contentItems.stream().findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends CMSContent> T getFirstContentOfType(Class<? extends CMSContent> clazz) {
        return (T) this.contentItems.stream()
                .map(CMSContentItem::getContent)
                .filter(c -> c.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends CMSContent> List<T> getAllContentOfType(Class<? extends CMSContent> clazz) {
        return (List<T>) this.contentItems.stream()
                .map(CMSContentItem::getContent)
                .filter(c -> c.getClass().equals(clazz))
                .collect(Collectors.toList());
    }

    public CMSContentItem getFirstContentItem(String className) {
        return this.contentItems.stream()
                .filter(item -> item.getClass().getSimpleName().equals(className))
                .findFirst()
                .orElse(null);
    }

    public CMSContentItem getContentItem(String componentId) {
        return this.contentItems.stream()
                .filter(item -> item.getItemId().equals(componentId))
                .findAny()
                .orElse(null);
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

    public boolean isPageScope() {
        return CMSComponentScope.PAGEVIEW == this.scope;
    }

    @Override
    public int compareTo(CMSComponent o) {
        int scopeCompare = Integer.compare(this.getScope().ordinal(), o.getScope().ordinal());
        int orderCompare = Integer.compare(this.getOrder(), o.getOrder());
        return scopeCompare * 100_000 + orderCompare;
    }

    public UIComponent getUiComponent() throws PresentationException {

        if (this.uiComponent == null && this.jsfComponent != null && StringUtils.isNotBlank(this.jsfComponent.getFilename())) {
            DynamicContentBuilder builder = new DynamicContentBuilder();
            this.uiComponent = FacesContext.getCurrentInstance().getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            this.uiComponent.setId(FilenameUtils.getBaseName(this.templateFilename) + "_" + Optional.ofNullable(this.order).orElse(0));
            UIComponent component = builder.build(this.getJsfComponent(), this.uiComponent, Collections.emptyMap());
            component.getAttributes().put("component", this);
            for (CMSComponentAttribute attribute : this.getAttributes().values()) {
                component.getAttributes().put(attribute.getName(), attribute.isBooleanValue() ? attribute.getBooleanValue() : attribute.getValue());
            }
        }
        return uiComponent;
    }

    public void setUiComponent(UIComponent uiComponent) {
        this.uiComponent = uiComponent;
    }

    public UIComponent getBackendUiComponent() throws PresentationException {
        if (this.backendUiComponent == null) {
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

    public boolean getBooleanAttributeValue(String key, boolean defaultValue) {
        return Optional.ofNullable(this.attributes).map(map -> map.get(key)).map(CMSComponentAttribute::getBooleanValue).orElse(defaultValue);
    }

    public String getAttributeValue(String key) {
        return Optional.ofNullable(this.attributes).map(map -> map.get(key)).map(CMSComponentAttribute::getValue).orElse("");
    }

    public void setAttribute(String key, String value) {
        CMSComponentAttribute attr = this.attributes.get(key);
        if (attr != null) {
            this.attributes.put(key, new CMSComponentAttribute(attr, value));
            Optional.ofNullable(persistentComponent).ifPresent(p -> p.setAttribute(key, value));
        } else {
            throw new IllegalArgumentException("Attempting to change the not configured attribute " + key);
        }
    }

    public void toggleAttribute(String key, String value) {
        String oldValue = getAttribute(key).getValue();
        if (StringUtils.isBlank(oldValue)) {
            setAttribute(key, value);
        } else if (oldValue.equals(value)) {
            setAttribute(key, null);
        } else {
            setAttribute(key, value);
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
     * Create a new CMSContentItem based on the given item and populated with a CMSContent from the given contentData list with a
     * {@link CMSContent#getItemId()} equals to the item's {@link CMSContentItem#getItemId()}
     * 
     * @param item
     * @param contentData
     * @return
     */
    private CMSContentItem populateContentItem(CMSContentItem item, List<CMSContent> contentData) {
        CMSContent content = contentData
                .stream()
                .filter(i -> i.getItemId().equals(item.getItemId()))
                .findAny()
                .orElse(null);
        if (content != null) {
            return new CMSContentItem(item.getItemId(), content, item.getLabel(), item.getDescription(), item.getHtmlGroup(), item.getJsfComponent(),
                    this,
                    item.isRequired());
        }

        CMSContentItem newContentItem = new CMSContentItem(item);
        this.persistentComponent.addContent(newContentItem.getContent());
        return newContentItem;

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
        if (isPublished()) {
            return true;
        }
        return user != null && user.isCmsAdmin();
    }

    public String getContentData(String itemId) {
        return getContentData(itemId, null, null);
    }

    public String getContentData(String itemId, Integer width, Integer height) {
        return this.contentItems.stream()
                .filter(item -> Objects.equals(item.getItemId(), itemId))
                .findAny()
                .map(CMSContentItem::getContent)
                .map(c -> c.getData(width, height))
                .orElse("");
    }

    /**
     * Check whether a contentItem with the given itemId exists and is not empty
     * 
     * @param itemId
     * @return true if the contentItem with the given itemId exists and its {@link CMSContent#isEmpty()} method returns false
     */
    public boolean hasContent(String itemId) {
        return this.contentItems.stream().anyMatch(item -> Objects.equals(item.getItemId(), itemId) && !item.isEmpty());
    }

    public CMSContent getContent(String itemId) {
        return Optional.ofNullable(this.getContentItem(itemId)).map(CMSContentItem::getContent).orElse(null);
    }

    /**
     * Set wether this component should be displayed when the owning page is embedded in another page, rather than on the owning page itself
     * 
     * @param preview
     */
    public void setPreview(boolean preview) {
        this.scope = preview ? CMSComponentScope.PREVIEW : CMSComponentScope.PAGEVIEW;
    }

    /**
     * wether this component should be displayed when the owning page is embedded in another page, rather than on the owning page itself
     * 
     * @return
     */
    public boolean isPreview() {
        return CMSComponentScope.PREVIEW.equals(this.scope);
    }

    public CMSComponentScope getScope() {
        return scope;
    }

    public void setScope(CMSComponentScope scope) {
        this.scope = scope;
    }

    public long getPersistenceId() {
        return Optional.ofNullable(this.persistentComponent).map(PersistentCMSComponent::getId).orElse(0l);
    }

    public CMSPage getOwningPage() {
        return Optional.ofNullable(this.persistentComponent).map(PersistentCMSComponent::getOwningPage).orElse(null);
    }

    public boolean isPaged() {
        return this.contentItems.stream().map(CMSContentItem::getContent).anyMatch(PagedCMSContent.class::isInstance);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.label == null ? "" : this.label);
        if (this.getPersistentComponent() != null) {
            sb.append(" id: ").append(this.getPersistentComponent().getId());
        }
        if (this.getContentItems() != null) {
            sb.append(" / ").append(this.getContentItems().size()).append(" content items");
        }
        return sb.toString();
    }

}
