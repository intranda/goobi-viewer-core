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
package io.goobi.viewer.faces.validators;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * <p>
 * URISyntaxValidator class.
 * </p>
 *
 * @author Florian Alpers
 */
@FacesValidator("URISyntaxValidator")
public class URISyntaxValidator implements Validator<String> {

    /* (non-Javadoc)
     * @see jakarta.faces.validator.Validator#validate(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        if (getAsBoolean(component.getAttributes().get("validator_active"))) {
            try {
                URI uri = new URI(value);
                Object requireAbsoluteURI = component.getAttributes().get("validator_requireAbsoluteURI");
                if (getAsBoolean(requireAbsoluteURI) && !uri.isAbsolute()) {
                    throw new URISyntaxException(value, "URI is not absolute");
                }
            } catch (URISyntaxException e) {
                FacesMessage message = new FacesMessage(ViewerResourceBundle.getTranslation("error_invalid_URI", null).replace("{0}", value), "");
                message.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(message);
            }
        }
    }

    private static boolean getAsBoolean(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else {
            return false;
        }
    }

}
