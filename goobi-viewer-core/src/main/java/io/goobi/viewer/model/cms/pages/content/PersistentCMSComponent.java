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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.PrivateOwned;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.translations.IPolyglott;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_components")
public class PersistentCMSComponent implements IPolyglott, Serializable, Comparable<PersistentCMSComponent> {

    private static final long serialVersionUID = 9073881774408347387L;

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "component_id")
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owningComponent")
    @PrivateOwned
    @CascadeOnDelete
    private final List<CMSContent> contentItems = new ArrayList<>();

    @Column(name = "publication_state")
    @Enumerated(EnumType.STRING)
    private ContentItemPublicationState publicationState = ContentItemPublicationState.ADMINISTRATOR;

    @Column(name = "template_filename")
    private String templateFilename;

    @Column(name = "component_order")
    private Integer order = 0;

    /** Reference to the owning <code>CMSPage</code>. */
    @ManyToOne
    @JoinColumn(name = "owning_page_id", nullable = true)
    private CMSPage owningPage;

    /** Reference to the owning <code>CMSPageTemplate</code>. */
    @ManyToOne
    @JoinColumn(name = "owning_template_id", nullable = true)
    private CMSPageTemplate owningTemplate;

    @ElementCollection
    @JoinTable(name = "cms_component_attribute_map", joinColumns = @JoinColumn(name = "attribute_id"))
    @MapKeyColumn(name = "component_id")
    @Column(name = "cms_component_attributes")
    private Map<String, String> attributes = new HashMap<>();

    /**
     * JPA constructor.
     */
    public PersistentCMSComponent() {
    }

    public PersistentCMSComponent(CMSComponent component) {
        this.order = component.getOrder();
        this.publicationState = component.getPublicationState();
        this.templateFilename = component.getTemplateFilename();
        this.contentItems
                .addAll(component.getContentItems().stream().map(CMSContentItem::getContent).map(CMSContent::copy).collect(Collectors.toList()));
        this.contentItems.forEach(c -> c.setOwningComponent(this));
    }

    public PersistentCMSComponent(CMSComponent template, Collection<CMSContent> contentData) {
        this.order = template.getOrder();
        this.publicationState = template.getPublicationState();
        this.templateFilename = template.getTemplateFilename();
        this.contentItems.addAll(contentData.stream().map(CMSContent::copy).collect(Collectors.toList()));
        this.contentItems.forEach(c -> c.setOwningComponent(this));
    }

    public PersistentCMSComponent(PersistentCMSComponent orig) {
        this.id = orig.id;
        this.order = orig.getOrder();
        this.publicationState = orig.publicationState;
        this.templateFilename = orig.templateFilename;
        this.owningPage = orig.owningPage;
        this.owningTemplate = orig.owningTemplate;
        this.contentItems.addAll(orig.getContentItems().stream().map(CMSContent::copy).collect(Collectors.toList()));
        this.contentItems.forEach(c -> c.setOwningComponent(this));
        this.attributes = new HashMap<>(orig.attributes);

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the publicationState
     */
    public ContentItemPublicationState getPublicationState() {
        return publicationState;
    }

    /**
     * @param publicationState the publicationState to set
     */
    public void setPublicationState(ContentItemPublicationState publicationState) {
        this.publicationState = publicationState;
    }

    /**
     * @return the order
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * @return the contentItems
     */
    public List<CMSContent> getContentItems() {
        return contentItems;
    }

    public CMSPage getOwningPage() {
        return owningPage;
    }

    public void setOwningPage(CMSPage ownerPage) {
        this.owningPage = ownerPage;
    }

    public void setOwningTemplate(CMSPageTemplate template) {
        this.owningTemplate = template;
    }

    public CMSPageTemplate getOwningTemplate() {
        return owningTemplate;
    }

    public String getTemplateFilename() {
        return templateFilename;
    }

    public void setTemplateFilename(String templateFilename) {
        this.templateFilename = templateFilename;
    }

    public void addContent(CMSContent content) {
        this.contentItems.add(content);
        content.setOwningComponent(this);
    }
    
    @Override
    public boolean isComplete(Locale locale) {
        for (TranslatableCMSContent cmsContent : getTranslatableContentItems()) {
            if (!cmsContent.isComplete(locale)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isValid(Locale locale) {
        for (TranslatableCMSContent cmsContent : getTranslatableContentItems()) {
            if (!cmsContent.isValid(locale)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty(Locale locale) {
        for (TranslatableCMSContent cmsContent : getTranslatableContentItems()) {
            if (!cmsContent.isEmpty(locale)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Locale getSelectedLocale() {
        if (getTranslatableContentItems().isEmpty()) {
            return BeanUtils.getDefaultLocale();
        }
        return getTranslatableContentItems().get(0).getSelectedLocale();
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        for (TranslatableCMSContent cmsContent : getTranslatableContentItems()) {
            cmsContent.setSelectedLocale(locale);
        }
    }

    public List<TranslatableCMSContent> getTranslatableContentItems() {
        return this.contentItems.stream()
                .filter(CMSContent::isTranslatable)
                .map(TranslatableCMSContent.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public int compareTo(PersistentCMSComponent o) {
        return Integer.compare(this.order, o.order);
    }

    public void setAttribute(String key, String value) {
        if (StringUtils.isBlank(value)) {
            this.attributes.remove(key);
        } else {
            this.attributes.put(key, value);
        }
    }

    public String getAttribute(String key) {
        return this.attributes.get(key);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Optional<CMSContent> getContentByItemId(String itemId) {
        return this.contentItems.stream().filter(c -> c.getItemId().equals(itemId)).findAny();
    }

    @SuppressWarnings("unchecked")
    public <T extends CMSContent> T getFirstContentOfType(Class<? extends CMSContent> clazz) {
        return (T) this.contentItems.stream()
                .filter(c -> c.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends CMSContent> List<T> getAllContentOfType(Class<? extends CMSContent> clazz) {
        return (List<T>) this.contentItems.stream()
                .filter(c -> c.getClass().equals(clazz))
                .collect(Collectors.toList());
    }

    public boolean isPaged() {
        return getContentItems().stream().anyMatch(PagedCMSContent.class::isInstance);
    }
    
    @Override
    public int hashCode() {
        return Optional.ofNullable(this.id).map(Long::intValue).orElse(-1);
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass().equals(this.getClass())) {
            PersistentCMSComponent other = (PersistentCMSComponent)obj;
            return this.hashCode() == other.hashCode() &&
                    Objects.equals(this.order, other.order);
        } else {
            return false;
        }
    }
}
