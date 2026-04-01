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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

public class SubthemeColumnUpdate implements IModelUpdate {

    private static final Logger logger = LogManager.getLogger(SubthemeColumnUpdate.class);

    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {

        int updates = 0;

        updates += migrateColumn(dao, "cms_pages");
        updates += migrateColumn(dao, "cms_page_templates");

        return updates > 0;
    }

    private int migrateColumn(IDAO dao, String table) throws DAOException {
        boolean hasOldColumn = !dao.getNativeQueryResults("SHOW COLUMNS FROM " + table + " LIKE 'subtheme_discriminator'").isEmpty();
        boolean hasNewColumn = !dao.getNativeQueryResults("SHOW COLUMNS FROM " + table + " LIKE 'subtheme'").isEmpty();

        if (hasOldColumn && hasNewColumn) {
            dao.executeUpdate("UPDATE " + table + " SET subtheme = subtheme_discriminator WHERE subtheme IS NULL OR subtheme = ''");
            dao.executeUpdate("ALTER TABLE " + table + " DROP COLUMN subtheme_discriminator");
            logger.info("Migrated data from {}.subtheme_discriminator to subtheme and dropped old column", table);
            return 1;
        } else if (hasOldColumn) {
            dao.executeUpdate("ALTER TABLE " + table + " RENAME COLUMN subtheme_discriminator TO subtheme");
            logger.info("Renamed column {}.subtheme_discriminator to subtheme", table);
            return 1;
        }

        return 0;
    }
}
