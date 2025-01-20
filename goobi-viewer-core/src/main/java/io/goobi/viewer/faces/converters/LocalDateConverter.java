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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import org.apache.commons.lang3.StringUtils;

/**
 * @author florian
 *
 */
@FacesConverter("localDateConverter")
public class LocalDateConverter implements Converter<LocalDate> {

    private static final String ATTRIBUTE_DATA_FORMAT = "data-format";

    /* (non-Javadoc)
     * @see jakarta.faces.convert.Converter#getAsObject(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.String)
     */
    @Override
    public LocalDate getAsObject(FacesContext context, UIComponent component, String value) {
        if (StringUtils.isNotBlank(value)) {
            if (component != null && component.getAttributes().get(ATTRIBUTE_DATA_FORMAT) != null) {
                String format = (String) component.getAttributes().get(ATTRIBUTE_DATA_FORMAT);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(value, dateTimeFormatter);
            }
            return LocalDate.parse(value);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.faces.convert.Converter#getAsString(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)
     */
    @Override
    public String getAsString(FacesContext context, UIComponent component, LocalDate value) {
        if (value != null) {
            if (component != null && component.getAttributes().get(ATTRIBUTE_DATA_FORMAT) != null) {
                String format = (String) component.getAttributes().get(ATTRIBUTE_DATA_FORMAT);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
                return value.format(dateTimeFormatter);
            }
            return value.toString();
        }

        return null;
    }

}
