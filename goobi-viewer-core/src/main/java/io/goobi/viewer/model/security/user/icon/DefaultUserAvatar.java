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
package io.goobi.viewer.model.security.user.icon;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * @author florian
 *
 */
public class DefaultUserAvatar implements UserAvatar {

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.user.icon.IconProvider#getIconUrl()
     */
    @Override
    public String getIconUrl(int size, HttpServletRequest request) {
        if (request != null) {
            String contextPath = request.getContextPath();
            return contextPath + "/resources/images/backend/thumbnail_goobi_person.svg";
        }

        return Optional.ofNullable(BeanUtils.getNavigationHelper())
                .map(NavigationHelper::getApplicationUrl)
                .orElse("/") + "resources/images/backend/thumbnail_goobi_person.svg";
    }

}
