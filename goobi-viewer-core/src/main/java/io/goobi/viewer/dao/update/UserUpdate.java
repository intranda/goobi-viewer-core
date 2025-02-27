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
import java.util.List;

import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

/**
 * @author florian
 *
 */
public class UserUpdate implements IModelUpdate {

    private static final String TABLE_NAME_CURRENT = "viewer_users";

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        boolean ret = false;

        // Update table name
        if (dao.tableExists("users")) {

            // Delete new table with anonymous use, if already created by EclipseLink
            if (dao.tableExists(TABLE_NAME_CURRENT)) {
                boolean newTableEmpty = dao.getNativeQueryResults("SELECT * FROM " + TABLE_NAME_CURRENT).size() <= 1;
                if (newTableEmpty) {
                    dao.executeUpdate("DROP TABLE " + TABLE_NAME_CURRENT);
                }

            }

            dao.executeUpdate("RENAME TABLE users TO " + TABLE_NAME_CURRENT);
            ret = true;
        }

        if (dao.columnsExists(TABLE_NAME_CURRENT, "use_gravatar")) {
            List<Long> userIds = dao.getNativeQueryResults("SELECT user_id FROM " + TABLE_NAME_CURRENT + " WHERE use_gravatar=1");
            for (Long userId : userIds) {
                dao.executeUpdate("UPDATE " + TABLE_NAME_CURRENT + " SET avatar_type='GRAVATAR' WHERE " + TABLE_NAME_CURRENT + ".user_id=" + userId);
            }
            dao.executeUpdate(StringConstants.SQL_ALTER_TABLE + TABLE_NAME_CURRENT + " DROP COLUMN use_gravatar");
            ret = true;
        }

        return ret;
    }

}
