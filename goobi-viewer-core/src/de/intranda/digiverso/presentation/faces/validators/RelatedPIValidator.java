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

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.user.User;

/**
 * Validates the entered PI belonging to a record for which the current user may create CMS content.
 */
@FacesValidator("relatedPiValidator")
public class RelatedPIValidator implements Validator<String> {

    private static final char[] ILLEGAL_CHARS = { '!', '?', '/', '\\', ':', ';', '(', ')', '@', '"', '\'' };

    /* (non-Javadoc)
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        User user = null;
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null) {
            user = userBean.getUser();
        }
        if (!validatePi(value, user)) {
            FacesMessage msg = new FacesMessage(Helper.getTranslation("cms_related_pi_invalid", null), "");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    /**
     *
     * @param pi
     * @param user
     * @return
     * @should return true if pi good
     * @should return false if pi empty, blank or null
     * @should return false if user is null
     * @should return true if user is superuser
     */
    public static boolean validatePi(String pi, User user) {
        if (StringUtils.isEmpty(pi)) {
            return true;
        }
        if (user == null || !user.isCmsAdmin()) {
            return false;
        }
        if (user.isSuperuser()) {
            return true;
        }

        // TODO check for the PI's having an allowed discriminator value
        if (StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField())) {
            
        }

        return !StringUtils.containsAny(pi, ILLEGAL_CHARS);
    }
}
