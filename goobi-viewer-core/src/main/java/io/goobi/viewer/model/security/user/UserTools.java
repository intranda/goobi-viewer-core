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
import io.goobi.viewer.faces.validators.EmailValidator;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.modules.IModule;

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

    /**
     * Deletes all public content created by this the given user.
     * 
     * @param user
     * @throws DAOException
     * @should delete all user public content correctly
     */
    public static void deleteUserPublicContributions(User user) throws DAOException {
        if (user == null) {
            return;
        }

        // Delete comments
        int comments = DataManager.getInstance().getDao().deleteComments(null, user);
        logger.debug("{} comment(s) of user {} deleted.", comments, user.getId());

        // Delete campaign statistics
        int campaigns = DataManager.getInstance().getDao().deleteCampaignStatisticsForUser(user);
        logger.debug("Deleted user from the statistics from {} campaign(s)", campaigns);

        // Delete module contributions
        for (IModule module : DataManager.getInstance().getModules()) {
            int count = module.deleteUserContributions(user);
            if (count > 0) {
                logger.debug("Deleted {} user contribution(s) via the '{}' module.", count, module.getName());
            }
        }
    }

    /**
     * Moves all public content from the given user to an anonymous user.
     * 
     * @param user
     * @throws DAOException
     * @should anonymize all user public content correctly
     */
    public static boolean anonymizeUserPublicContributions(User user) throws DAOException {
        User anon = UserTools.checkAndCreateAnonymousUser();
        if (anon == null) {
            logger.error("Anonymous user could not be found");
            return false;
        }

        // Move comments
        int comments = DataManager.getInstance().getDao().changeCommentsOwner(user, anon);
        logger.debug("{} comment(s) of user {} anonymized.", comments, user.getId());

        // Move campaign statistics
        int campaigns = DataManager.getInstance().getDao().changeCampaignStatisticContributors(user, anon);
        logger.debug("Anonymized user in the statistics from {} campaign(s)", campaigns);

        // Move module contributions
        for (IModule module : DataManager.getInstance().getModules()) {
            int count = module.moveUserContributions(user, anon);
            if (count > 0) {
                logger.debug("Anonymized {} user contribution(s) via the '{}' module.", count, module.getName());
            }
        }
        return true;
    }

    /**
     * 
     * @return
     * @throws DAOException
     */
    public static User checkAndCreateAnonymousUser() throws DAOException {
        String email = DataManager.getInstance().getConfiguration().getAnonymousUserEmailAddress();
        if (!EmailValidator.validateEmailAddress(email)) {
            logger.warn("'anonymousUserEmailAddress' not configured or contains an invalid address"
                    + " - unable to keep anonymous contributions of deleted users.");
            return null;
        }

        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setNickName("Anonymous");
            user.setSuperuser(false);
            user.setSuspended(true);
            if (DataManager.getInstance().getDao().addUser(user)) {
                logger.info("Added anonymous user '{}'.", email);
            }
        } else {
            logger.trace("Anonymous user '{}' already exists.", email);
        }

        return user;
    }
}
