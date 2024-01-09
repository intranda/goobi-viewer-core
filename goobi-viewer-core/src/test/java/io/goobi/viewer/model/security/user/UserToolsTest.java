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

public class UserToolsTest extends AbstractDatabaseEnabledTest {

    /**
     * @see UserTools#deleteBookmarkListsForUser(User)
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
     * @see UserTools#deleteSearchesForUser(User)
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
     * @see UserTools#deleteUserGroupOwnedByUser(User)
     * @verifies delete all user groups owned by user
     */
    @Test
    void deleteUserGroupOwnedByUser_shouldDeleteAllUserGroupsOwnedByUser() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assertions.assertNotNull(user);
        Assertions.assertNotNull(DataManager.getInstance().getDao().getUserGroup(1));

        UserTools.deleteUserGroupOwnedByUser(user);
        Assertions.assertNull(DataManager.getInstance().getDao().getUserGroup(1));
    }

    /**
     * @see UserTools#deleteUserPublicContributions(User)
     * @verifies delete all user public content correctly
     */
    @Test
    void deleteUserPublicContributions_shouldDeleteAllUserPublicContentCorrectly() throws Exception {
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
     * @see UserTools#anonymizeUserPublicContributions(User)
     * @verifies anonymize all user public content correctly
     */
    @Test
    void anonymizeUserPublicContributions_shouldAnonymizeAllUserPublicContentCorrectly() throws Exception {
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
