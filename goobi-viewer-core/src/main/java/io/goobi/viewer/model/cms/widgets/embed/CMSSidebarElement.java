/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.cms.widgets.embed;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.dao.converter.WidgetContentTypeConverter;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.misc.NumberIterator;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

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
public class CMSSidebarElement {

    public static final String WIDGET_TYPE_DEFAULT = "DEFAULT";
    public static final String WIDGET_TYPE_AUTOMATIC = "AUTOMATIC";
    public static final String WIDGET_TYPE_CUSTOM = "CUSTOM";

    private static final Logger logger = LoggerFactory.getLogger(CMSSidebarElement.class);
    /** Constant <code>HASH_MULTIPLIER=11</code> */
    protected static final int HASH_MULTIPLIER = 11;
    private static final NumberIterator ID_COUNTER = new NumberIterator();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_sidebar_element_id")
    private Long id;

    /** Reference to the owning <code>CMSPage</code>. */
    @ManyToOne
    @JoinColumn(name = "owner_page_id", nullable = false)
    private CMSPage ownerPage;

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
     * @param type
     */
    public CMSSidebarElement(WidgetContentType type) {
        this.generationType = WidgetContentType.getGenerationType(type);
        this.contentType = type;
    }

    /**
     * Default constructor for a certain type of widget and owning CMSPage
     * @param type
     */
    public CMSSidebarElement(WidgetContentType type, CMSPage owner) {
        this(type);
        this.ownerPage = owner;
    }

    /**
     * Clones the given sidebar element and assigns the given CMSPage as owner.
     * Depends on cloning constructors of subclasses
     * @param orig
     * @param owner
     * @return
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
     * the database id
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

    /**
     * The order in which the element is shown. Low numbers are displayed on top of the sidebar, high numbers at the bottom
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
     * @return
     */
    public TranslatedText getTitle() {
        return new TranslatedText(ViewerResourceBundle.getTranslations(getContentType().getLabel()));
    } 

}
