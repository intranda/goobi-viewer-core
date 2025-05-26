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

import java.util.Locale;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A subtype of {@link CustomSidebarWidget} to display a html text in different languages
 * 
 * @author florian
 *
 */
@Entity
@DiscriminatorValue("HtmlSidebarWidget")
public class HtmlSidebarWidget extends CustomSidebarWidget {

    private static final long serialVersionUID = 3921353218873876880L;

    @Column(name = "html_text", columnDefinition = "LONGTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText htmlText = new TranslatedText(IPolyglott.getLocalesStatic());

    /**
     * Empty default constructor
     */
    public HtmlSidebarWidget() {

    }

    /**
     * Cloning constructor
     * 
     * @param o
     */
    public HtmlSidebarWidget(HtmlSidebarWidget o) {
        super(o);
        this.htmlText = new TranslatedText(o.htmlText);
        this.htmlText.setSelectedLocale(getSelectedLocale());
    }

    /**
     *
     * @return the html text
     */
    public TranslatedText getHtmlText() {
        return htmlText;
    }

    @Override
    public boolean isComplete(Locale locale) {
        return super.isComplete(locale) && htmlText.isComplete(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return super.isEmpty(locale) && htmlText.isEmpty(locale);
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        super.setSelectedLocale(locale);
        this.htmlText.setSelectedLocale(locale);
    }

    @Override
    public CustomWidgetType getType() {
        return CustomWidgetType.WIDGET_HTML;
    }

    public boolean isHasShortDescription() {
        return !this.getDescription().isEmpty() && !this.getHtmlText().isEmpty();
    }

    public IMetadataValue getShortDescription(int maxLength) {
        if (!this.getDescription().isEmpty()) {
            return getDescription().copy().transformValues(s -> StringTools.truncateText(s, maxLength));
        } else if (!this.getHtmlText().isEmpty()) {
            return getHtmlText().copy().transformValues(s -> StringTools.truncateText(s, maxLength));
        } else {
            return new SimpleMetadataValue("");
        }
    }

}
