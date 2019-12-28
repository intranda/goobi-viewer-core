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
package io.goobi.viewer.faces.validators;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Syntax validator for passwords addresses.
 */
@FacesValidator("piInSolrValidator")
public class PIInSolrValidator implements Validator {

    /* (non-Javadoc)
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        try {
            if (!validatePi((String) value)) {
                FacesMessage msg = new FacesMessage(Helper.getTranslation("pi_errNotFound", null), "");
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
        } catch (IndexUnreachableException | PresentationException e) {
            FacesMessage msg = new FacesMessage(Helper.getTranslation("pi_validationError", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     * Checks if the given pi matches a known PI in the solr index. If the pi is empty, true is returned to allow not setting any pi
     *
     * @return false if no current collection is set, the pi does not match any known work
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @param pi a {@link java.lang.String} object.
     */
    public static boolean validatePi(String pi) throws IndexUnreachableException, PresentationException {
        if(StringUtils.isNotBlank(pi)) {            
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
            return doc != null;
        } else {
            return true;
        }
    }
}
