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
package io.goobi.viewer.model.cms.widgets;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * Class for displaying available widgets in GUI
 * 
 * @author florian
 *
 */
public class WidgetDisplayElement implements IPolyglott, Comparable<WidgetDisplayElement>{
        
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
     * @param title
     * @param description
     * @param embeddingPages
     * @param generationType
     * @param contentType
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
    /**
     * @return the title
     */
    public TranslatedText getTitle() {
        return title;
    }
    /**
     * @return the description
     */
    public TranslatedText getDescription() {
        return description;
    }
    /**
     * @return the embeddingPages
     */
    public List<CMSPage> getEmbeddingPages() {
        return embeddingPages;
    }
    /**
     * @return the generationType
     */
    public WidgetGenerationType getGenerationType() {
        return generationType;
    }
    /**
     * @return the contentType
     */
    public WidgetContentType getContentType() {
        return contentType;
    }
    
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
    @Override
    public void setSelectedLocale(Locale locale) {
        //Do nothing
    }
    
    public boolean hasTranslations() {
        return this.translations != null;
    }
    
    public IPolyglott getTranslations() {
        return this.translations;
    }
    
    @Override
    public String toString() {
        return getTitle().getText();
    }

    @Override
    public int compareTo(WidgetDisplayElement other) {
        return StringUtils.compare(this.getTitle().getText(), other.getTitle().getText());
    }

}
