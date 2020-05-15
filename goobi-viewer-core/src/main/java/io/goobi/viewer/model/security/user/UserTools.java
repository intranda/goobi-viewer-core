/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.security.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.search.Search;

public class UserTools {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(UserTools.class);

    /**
     * Deletes given user from the database and removes any database rows that reference this user (only those that are of use to this user - public
     * content such as comments must be deleted separetely).
     * 
     * @param user User to delete
     * @return true if successful; false otherwise
     * @throws DAOException
     */
    public static boolean deleteUser(User user) throws DAOException {
        if (user == null) {
            return false;
        }

        // Delete group memberships
        if (user.getUserGroupMemberships() != null) {
            for (UserRole userRole : user.getUserGroupMemberships()) {
                DataManager.getInstance().getDao().deleteUserRole(userRole);
            }
        }
        // Delete bookmark lists
        deleteBookmarkListsForUser(user);
        // Delete searches
        deleteSearchesForUser(user);

        return DataManager.getInstance().getDao().deleteUser(user);
    }

    /**
     * 
     * @param user Owner of groups to delete
     * @return
     * @throws DAOException
     * @should delete all user groups owned by user
     */
    public static int deleteUserGroupOwnedByUser(User owner) throws DAOException {
        List<UserGroup> userGroups = owner.getUserGroupOwnerships();
        logger.error("user groups: " + userGroups.size());
        if (userGroups.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (UserGroup userGroup : userGroups) {
            // Delete memberships first
            if (!userGroup.getMemberships().isEmpty()) {
                for (UserRole userRole : userGroup.getMemberships()) {
                    DataManager.getInstance().getDao().deleteUserRole(userRole);
                }
            }
            if (DataManager.getInstance().getDao().deleteUserGroup(userGroup)) {
                logger.debug("User group '{}' belonging to user {} deleted", userGroup.getName(), owner.getId());
                count++;
            }
        }

        return count;
    }

    /**
     * 
     * @param owner
     * @return
     * @throws DAOException
     * @should delete all bookmark lists owned by user
     */
    public static int deleteBookmarkListsForUser(User owner) throws DAOException {
        List<BookmarkList> bookmarkLists = DataManager.getInstance().getDao().getBookmarkLists(owner);
        if (bookmarkLists.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (BookmarkList bookmarkList : bookmarkLists) {
            if (DataManager.getInstance().getDao().deleteBookmarkList(bookmarkList)) {
                count++;
            }
        }
        logger.debug("{} bookmarklists of user {} deleted.", count, owner.getId());

        return count;
    }

    /**
     * 
     * @param owner
     * @return
     * @throws DAOException
     * @should delete all searches owned by user
     */
    public static int deleteSearchesForUser(User owner) throws DAOException {
        List<Search> searches = DataManager.getInstance().getDao().getSearches(owner);
        if (searches.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Search search : searches) {
            if (DataManager.getInstance().getDao().deleteSearch(search)) {
                count++;
            }
        }
        logger.debug("{} saved searches of user {} deleted.", count, owner.getId());

        return count;
    }
}
