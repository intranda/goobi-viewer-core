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
package io.goobi.viewer.faces.converters;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.translations.language.Language;

/**
 * @author florian
 *
 */
@FacesConverter("languageConverter")
public class LanguageConverter implements Converter<Language> {

    /* (non-Javadoc)
     * @see jakarta.faces.convert.Converter#getAsObject(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.String)
     */
    @Override
    public Language getAsObject(FacesContext context, UIComponent component, String value) {
        return DataManager.getInstance().getLanguageHelper().getLanguage(value);
    }

    /* (non-Javadoc)
     * @see jakarta.faces.convert.Converter#getAsString(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)
     */
    @Override
    public String getAsString(FacesContext context, UIComponent component, Language value) {
        return value.getIsoCode();
    }

}
