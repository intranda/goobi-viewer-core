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
 * Adds an index on <code>mq_messages.last_update_time</code> to existing databases. Old finished messages are deleted by this column
 * (see {@link io.goobi.viewer.managedbeans.MessageQueueBean}); without an index, every such deletion scans and locks the entire table, which
 * with hundreds of thousands of messages blocked the message queue and every request that queued a task.
 */
public class MessageQueueUpdate implements IModelUpdate {

    private static final Logger logger = LogManager.getLogger(MessageQueueUpdate.class);

    static final String TABLE_NAME = "mq_messages";
    static final String COLUMN_NAME = "last_update_time";
    static final String INDEX_NAME = "idx_mq_messages_last_update_time";

    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        if (!dao.tableExists(TABLE_NAME) || dao.columnIndexExists(TABLE_NAME, COLUMN_NAME)) {
            return false;
        }
        dao.executeUpdate("CREATE INDEX " + INDEX_NAME + " ON " + TABLE_NAME + " (" + COLUMN_NAME + ")");
        logger.info("Added index {} on {}.{}", INDEX_NAME, TABLE_NAME, COLUMN_NAME);
        return true;
    }
}
