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
package io.goobi.viewer.managedbeans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.storage.StorageBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;

/**
 * @author florian Store variables in session scope
 */
@Named
@SessionScoped
public class SessionBean extends StorageBean {

    private static final long serialVersionUID = -4828711470203824408L;
    
    private static final Logger logger = LogManager.getLogger(SessionBean.class);

    /**
     * Removes the user and permission attributes from the session.
     *
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void wipeSessionAttributes() throws IndexUnreachableException, PresentationException, DAOException {
        logger.trace("wipeSession");
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return;
            }
            session.removeAttribute("user");

            // Remove priv maps
            AccessConditionUtils.clearSessionPermissions(session);

            cleanObjects();

            BeanUtils.getBeanFromRequest(request, "collectionViewBean", CollectionViewBean.class)
                    .ifPresentOrElse(CollectionViewBean::invalidate,
                            () -> logger.trace("Cannot invalidate CollectionViewBean. Not instantiated yet?"));
            BeanUtils.getBeanFromRequest(request, "activeDocumentBean", ActiveDocumentBean.class)
                    .ifPresentOrElse(ActiveDocumentBean::resetAccess,
                            () -> logger.trace("Cannot reset access permissions in ActiveDocumentBean. Not instantiated yet?"));
            BeanUtils.getBeanFromRequest(request, "displayConditions", DisplayConditions.class)
                    .ifPresentOrElse(DisplayConditions::clearCache,
                            () -> logger.trace("Cannot clear DosplayConditions cache. Not instantiated yet?"));
            // Reset loaded user-generated content lists
            BeanUtils.getBeanFromRequest(request, "contentBean", ContentBean.class)
                    .ifPresentOrElse(ContentBean::resetContentList,
                            () -> logger.trace("Cannot reset content list. Not instantiated yet?"));
            // Reset visible navigation menu
            BeanUtils.getBeanFromRequest(request, "cmsBean", CmsBean.class)
                    .ifPresentOrElse(CmsBean::resetNavigationMenuItems,
                            () -> logger.trace("Cannot reset navigation menu items. Not instantiated yet?"));
        }
    }
}
