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
package de.intranda.digiverso.presentation.faces.validators;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;

import de.intranda.digiverso.presentation.controller.Helper;

/**
 * Syntax validator for passwords addresses.
 */
@FacesValidator("passwordValidator")
public class PasswordValidator implements Validator {

    /* (non-Javadoc)
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!validatePassword((String) value)) {
            FacesMessage msg = new FacesMessage(Helper.getTranslation("password_errInvalid", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     *
     * @param password
     * @return
     * @should return true if password good
     * @should return false if password empty
     * @should return false if password blank
     * @should return false if password too short
     */
    public static boolean validatePassword(String password) {
        if (StringUtils.isBlank(password)) {
            return false;
        }
        if (password.length() < 8) {
            return false;
        }

        return true;
    }
}
