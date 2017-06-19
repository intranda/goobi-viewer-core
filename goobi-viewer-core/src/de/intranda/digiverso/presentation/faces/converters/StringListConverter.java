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
package de.intranda.digiverso.presentation.faces.converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter("stringListConverter")
public class StringListConverter implements Converter {

    /* (non-Javadoc)
     * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
     */
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String input) {
        if (input.isEmpty() || input.equals("[]")) {
            return new ArrayList<String>();
        }
        input = input.substring(2, input.length() - 2);
        String[] tokens = input.split("(\",+\")+");
        return Arrays.asList(tokens);

    }

    /* (non-Javadoc)
     * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object input) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (input instanceof List) {
            for (Object token : (List) input) {
                sb.append("\"").append(token).append("\"");
                sb.append(",");
            }
            if (sb.toString().endsWith(",")) {
                sb.delete(sb.length() - 1, sb.length());
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
