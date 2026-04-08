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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.JsfComponent;
import io.goobi.viewer.model.security.user.User;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.html.HtmlPanelGroup;
import jakarta.faces.context.FacesContext;

/**
 * Represents a reusable UI component on a CMS page, combining a JSF component reference with
 * ordered content items, display metadata, and optional access restrictions.
 */
public class CMSComponent implements Comparable<CMSComponent>, Serializable {

    private static final long serialVersionUID = -6820973601918866139L;

    private final String templateFilename;

    private final JsfComponent jsfComponent;

    private final List<CMSContentItem> contentItems = new ArrayList<>();

    private final String label;

    private final String description;

    private Integer order = null;

    private final List<Property> properties;

    private final Map<String, CMSComponentAttribute> attributes;

    private final PersistentCMSComponent persistentComponent;

    /**
     * Used for sorting of component selection menu.
     */
    private final List<String> types;

    private transient UIComponent uiComponent;
    private transient UIComponent backendUiComponent;

    private CMSComponentScope scope = CMSComponentScope.PAGEVIEW;

    /**
     *
     * @param template the component to copy structure from
     * @param items content items to populate the new component with
     */
    public CMSComponent(CMSComponent template, List<CMSContentItem> items) {
        this(template.getJsfComponent(), template.getLabel(), template.getDescription(), template.types, template.getTemplateFilename(),
                template.getScope(), template.getAttributes(), template.getProperties(), template.getOrder(),
                Optional.ofNullable(template.getPersistentComponent()));
        List<CMSContentItem> newItems = items.stream().map(item -> new CMSContentItem(item, this)).toList();
        this.contentItems.addAll(newItems);
    }

    /**
     *
     * @param template the component to copy structure and configuration from
     * @param jpa optional persistentComponent to load persisted data from
     */
    public CMSComponent(CMSComponent template, Optional<PersistentCMSComponent> jpa) {
        this(template.getJsfComponent(), template.getLabel(), template.getDescription(), template.types, template.getTemplateFilename(),
                template.getScope(),
                CMSComponent.initializeAttributes(template.getAttributes(),
                        jpa.map(PersistentCMSComponent::getAttributes).orElse(Collections.emptyMap())),
                template.getProperties(),
                template.getOrder(),
                jpa);
        List<CMSContent> contentData = jpa.map(PersistentCMSComponent::getContentItems).orElse(Collections.emptyList());
        List<CMSContentItem> items =
                template.getContentItems().stream().map(item -> populateContentItem(item, contentData)).toList();
        this.contentItems.addAll(items);
    }

    /**
     *
     * Constructor to create Component from template file.
     *
     * @param jsfComponent the JSF component used to render this CMS component
     * @param label human-readable display label of the component
     * @param description short description of the component's purpose
     * @param types list of category type strings for the component selection menu
     * @param templateFilename filename of the component template
     * @param scope visibility scope of the component
     * @param attributes map of configurable component attributes
     * @param properties list of additional behaviour properties
     * @param order display order position of the component
     */
    public CMSComponent(JsfComponent jsfComponent, String label, String description, List<String> types, String templateFilename,
            CMSComponentScope scope, Map<String, CMSComponentAttribute> attributes, List<Property> properties, Integer order) {
        this(jsfComponent, label, description, types, templateFilename, scope, attributes, properties, order, Optional.empty());
    }

    /**
     *
     * @param jsfComponent the JSF component used to render this CMS component
     * @param label human-readable display label of the component
     * @param description short description of the component's purpose
     * @param types list of category type strings for the component selection menu
     * @param templateFilename filename of the component template
     * @param scope visibility scope of the component
     * @param attributes map of configurable component attributes
     * @param properties list of component properties
     * @param order display order position of the component
     * @param jpa optional persistentComponent to load persisted data from
     */
    private CMSComponent(JsfComponent jsfComponent, String label, String description, List<String> types, String templateFilename,
            CMSComponentScope scope, Map<String, CMSComponentAttribute> attributes, List<Property> properties, Integer order,
            Optional<PersistentCMSComponent> jpa) {
        this.jsfComponent = jsfComponent;
        this.label = label;
        this.description = description;
        this.types = types == null ? Collections.emptyList() : types;
        this.templateFilename = templateFilename;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
        this.properties = properties == null ? Collections.emptyList() : properties;
        this.scope = scope;
        this.persistentComponent = jpa.orElse(null);
        this.order = Optional.ofNullable(order).orElse(jpa.map(PersistentCMSComponent::getOrder).orElse(0));
    }

    
    public PersistentCMSComponent getPersistentComponent() {
        return persistentComponent;
    }

    /**
     *
     * @param publicationState the publication state to set on the persistentComponent
     */
    public void setPublicationState(ContentItemPublicationState publicationState) {
        Optional.ofNullable(persistentComponent).ifPresent(p -> p.setPublicationState(publicationState));
    }

    public ContentItemPublicationState getPublicationState() {
        return Optional.ofNullable(persistentComponent)
                .map(PersistentCMSComponent::getPublicationState)
                .orElse(ContentItemPublicationState.PUBLISHED);
    }

    /**
     *
     * @param order display order position of this component on the page
     */
    public void setOrder(int order) {
        Optional.ofNullable(persistentComponent).ifPresent(p -> p.setOrder(order));
        this.order = order;
    }

    public Integer getOrder() {
        return Optional.ofNullable(persistentComponent).map(PersistentCMSComponent::getOrder).orElse(this.order);
    }

    /**
     *
     * @param item the content item to add to this component
     * @return true if item added successfully; false otherwise
     */
    public boolean addContentItem(CMSContentItem item) {
        if (!this.contentItems.contains(item)) {
            return this.contentItems.add(item);
        }
        return false;
    }

    /**
     *
     * @param item the content item to remove from this component
     * @return true if item removed successfully; false otherwise
     */
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

        if (this.uiComponent == null && this.jsfComponent != null && this.jsfComponent.exists()) {
            DynamicContentBuilder builder = new DynamicContentBuilder();
            this.uiComponent = FacesContext.getCurrentInstance().getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            this.uiComponent.setId("cms_" + FilenameUtils.getBaseName(this.templateFilename) + "_" + Optional.ofNullable(this.order).orElse(0));
            Map<String, Object> attributes = new HashMap<>();
            for (CMSComponentAttribute attribute : this.getAttributes().values()) {
                attributes.put(attribute.getName(), attribute.isBooleanValue() ? attribute.getBooleanValue() : attribute.getValue());
            }
            attributes.put("component", this);
            UIComponent component = builder.build(this.getJsfComponent(), this.uiComponent, attributes);
        }
        return uiComponent;
    }

    /**
     *
     * @param uiComponent the frontend JSF UI component to set
     */
    public void setUiComponent(UIComponent uiComponent) {
        this.uiComponent = uiComponent;
    }

    public UIComponent getBackendUiComponent() throws PresentationException {
        if (this.backendUiComponent == null) {
            String id = FilenameUtils.getBaseName("component_" + this.getTemplateFilename()) + "_" + System.nanoTime();
            this.backendUiComponent = new HtmlPanelGroup();
            this.backendUiComponent.setId(id);
            int itemCount = 0;
            for (CMSContentItem cmsContentItem : contentItems) {
                UIComponent itemComponent = cmsContentItem.getUiComponent();
                itemComponent.setId(id + "_" + itemCount);
                this.backendUiComponent.getChildren().add(itemComponent);
                //itemComponent.setParent(this.backendUiComponent);
                ++itemCount;
            }
        }
        return backendUiComponent;
    }

    /**
     *
     * @param backendUiComponent the backend JSF UI component to set
     */
    public void setBackendUiComponent(UIComponent backendUiComponent) {
        this.backendUiComponent = backendUiComponent;
    }

    /**
     *
     * @param key attribute name to look up
     * @return {@link CMSComponentAttribute}
     */
    public CMSComponentAttribute getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     *
     * @param key attribute name to look up
     * @param defaultValue value returned when attribute is absent
     * @return a boolean
     */
    public boolean getBooleanAttributeValue(String key, boolean defaultValue) {
        return Optional.ofNullable(this.attributes).map(map -> map.get(key)).map(CMSComponentAttribute::getBooleanValue).orElse(defaultValue);
    }

    /**
     *
     * @param key attribute name to look up
     * @return {@link String}
     */
    public String getAttributeValue(String key) {
        return Optional.ofNullable(this.attributes).map(map -> map.get(key)).map(CMSComponentAttribute::getValue).orElse("");
    }

    /**
     *
     * @param key name of the attribute to set
     * @param value new string value to assign to the attribute
     */
    public void setAttribute(String key, String value) {
        CMSComponentAttribute attr = this.attributes.get(key);
        if (attr != null) {
            this.attributes.put(key, new CMSComponentAttribute(attr, value));
            Optional.ofNullable(persistentComponent).ifPresent(p -> p.setAttribute(key, value));
        } else {
            throw new IllegalArgumentException("Attempting to change the not configured attribute " + key);
        }
    }

    /**
     *
     * @param key name of the attribute to toggle
     * @param value value to set, or clear if already set
     */
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

    /**
     *
     * @param published true to publish the component; false to restrict to admins
     */
    public void setPublished(boolean published) {
        setPublicationState(published ? ContentItemPublicationState.PUBLISHED : ContentItemPublicationState.ADMINISTRATOR);
    }

    public boolean isPrivate() {
        return !isPublished();
    }

    /**
     *
     * @param privat true to restrict component visibility to admins
     */
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
     * {@link CMSContent#getItemId()} equals to the item's {@link CMSContentItem#getItemId()}.
     * 
     * @param item the template content item providing structure and ID
     * @param contentData list of persisted content objects to match against
     * @return {@link CMSContentItem}
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

        CMSContentItem newContentItem = new CMSContentItem(item, this);
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

    /**
     *
     * @param user the user whose access rights to check; may be null
     * @return a boolean
     */
    public boolean hasAccess(User user) {
        if (isPublished()) {
            return true;
        }
        return user != null && user.isCmsAdmin();
    }

    /**
     *
     * @param itemId ID of the content item to retrieve data for
     * @return {@link String}
     */
    public String getContentData(String itemId) {
        return getContentData(itemId, null, null);
    }

    /**
     *
     * @param itemId ID of the content item to retrieve data for
     * @param width optional width constraint passed to the content's getData method
     * @param height optional height constraint passed to the content's getData method
     * @return {@link String}
     */
    public String getContentData(String itemId, Integer width, Integer height) {
        return this.contentItems.stream()
                .filter(item -> Objects.equals(item.getItemId(), itemId))
                .findAny()
                .map(CMSContentItem::getContent)
                .map(c -> c.getData(width, height))
                .orElse("");
    }

    /**
     * Checks whether a contentItem with the given itemId exists and is not empty.
     * 
     * @param itemId ID of the content item to check
     * @return true if the contentItem with the given itemId exists and its {@link CMSContent#isEmpty()} method returns false
     */
    public boolean hasContent(String itemId) {
        return this.contentItems.stream().anyMatch(item -> Objects.equals(item.getItemId(), itemId) && !item.isEmpty());
    }

    public CMSContent getContent(String itemId) {
        return Optional.ofNullable(this.getContentItem(itemId)).map(CMSContentItem::getContent).orElse(null);
    }

    /**
     * Sets whether this component should be displayed when the owning page is embedded in another page, rather than on the owning page itself.
     *
     * @param preview true to set scope to preview; false for normal page view
     */
    public void setPreview(boolean preview) {
        this.scope = preview ? CMSComponentScope.PREVIEW : CMSComponentScope.PAGEVIEW;
    }

    /**
     * Returns whether this component should be displayed when the owning page is embedded in another page, rather than on the owning page itself.
     * 
     * @return true if scope is CMSComponentScope.PREVIEW; false otherwise
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
        return Optional.ofNullable(this.persistentComponent).map(PersistentCMSComponent::getId).orElse(0L);
    }

    public CMSPage getOwningPage() {
        return Optional.ofNullable(this.persistentComponent).map(PersistentCMSComponent::getOwningPage).orElse(null);
    }

    public boolean isPaged() {
        return this.contentItems.stream().map(CMSContentItem::getContent).anyMatch(PagedCMSContent.class::isInstance);
    }

    public List<String> getTypes() {
        if (this.types == null || this.types.isEmpty()) {
            return List.of("other");
        } else {
            return this.types;
        }
    }

    public List<Property> getProperties() {
        return properties;
    }

    public boolean hasProperty(Property property) {
        return this.properties.contains(property);
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

    /**
     * Additional properties that can be passed to the component to set certain behaviour.
     */
    public static enum Property {
        /**
         * Applicable to any search related content: Enforce faceting on page load.
         */
        FORCE_FACETING;

        public static Property getProperty(String s) {
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                return null;
            }
        }
    }

}
