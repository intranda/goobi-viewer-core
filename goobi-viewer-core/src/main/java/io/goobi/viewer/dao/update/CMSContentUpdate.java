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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

/**
 * Database migration step for the cms_content table: remaps removed content types and drops
 * their orphaned data columns.
 */
public class CMSContentUpdate implements IModelUpdate {

    private static final Logger logger = LogManager.getLogger(CMSContentUpdate.class);

    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {

        int updates = 0;

        if (dao.columnsExists("cms_content", "content_type")) {
            int rows = dao.executeUpdate(
                    "UPDATE cms_content SET content_type='shorttext' WHERE content_type='glossary'");
            if (rows > 0) {
                updates++;
                logger.info("Migrated {} orphaned glossary cms_content rows to shorttext", rows);
            }
        }

        if (dao.columnsExists("cms_content", "glossary")) {
            dao.executeUpdate("ALTER TABLE cms_content DROP COLUMN glossary");
            updates++;
            logger.info("Dropped obsolete column: cms_content.glossary");
        }

        return updates > 0;
    }
}
