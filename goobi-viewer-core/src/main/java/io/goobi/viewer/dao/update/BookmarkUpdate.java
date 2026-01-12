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

public class BookmarkUpdate implements IModelUpdate {

    /** {@inheritDoc} */
    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        return removeMainTitleCol(dao);
    }

    private static boolean removeMainTitleCol(IDAO dao) throws DAOException, SQLException {
        if (dao.columnsExists("bookshelf_items", "main_title")) {
            dao.executeUpdate("ALTER TABLE bookshelf_items DROP COLUMN main_title");
            return true;
        }

        return false;
    }
}
