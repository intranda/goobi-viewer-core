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
package io.goobi.viewer.model.cms.widgets.embed;

import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.dao.converter.WidgetContentTypeConverter;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * <p>
 * CMSSidebarElement class.
 * </p>
 * Wrapper to link sidebar widgets to cms-pages. Has a subclass for each type of sidebar widget
 */
@Entity
@Table(name = "cms_page_sidebar_elements")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "generation_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("BASE")
public class CMSSidebarElement implements Serializable {

    private static final long serialVersionUID = -7230299208726799480L;

    private static final Logger logger = LogManager.getLogger(CMSSidebarElement.class);

    public static final String WIDGET_TYPE_DEFAULT = "DEFAULT";
    public static final String WIDGET_TYPE_AUTOMATIC = "AUTOMATIC";
    public static final String WIDGET_TYPE_CUSTOM = "CUSTOM";

    /** Constant <code>HASH_MULTIPLIER=11</code> */
    protected static final int HASH_MULTIPLIER = 11;
    // private static final NumberIterator ID_COUNTER = new NumberIterator();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_sidebar_element_id")
    private Long id;

    /** Reference to the owning <code>CMSPage</code>. */
    @ManyToOne
    @JoinColumn(name = "owner_page_id", nullable = true)
    private CMSPage ownerPage;

    /** Reference to the owning <code>CMSPageTemplate</code>. */
    @ManyToOne
    @JoinColumn(name = "owner_template_id", nullable = true)
    private CMSPageTemplate ownerTemplate;

    @Column(name = "sort_order")
    private int order;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false)
    private WidgetGenerationType generationType;

    @Convert(converter = WidgetContentTypeConverter.class)
    @Column(name = "content_type", nullable = false)
    private WidgetContentType contentType;

    /**
     * Empty constructor for the DAO
     */
    public CMSSidebarElement() {

    }

    /**
     * Default constructor for a certain type of widget
     * 
     * @param type
     */
    public CMSSidebarElement(WidgetContentType type) {
        this.generationType = WidgetContentType.getGenerationType(type);
        this.contentType = type;
    }

    /**
     * Default constructor for a certain type of widget
     * 
     * @param type
     * @param owner
     */
    public CMSSidebarElement(WidgetContentType type, CMSPage owner) {
        this(type);
        this.ownerPage = owner;
    }

    /**
     * Default constructor for a certain type of widget
     * 
     * @param type
     * @param owner
     */
    public CMSSidebarElement(WidgetContentType type, CMSPageTemplate owner) {
        this(type);
        this.ownerTemplate = owner;
    }

    /**
     * Default constructor for a certain type of widget and owning CMSPage
     * 
     * @param orig
     * @param owner
     */
    public CMSSidebarElement(CMSSidebarElement orig, CMSPage owner) {
        this(orig.contentType);
        this.id = orig.getId();
        this.order = orig.getOrder();
        this.ownerPage = owner;
    }

    /**
     * Default constructor for a certain type of widget and owning CMSPageTemplate
     * 
     * @param orig
     * @param owner
     */
    public CMSSidebarElement(CMSSidebarElement orig, CMSPageTemplate owner) {
        this(orig.contentType);
        this.id = orig.getId();
        this.order = orig.getOrder();
        this.ownerTemplate = owner;
    }

    /**
     * Clones the given sidebar element and assigns the given CMSPage as owner. Depends on cloning constructors of subclasses
     * 
     * @param orig
     * @param owner
     * @return {@link CMSSidebarElement}
     */
    public static CMSSidebarElement copy(CMSSidebarElement orig, CMSPage owner) {
        switch (orig.getClass().getSimpleName()) {
            case "CMSSidebarElementDefault":
                return new CMSSidebarElementDefault((CMSSidebarElementDefault) orig, owner);
            case "CMSSidebarElementAutomatic":
                return new CMSSidebarElementAutomatic((CMSSidebarElementAutomatic) orig, owner);
            case "CMSSidebarElementCustom":
                return new CMSSidebarElementCustom((CMSSidebarElementCustom) orig, owner);
            default:
                throw new IllegalArgumentException("Copying of sidebar element type " + orig.getClass() + " not implemented");
        }
    }

    /**
     * Clones the given sidebar element and assigns the given CMSPage as owner. Depends on cloning constructors of subclasses
     * 
     * @param orig
     * @param owner
     * @return {@link CMSSidebarElement}
     */
    public static CMSSidebarElement copy(CMSSidebarElement orig, CMSPageTemplate owner) {
        switch (orig.getClass().getSimpleName()) {
            case "CMSSidebarElementDefault":
                return new CMSSidebarElementDefault((CMSSidebarElementDefault) orig, owner);
            case "CMSSidebarElementAutomatic":
                return new CMSSidebarElementAutomatic((CMSSidebarElementAutomatic) orig, owner);
            case "CMSSidebarElementCustom":
                return new CMSSidebarElementCustom((CMSSidebarElementCustom) orig, owner);
            default:
                throw new IllegalArgumentException("Copying of sidebar element type " + orig.getClass() + " not implemented");
        }
    }

    /**
     * the database id
     * 
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * the {@link CMSPage} containing the element
     * 
     * @return the ownerPage
     */
    public CMSPage getOwnerPage() {
        return ownerPage;
    }

    /**
     * @param ownerPage the ownerPage to set
     */
    public void setOwnerPage(CMSPage ownerPage) {
        this.ownerPage = ownerPage;
    }

    public CMSPageTemplate getOwnerTemplate() {
        return ownerTemplate;
    }

    public void setOwnerTemplate(CMSPageTemplate ownerTemplate) {
        this.ownerTemplate = ownerTemplate;
    }

    /**
     * The order in which the element is shown. Low numbers are displayed on top of the sidebar, high numbers at the bottom
     * 
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * the {@link WidgetGenerationType} of the underlying widget
     * 
     * @return the generationType
     */
    public WidgetGenerationType getGenerationType() {
        return generationType;
    }

    /**
     * @param generationType the generationType to set
     */
    public void setGenerationType(WidgetGenerationType generationType) {
        this.generationType = generationType;
    }

    /**
     * the {@link WidgetContentType} of the underlying widget
     * 
     * @return the contentType
     */
    public WidgetContentType getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(WidgetContentType contentType) {
        this.contentType = contentType;
    }

    /**
     * The title displayed for this element when editing the owning CMSPage
     * 
     * @return {@link TranslatedText}
     */
    public TranslatedText getTitle() {
        return new TranslatedText(ViewerResourceBundle.getTranslations(getContentType().getLabel()));
    }

    public boolean canEdit() {
        return false;
    }

    public String getAdminBackendUrl() {
        return "";
    }

}
