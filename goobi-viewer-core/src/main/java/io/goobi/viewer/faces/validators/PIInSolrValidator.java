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
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Syntax validator for passwords addresses.
 */
@FacesValidator("piInSolrValidator")
public class PIInSolrValidator implements Validator<String> {

    /* (non-Javadoc)
     * @see jakarta.faces.validator.Validator#validate(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        try {
            if (!validatePi(value)) {
                FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("pi_errNotFound", null), "");
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
        } catch (IndexUnreachableException | PresentationException e) {
            FacesMessage msg = new FacesMessage(ViewerResourceBundle.getTranslation("pi_validationError", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     * Checks if the given pi matches a known PI in the solr index. If the pi is empty, true is returned to allow not setting any pi
     *
     * @return false if no current collection is set, the pi does not match any known work
     * @param pi a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static boolean validatePi(String pi) throws IndexUnreachableException, PresentationException {
        if (StringUtils.isNotBlank(pi)) {
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
            return doc != null;
        }

        return true;
    }
}
