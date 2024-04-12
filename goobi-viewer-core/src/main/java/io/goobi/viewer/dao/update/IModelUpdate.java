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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

/**
 * Interface for classes which manipulate the database to conform to the setup required by the viewer
 *
 * @author florian
 */
public interface IModelUpdate {

    /**
     * <p>
     * update.
     * </p>
     *
     * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
     * @param templateManager
     * @return true if rows were updated; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.sql.SQLException if any.
     */
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException;
}
