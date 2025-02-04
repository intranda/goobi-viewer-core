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
 * Validates that any input text has no "script" tags
 *
 * @author Florian Alpers
 */
@FacesValidator("htmlScriptValidator")
public class HtmlScriptValidator implements Validator<String> {

    /**
     * {@inheritDoc}
     *
     * Throws a {@link ValidatorException} with message key {@code validate_error_scriptTag} if {@link #validate(String)} returns false
     */
    @Override
    public void validate(FacesContext context, UIComponent component, String input) throws ValidatorException {
        if (!validate(input)) {
            FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("validate_error_scriptTag", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }

    }

    /**
     * Returns false if the input string is not blank and does not contain the string {@code <script} (disregarding case)
     *
     * @param input a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean validate(String input) {
        return !(StringUtils.isNotBlank(input) && input.toLowerCase().contains("<script"));
    }

}
