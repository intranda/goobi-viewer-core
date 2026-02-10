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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.messages.ViewerResourceBundle;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

/**
 * Syntax validator for e-mail addresses.
 */
@FacesValidator("emailValidator")
public class EmailValidator implements Validator<String> {

    private static final String REGEX =
            "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"//NOSONAR
                    + "(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$"; //NOSONAR   input size is limited

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    /* (non-Javadoc)
     * @see jakarta.faces.validator.Validator#validate(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        boolean allowEmptyString = getAsBoolean(component.getAttributes().get("allowEmptyString"));
        if (!validateEmailAddress(value, allowEmptyString)) {
            FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("email_errlnvalid", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     * <p>
     * validateEmailAddress.
     * </p>
     *
     * @param email a {@link java.lang.String} object.
     * @param allowEmptyString use `true` if an empty email address is allowed
     * @should match correct email addresses
     * @should match entire email address only
     * @should not match invalid addresses
     * @return a boolean.
     */
    public static boolean validateEmailAddress(String email, boolean allowEmptyString) {
        if (allowEmptyString && StringUtils.isBlank(email)) {
            return true;
        }
        if (email == null || email.length() > 10_000) {
            return false;
        }
        Matcher m = PATTERN.matcher(email.toLowerCase());
        return m.find();
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
