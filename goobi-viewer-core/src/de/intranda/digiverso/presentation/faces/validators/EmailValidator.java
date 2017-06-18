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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import de.intranda.digiverso.presentation.controller.Helper;

/**
 * Syntax validator for e-mail addresses.
 */
@FacesValidator("emailValidator")
public class EmailValidator implements Validator {

    private static final String REGEX =
            "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    /* (non-Javadoc)
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!validateEmailAddress((String) value)) {
            FacesMessage msg = new FacesMessage(Helper.getTranslation("email_errlnvalid", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     *
     * @param email
     * @return
     * @should match correct email addresses
     * @should match entire email address only
     * @should not match invalid addresses
     */
    protected static boolean validateEmailAddress(String email) {
        if (email == null) {
            return false;
        }
        Matcher m = PATTERN.matcher(email.toLowerCase());
        return m.find();
    }
}
