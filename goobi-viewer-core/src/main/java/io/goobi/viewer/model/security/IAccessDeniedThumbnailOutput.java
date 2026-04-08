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
package io.goobi.viewer.model.security;

import java.util.Locale;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;

/**
 * Classes implementing this interface must provide a method returning the appropriate access denied replacement thumnnail URL.
 */
public interface IAccessDeniedThumbnailOutput {

    /**
     * 
     * @param locale locale for selecting the appropriate image
     * @return Configured image URI for the given language; null if none found
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getAccessDeniedThumbnailUrl(Locale locale) throws IndexUnreachableException, DAOException;
}
