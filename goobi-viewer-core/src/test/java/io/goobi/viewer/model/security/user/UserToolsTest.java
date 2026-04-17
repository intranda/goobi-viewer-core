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
package io.goobi.viewer.model.security.user;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;

class UserToolsTest extends AbstractDatabaseEnabledTest {

    /**
     * @verifies delete all bookmark lists owned by user
     */
    @Test
    void deleteBookmarkListsForUser_shouldDeleteAllBookmarkListsOwnedByUser() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assertions.assertNotNull(user);
        Assertions.assertFalse(DataManager.getInstance().getDao().getBookmarkLists(user).isEmpty());

        UserTools.deleteBookmarkListsForUser(user);
        Assertions.assertTrue(DataManager.getInstance().getDao().getBookmarkLists(user).isEmpty());
    }

    /**
     * @verifies delete all searches owned by user
     */
    @Test
    void deleteSearchesForUser_shouldDeleteAllSearchesOwnedByUser() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assertions.assertNotNull(user);
        Assertions.assertFalse(DataManager.getInstance().getDao().getSearches(user).isEmpty());

        UserTools.deleteSearchesForUser(user);
        Assertions.assertTrue(DataManager.getInstance().getDao().getSearches(user).isEmpty());
    }

    /**
     * @verifies delete all user groups owned by user
     */
    @Test
    void deleteUserGroupOwnedByUser_shouldDeleteAllUserGroupsOwnedByUser() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assertions.assertNotNull(user);
        Assertions.assertNotNull(DataManager.getInstance().getDao().getUserGroup(2));

        UserTools.deleteUserGroupOwnedByUser(user);
        Assertions.assertNull(DataManager.getInstance().getDao().getUserGroup(2));
        // TODO: This test only verifies that group 2 was deleted. User 1 owns additional groups
        // (e.g. "user group 1 name") whose deletion fails silently (ERROR log visible during test run).
        // Root cause: deleteUserGroup() returns false, likely due to an unresolved foreign key
        // dependency in the test data. The test should assert that ALL groups owned by the user
        // are deleted, and the method must properly clean up all dependencies beforehand.
    }

    /**
     * @verifies delete user comments and remove user from campaign record statistics
     */
    @Test
    void deleteUserPublicContributions_shouldDeleteUserCommentsAndRemoveUserFromCampaignRecordStatistics() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assertions.assertNotNull(user);
        UserTools.deleteUserPublicContributions(user);

        // Comments
        Assertions.assertNull(DataManager.getInstance().getDao().getComment(2));

        // Campaign statistics
        List<CampaignRecordStatistic> statistics = DataManager.getInstance().getDao().getCampaignStatisticsForRecord("PI_1", null);
        Assertions.assertEquals(1, statistics.size());
        Assertions.assertTrue(statistics.get(0).getReviewers().isEmpty());
        Assertions.assertFalse(statistics.get(0).getReviewers().contains(user));
    }

    /**
     * @verifies replace user as comment creator and campaign reviewer with anonymous identity
     */
    @Test
    void anonymizeUserPublicContributions_shouldReplaceUserAsCommentCreatorAndCampaignReviewerWithAnonymousIdentity() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assertions.assertNotNull(user);
        Assertions.assertTrue(UserTools.anonymizeUserPublicContributions(user));

        // Comments
        Comment comment = DataManager.getInstance().getDao().getComment(2);
        Assertions.assertNotNull(comment);
        Assertions.assertNotEquals(user, comment.getCreator());

        // Campaign statistics
        List<CampaignRecordStatistic> statistics = DataManager.getInstance().getDao().getCampaignStatisticsForRecord("PI_1", null);
        Assertions.assertEquals(1, statistics.size());
        Assertions.assertEquals(1, statistics.get(0).getReviewers().size());
        Assertions.assertFalse(statistics.get(0).getReviewers().contains(user));
    }
}
