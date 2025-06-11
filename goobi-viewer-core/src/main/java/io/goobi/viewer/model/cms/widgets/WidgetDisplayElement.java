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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.managedbeans.CMSSidebarWidgetsBean;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.GenerationType;

/**
 * Class for displaying information about available sidebar widgets in /admin/cms/widgets and the sidebar edit tab of /admin/cms/pages/edit. Widgets
 * are distinguished by {@link #getGenerationType()} into default widgets with static GUI, automatic widgets provided by other custom content and
 * custom widgets explicitly created by users They are further distinguished by {@link #getContentType()} corresponding to the individual widget xhtml
 * component
 *
 * @author florian
 *
 */
public class WidgetDisplayElement implements IPolyglott, Comparable<WidgetDisplayElement> {

    private final TranslatedText title;
    private final TranslatedText description;
    private final List<CMSPage> embeddingPages;
    private final WidgetGenerationType generationType;
    private final WidgetContentType contentType;
    /**
     * Identifier of the underlying CustomSidebarWidget or GeoMap, if any
     */
    private final Long id;
    private final IPolyglott translations;

    /**
     * Default constructor for widgets without underlying data (i.e. default widgets)
     *
     * @param title
     * @param description
     * @param embeddingPages
     * @param generationType
     * @param contentType
     */
    public WidgetDisplayElement(IMetadataValue title, IMetadataValue description, List<CMSPage> embeddingPages, WidgetGenerationType generationType,
            WidgetContentType contentType) {
        this(title, description, embeddingPages, generationType, contentType, null, null);
    }

    /**
     *
     * Default constructor for widgets with underlying data identified by the given id
     *
     * @param title
     * @param description
     * @param embeddingPages
     * @param generationType
     * @param contentType
     * @param id the database id of the underlying content. The type of content depends on generationType and contentType
     * @param translations used to display translation status of the widget. Usually the underlying custom widget
     */
    public WidgetDisplayElement(IMetadataValue title, IMetadataValue description, List<CMSPage> embeddingPages, WidgetGenerationType generationType,
            WidgetContentType contentType, Long id, IPolyglott translations) {
        super();
        this.title = new TranslatedText(title, getSelectedLocale());
        this.description = new TranslatedText(description, getSelectedLocale());
        this.embeddingPages = embeddingPages;
        this.generationType = generationType;
        this.contentType = contentType;
        this.id = id;
        this.translations = translations;
    }

    public WidgetDisplayElement(CustomSidebarWidget widget) {
        this(widget, Collections.emptyList());
    }

    public WidgetDisplayElement(CustomSidebarWidget widget, List<CMSPage> embeddedPages) {
        this(
                widget.getTitle(),
                widget.getShortDescription(CMSSidebarWidgetsBean.MAX_DESCRIPTION_LENGTH),
                embeddedPages,
                WidgetGenerationType.CUSTOM,
                widget.getType(), widget.getId(), CustomWidgetType.WIDGET_FIELDFACETS.equals(widget.getType()) ? null : widget);
    }

    /**
     * The displayed title of the element
     * 
     * @return the title
     */
    public TranslatedText getTitle() {
        return title;
    }

    /**
     * A description of the element
     * 
     * @return the description
     */
    public TranslatedText getDescription() {
        return description;
    }

    public TranslatedText getDescriptionOrTypeDescription() {
        if (getDescription().isEmpty()) {
            return new TranslatedText(ViewerResourceBundle.getTranslations(getContentType().getDescription()));
        } else {
            return getDescription();
        }
    }

    /**
     * A list of CMS pages using this element. Only used to automatic and custom widgets
     * 
     * @return the embeddingPages
     */
    public List<CMSPage> getEmbeddingPages() {
        return embeddingPages;
    }

    /**
     * Describes they way in which way data for this widget is created and stored
     * 
     * @return the {@link GenerationType generation type}
     */
    public WidgetGenerationType getGenerationType() {
        return generationType;
    }

    /**
     * Describes the specific xhtml component used for this widget
     * 
     * @return the contentType
     */
    public WidgetContentType getContentType() {
        return contentType;
    }

    /**
     * Identifier of the underlying data, if any
     * 
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Both title and description are filled
     */
    @Override
    public boolean isComplete(Locale locale) {
        return !this.title.isEmpty(locale) && !this.description.isEmpty(locale);
    }

    /**
     * At least one of title and description is filled
     */
    @Override
    public boolean isValid(Locale locale) {
        return !this.title.isEmpty();
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return this.title.isEmpty(locale);
    }

    @Override
    public Locale getSelectedLocale() {
        return IPolyglott.getCurrentLocale();
    }

    /**
     * Not used, since this element isn't editable
     */
    @Override
    public void setSelectedLocale(Locale locale) {
        //Do nothing
    }

    /**
     *
     * @return true if an object exists providing the translation status of the widget
     */
    public boolean hasTranslations() {
        return this.translations != null;
    }

    /**
     *
     * @return the object providing the translation status of the widget
     */
    public IPolyglott getTranslations() {
        return this.translations;
    }

    /**
     * @return the text of the {@link #getTitle() title} in the current faces context language
     */
    @Override
    public String toString() {
        return getTitle().getText();
    }

    /**
     * Two elements are equal if their titles are equal
     */
    @Override
    public int compareTo(WidgetDisplayElement other) {
        int typeCompare = Integer.compare(this.generationType.ordinal(), other.generationType.ordinal());
        if (typeCompare == 0) {
            return StringUtils.compare(this.getTitle().getText(), other.getTitle().getText());
        }
        return typeCompare;
    }

    public String getAdminBackendUrl() {
        if (this.getId() != null) {
            if (this.contentType.equals(AutomaticWidgetType.WIDGET_CMSGEOMAP)) {
                return PrettyUrlTools.getAbsolutePageUrl("adminCmsGeoMapEdit", this.getId());
            } else {
                return PrettyUrlTools.getAbsolutePageUrl("adminCmsWidgetsEdit", this.getId());
            }
        } else {
            return PrettyUrlTools.getAbsolutePageUrl("adminCmsWidgetsAdd");
        }
    }

}
