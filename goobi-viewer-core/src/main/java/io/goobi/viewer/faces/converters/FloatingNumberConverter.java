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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * FloatingNumberConverter class.
 * </p>
 *
 * @author Florian Alpers
 */
@FacesConverter("floatingNumberConverter")
public class FloatingNumberConverter implements Converter {

    private static final String REPLACEMENT_REGEX = "[^0-9.+-]";

    /** {@inheritDoc} */
    @Override
    public Object getAsObject(final FacesContext context, final UIComponent component, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        value = value.replace(",", ".");
        value = value.replaceAll(REPLACEMENT_REGEX, "");
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getAsString(final FacesContext context, final UIComponent component, final Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof String) {
            return (String) object;
        }
        if (object instanceof Number) {
            return Double.toString(((Number) object).doubleValue());
        }
        return "";
    }

}
