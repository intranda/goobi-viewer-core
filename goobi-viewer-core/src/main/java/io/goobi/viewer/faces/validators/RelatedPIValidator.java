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

import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Validates the entered PI belonging to a record for which the current user may create CMS content.
 */
@FacesValidator("relatedPiValidator")
public class RelatedPIValidator extends PIValidator {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(RelatedPIValidator.class);

    /* (non-Javadoc)
     * @see jakarta.faces.validator.Validator#validate(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        User user = null;
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null) {
            user = userBean.getUser();
        }
        try {
            String key = validatePi(value, user);
            if (key != null) {
                String message = ViewerResourceBundle.getTranslation(key, null).replace("{0}", value);
                FacesMessage msg = new FacesMessage(message, "");
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
            Messages.info(component.getClientId(), "");
        } catch (PresentationException e) {
            logger.error(e.getMessage());
            FacesMessage msg = new FacesMessage(e.getMessage(), "");
            throw new ValidatorException(msg);
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
            FacesMessage msg = new FacesMessage(e.getMessage(), "");
            throw new ValidatorException(msg);
        }
    }

    /**
     * <p>
     * validatePi.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @should return true if pi good
     * @should return false if pi empty, blank or null
     * @should return false if user is null
     * @should return true if user is superuser
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static String validatePi(String pi, User user) throws PresentationException, IndexUnreachableException {
        // Allow for related PI to be optional
        if (StringUtils.isEmpty(pi)) {
            return null;
        }
        if (StringUtils.containsAny(pi, PIValidator.ILLEGAL_CHARS)) {
            return "cms_page_related_pi_illegal_chars";
        }
        if (user == null || !user.isCmsAdmin()) {
            return "cms_page_related_pi_forbidden";
        }
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ':' + pi, null);
        if (doc == null) {
            return "cms_page_related_pi_not_found";
        }
        if (user.isSuperuser()) {
            return null;
        }

        // TODO check for the PI's having an allowed discriminator value
        String discriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
        if (StringUtils.isNotEmpty(discriminatorField)) {
            //            discriminatorField += SolrConstants._UNTOKENIZED;
            if (doc.containsKey(discriminatorField)) {
                List<String> allValues = SearchHelper.getFacetValues(discriminatorField + ":*", discriminatorField, 0);
                List<String> allowedValues = user.getAllowedSubthemeDiscriminatorValues(allValues);
                if (!allowedValues.contains(doc.getFieldValue(discriminatorField))) {
                    return "cms_page_related_pi_forbidden";
                }
            }
        }

        return null;
    }
}
