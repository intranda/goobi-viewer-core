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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

public class CommentGroupUpdate implements IModelUpdate {

    private static final Logger logger = LogManager.getLogger(CommentGroupUpdate.class);

    /** {@inheritDoc} */
    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        return performUpdates(dao);
    }

    /**
     * <p>
     * persistData.
     * </p>
     *
     * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    private static boolean performUpdates(IDAO dao) throws DAOException {
        // Add "all" comment group, if not yet exists
        CommentGroup commentGroup = dao.getCommentGroupUnfiltered();
        if (commentGroup == null) {
            commentGroup = CommentGroup.createCommentGroupAll();
            if (DataManager.getInstance().getDao().addCommentGroup(commentGroup)) {
                logger.info("Added static \"all comments\" comment group to DB.");
                return true;
            }
        }

        return false;
    }
}
