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
package io.goobi.viewer.model.bookmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * <p>
 * BookmarkTools class.
 * </p>
 */
public final class BookmarkTools {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(BookmarkTools.class);

    /** Private constructor */
    private BookmarkTools() {
        //
    }

    /**
     * <p>
     * getBookmarkListsSharedWithUser.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @should return shared bookmark lists
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static List<BookmarkList> getBookmarkListsSharedWithUser(User user) throws DAOException {
        if (user == null) {
            return Collections.emptyList();
        }
        List<UserGroup> userGroups = user.getAllUserGroups();
        logger.trace("user groups: {}", userGroups.size());
        if (userGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<BookmarkList> allBookmarkLists = DataManager.getInstance().getDao().getAllBookmarkLists();
        logger.trace("all bookmark lists: {}", allBookmarkLists.size());
        if (allBookmarkLists.isEmpty()) {
            return Collections.emptyList();
        }

        List<BookmarkList> ret = new ArrayList<>();
        for (BookmarkList bl : allBookmarkLists) {
            if (bl.getOwner().equals(user) || bl.getGroupShares().isEmpty()) {
                continue;
            }
            for (UserGroup ug : userGroups) {
                if (!ret.contains(bl) && bl.getGroupShares().contains(ug)) {
                    ret.add(bl);
                    break;
                }
            }
        }

        return ret;
    }

}
