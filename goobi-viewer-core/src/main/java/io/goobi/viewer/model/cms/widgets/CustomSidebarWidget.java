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
package io.goobi.viewer.model.cms.widgets;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * Class to persist user generated CMS-Sidebar widgets in the database. Different types of widgets containing different data are encoded in
 * subclasses. This main class should be considered effectively abstract, even though it cannot be marked as abstract due to dao persistence
 * restrictions. The exact type of custom widget can be gathered from #{CustomSidebarWidget{@link #getType()}
 *
 * Each inheriting class must implement a cloning constructor, i.e. a constructor taking an argument of the same class and copying all its data
 *
 * @author florian
 *
 */
@Entity
@Table(name = "custom_sidebar_widgets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "widget_type")
@DiscriminatorValue("CustomSidebarWidget")
public class CustomSidebarWidget implements IPolyglott, Serializable {

    private static final long serialVersionUID = -7060014691745797150L;

    private static final Logger logger = LogManager.getLogger(CustomSidebarWidget.class);

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "widget_id")
    protected Long id;

    @Column(name = "widget_title", columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText title = new TranslatedText(IPolyglott.getLocalesStatic(), IPolyglott.getCurrentLocale());

    /**
     * Currently not in use
     */
    @Column(name = "widget_description", columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText description = new TranslatedText(IPolyglott.getLocalesStatic(), IPolyglott.getCurrentLocale());

    @Column(name = "style_class", nullable = true)
    private String styleClass = "";

    @Column(name = "collapsed")
    private boolean collapsed = false;

    /**
     * The currently selected locale when editing the widget. Any changes must be passed down to any translatable proberties of the widget, e.g. the
     * title
     */
    @Transient
    private Locale locale = IPolyglott.getCurrentLocale();

    public CustomSidebarWidget() {
    }

    /**
     * Cloning constructor.
     * 
     * @param source
     */
    public CustomSidebarWidget(CustomSidebarWidget source) {
        this.id = source.id;
        this.title = new TranslatedText(source.title);
        this.title.setSelectedLocale(getSelectedLocale());
        this.description = new TranslatedText(source.description);
        this.description.setSelectedLocale(getSelectedLocale());
        this.collapsed = source.collapsed;
        this.styleClass = source.styleClass;

    }

    @Override
    public boolean isComplete(Locale locale) {
        return title.isComplete(locale);
    }

    @Override
    public boolean isValid(Locale locale) {
        return title.isComplete(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return title.isEmpty(locale);
    }

    @Override
    public Locale getSelectedLocale() {
        return this.locale;
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.locale = locale;
        this.description.setSelectedLocale(locale);
        this.title.setSelectedLocale(locale);
    }

    /**
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
     * @return the title
     */
    public TranslatedText getTitle() {
        return title;
    }

    public void setTitle(TranslatedText title) {
        this.title = title;
    }

    /**
     * @return the description
     */
    public TranslatedText getDescription() {
        return description;
    }

    /**
     * Return the type of this custom sidebar widget. Must be implemented by subclasses of {@link CustomSidebarWidget}
     * 
     * @return {@link CustomWidgetType}
     */
    public CustomWidgetType getType() {
        return null;
    }

    /**
     *
     * @return the css style class to use for this widget, if any
     */
    public String getStyleClass() {
        return styleClass;
    }

    /**
     * Set the css style class to use for this widget
     * 
     * @param styleClass
     */
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    /**
     * @return true if the widget should initially be displayed in a collapsed state with a button to expand it
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * Set this widget to be displayed as a collapseable
     * 
     * @param collapsed the collapsed to set
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /**
     * Creates a copy of the given custom widget o. Depends on cloning constructors if sublass
     *
     * @param o
     * @return {@link CustomSidebarWidget}
     */
    public static CustomSidebarWidget clone(CustomSidebarWidget o) {
        if (o == null) {
            return null;
        }
        try {
            return o.getClass().getConstructor(o.getClass()).newInstance(o);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            logger.error("Cannot instatiate CustomSidebarWidget of class {}. Reason: {}", o.getClass(), e.toString());
            return null;
        }
    }

}
