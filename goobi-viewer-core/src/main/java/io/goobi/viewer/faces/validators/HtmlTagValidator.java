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

import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Validates that any input text has no html-tags other than <br>
 * <
 *
 * @author Florian Alpers
 */
@FacesValidator("htmlTagValidator")
public class HtmlTagValidator implements Validator<String> {

    private static final List<String> ALLOWED_TAGS =
            Arrays.asList("br", "b", "strong", "em", "i", "mark", "small", "del", "ins", "sub", "sup");

    /**
     * {@inheritDoc}
     *
     * Throws a {@link ValidatorException} with message key {@code validate_error_scriptTag} if {@link #validate(String)} returns false
     */
    @Override
    public void validate(FacesContext context, UIComponent component, String input) throws ValidatorException {
        if (!validate(input)) {
            FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("validate_error_invalidTag", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }

    }

    /**
     * Returns false if the input string is not blank and does not contain any tags other than the allowed
     *
     * @param input a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean validate(String input) {
        if (StringUtils.isNotBlank(input)) {
            Document doc = Jsoup.parse(input);
            return doc.head().childNodes().isEmpty() //no content in head
                    && doc.body()
                            .select("*")
                            .stream() //all tags within body
                            .skip(1) //skip body tag itself
                            .allMatch(element -> ALLOWED_TAGS.contains(element.tagName().toLowerCase())); //all tags have names in allowedTags
        }

        return true;
    }

}
