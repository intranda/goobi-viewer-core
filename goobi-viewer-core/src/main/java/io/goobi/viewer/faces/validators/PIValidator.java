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

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Syntax validator for passwords addresses.
 */
@FacesValidator("piValidator")
public class PIValidator implements Validator<String> {

    /** Constant <code>ILLEGAL_CHARS</code> */
    protected static final char[] ILLEGAL_CHARS = { '!', '?', '/', '\\', ':', ';', '(', ')', '@', '"', '\'' };

    /* (non-Javadoc)
     * @see jakarta.faces.validator.Validator#validate(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        if (!validatePi(value)) {
            FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("pi_errInvalid", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     * <p>
     * validatePi.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @should return true if pi good
     * @should return false if pi empty, blank or null
     * @should return false if pi contains illegal characters
     * @return a boolean.
     */
    public static boolean validatePi(String pi) {
        if (StringUtils.isBlank(pi)) {
            return false;
        }

        return !StringUtils.containsAny(pi, ILLEGAL_CHARS);
    }
}
