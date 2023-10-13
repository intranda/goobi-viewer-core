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

import java.io.UnsupportedEncodingException;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Syntax validator for passwords.
 */
@FacesValidator("passwordValidator")
public class PasswordValidator implements Validator<String> {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(PasswordValidator.class);

    /* (non-Javadoc)
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        if (!validatePassword(value)) {
            FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("password_errInvalid", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     * <p>
     * validatePassword.
     * </p>
     *
     * @param password a {@link java.lang.String} object.
     * @should return true if password good
     * @should return false if password empty
     * @should return false if password blank
     * @should return false if password too short
     * @should return false if password too long
     * @return a boolean.
     */
    public static boolean validatePassword(String password) {
        /**
         * Let the cery existance of a password be validated by a required="true" condition on the input element. When changing other properties of an
         * existing user, it must be legal to keep the password input empty In this case, the password will not be updated anyway (see
         * AdminBean#saveUserAction(User user)
         */
        if (StringUtils.isEmpty(password)) {
            return true;
        }
        if (password.length() < 8) {
            return false;
        }

        // Limit to 72 Bytes
        try {
            byte[] utf16Bytes = password.getBytes("UTF-8");
            if (utf16Bytes.length > 72) {
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
            return false;
        }

        return true;
    }
}
