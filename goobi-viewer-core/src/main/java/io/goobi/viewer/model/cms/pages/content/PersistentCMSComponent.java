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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.persistence.annotations.PrivateOwned;

import io.goobi.viewer.dao.converter.StringListConverter;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.translations.IPolyglott;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "cms_components")
public class PersistentCMSComponent implements IPolyglott {

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "component_id")
    private Long id;
    
    @Column(name = "css_classes")
    @Convert(converter = StringListConverter.class)
    private final List<String> cssClasses = new ArrayList<>();
    
    @OneToMany(mappedBy = "owningComponent", fetch = FetchType.EAGER, cascade = { CascadeType.ALL, CascadeType.REMOVE })
    @PrivateOwned
    private final List<CMSContent> contentItems = new ArrayList<>();
    
    @Column(name="publication_state")
    @Enumerated(EnumType.STRING)
    private ContentItemPublicationState publicationState = ContentItemPublicationState.ADMINISTRATOR;
    
    @Column(name="template_filename")
    private String templateFilename;
    
    @Column(name="component_order")
    private Integer order = 0;
    
    /** Reference to the owning <code>CMSPage</code>. */
    @ManyToOne
    @JoinColumn(name = "owner_page_id")
    private CMSPage ownerPage;
    
    /** Reference to the owning <code>CMSPage</code>. */
    @ManyToOne
    @JoinColumn(name = "owner_template_id")
    private CMSPageTemplate ownerTemplate;
    
    /**
     * If the content of this component is spread out over several pages of views, as in search result lists for example, 
     * this number indicates the current page the user is seeing
     */
    @Transient
    private int listPage = 1;
    
    /**
     * JPA contrutor
     */
    public PersistentCMSComponent() {
    }
    
    public PersistentCMSComponent(CMSComponent component) {
        this.id = component.getPersistenceId();
        this.cssClasses.addAll(component.getCssClasses());
        this.order = component.getOrder();
        this.publicationState = component.getPublicationState();
        this.templateFilename = component.getTemplateFilename();
        this.contentItems.addAll(component.getContentItems().stream().map(CMSContentItem::getContent).map(CMSContent::copy).collect(Collectors.toList()));
        this.contentItems.forEach(c -> c.setOwningComponent(this));
    }
    
    public PersistentCMSComponent(PersistentCMSComponent orig) {
        this.id = orig.id;
        this.cssClasses.addAll(orig.getCssClasses());
        this.order = orig.getOrder();
        this.publicationState = orig.publicationState;
        this.templateFilename = orig.templateFilename;
        this.ownerPage = orig.ownerPage;
        this.ownerTemplate = orig.ownerTemplate;
        this.contentItems.addAll(orig.getContentItems().stream().map(CMSContent::copy).collect(Collectors.toList()));
        this.contentItems.forEach(c -> c.setOwningComponent(this));
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
     * @return the cssClasses
     */
    public List<String> getCssClasses() {
        return cssClasses;
    }

    /**
     * @return the contentItems
     */
    public List<CMSContent> getContentItems() {
        return contentItems;
    }
    
    public CMSPage getOwnerPage() {
        return ownerPage;
    }
    
    public void setOwnerPage(CMSPage ownerPage) {
        this.ownerPage = ownerPage;
    }
    
    public void setOwnerTemplate(CMSPageTemplate template) {
        this.ownerTemplate = template;
    }

    public CMSPageTemplate getOwnerTemplate() {
        return ownerTemplate;
    }
    
    public String getTemplateFilename() {
        return templateFilename;
    }
    
    public void setTemplateFilename(String templateFilename) {
        this.templateFilename = templateFilename;
    }

    @Override
    public boolean isComplete(Locale locale) {
        for (TranslatableCMSContent cmsContent : getTranslatableContentItems()) {
            if(!cmsContent.isComplete(locale)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isValid(Locale locale) {
        for (TranslatableCMSContent cmsContent : getTranslatableContentItems()) {
            if(!cmsContent.isValid(locale)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty(Locale locale) {
        for (TranslatableCMSContent cmsContent : getTranslatableContentItems()) {
            if(!cmsContent.isEmpty(locale)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Locale getSelectedLocale() {
        if(getTranslatableContentItems().isEmpty()) {
            return BeanUtils.getDefaultLocale();
        } else {
            return getTranslatableContentItems().get(0).getSelectedLocale();
        }
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        for (TranslatableCMSContent cmsContent : getTranslatableContentItems()) {
            cmsContent.setSelectedLocale(locale);
        }
    }

    /**
     * set the {@link #listPage}
     * @param listPage
     */
    public void setListPage(int listPage) {
        this.listPage = listPage;
    }
    
    /**
     * get the {@link #listPage}
     * @return
     */
    public int getListPage() {
        return listPage;
    }
    
    public List<TranslatableCMSContent> getTranslatableContentItems() {
        return this.contentItems.stream()
                .filter(CMSContent::isTranslatable)
                .map(TranslatableCMSContent.class::cast)
                .collect(Collectors.toList());
    }
}
