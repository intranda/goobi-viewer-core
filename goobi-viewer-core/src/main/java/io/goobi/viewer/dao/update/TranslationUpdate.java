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
import java.util.Objects;

import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

public class TranslationUpdate implements IModelUpdate {

    private static final String[] TABLES = { "cs_campaign_translations", "cms_geomap_translation", "terms_of_use_translations", "translations" };

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        // Update column names
        for (String table : TABLES) {
            if (dao.tableExists(table)) {
                boolean newColumnHasEntries = dao.getNativeQueryResults("SELECT translation_value FROM " + table).stream().anyMatch(Objects::nonNull);
                if (!newColumnHasEntries) {
                    dao.executeUpdate(StringConstants.SQL_ALTER_TABLE + table + " DROP translation_value;");
                    try {
                        dao.executeUpdate(StringConstants.SQL_ALTER_TABLE + table + " RENAME COLUMN value TO translation_value");
                        if (!dao.columnsExists(table, "translation_value")) {
                            dao.executeUpdate(StringConstants.SQL_ALTER_TABLE + table + " CHANGE value translation_value varchar(255)");
                        }
                    } catch (DAOException e) {
                        //exception is  not reliable. Ignore for now and check result of operation later
                    }
                    if (!dao.columnsExists("cms_pages", "page_template_id")) {
                        dao.executeUpdate(StringConstants.SQL_ALTER_TABLE + table + " ADD COLUMN page_template_id varchar(255);");
                    }

                }

            }
        }

        return true;
    }
}
