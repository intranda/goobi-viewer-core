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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.StatisticMode;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

class CampaignTest extends AbstractDatabaseEnabledTest {

    /**
     * @see Campaign#getDaysLeft()
     * @verifies return -1 if no dateEnd
     */
    @Test
    void getDaysLeft_shouldReturn1IfNoDateEnd() throws Exception {
        Campaign campaign = new Campaign();
        Assertions.assertEquals(-1, campaign.getDaysLeft());
    }

    /**
     * @see Campaign#getDaysLeft()
     * @verifies calculate days correctly
     */
    @Test
    void getDaysLeft_shouldCalculateDaysCorrectly() throws Exception {
        Campaign campaign = new Campaign();
        {
            LocalDateTime later = LocalDateTime.now().plusDays(99);
            campaign.setDateEnd(later);
            Assertions.assertEquals(99, campaign.getDaysLeft());
        }
        {
            LocalDateTime earlier = LocalDateTime.now().plusDays(-20);
            campaign.setDateEnd(earlier);
            Assertions.assertEquals(0, campaign.getDaysLeft());
        }
    }

    /**
     * @see Campaign#getDaysBeforeStart()
     * @verifies return -1 if no dateStart
     */
    @Test
    void getDaysBeforeStart_shouldReturn1IfNoDateStart() throws Exception {
        Campaign campaign = new Campaign();
        Assertions.assertEquals(-1, campaign.getDaysBeforeStart());
    }

    /**
     * @see Campaign#getDaysBeforeStart()
     * @verifies calculate days correctly
     */
    @Test
    void getDaysBeforeStart_shouldCalculateDaysCorrectly() throws Exception {
        Campaign campaign = new Campaign();
        {
            LocalDateTime later = LocalDateTime.now().plusDays(15);
            campaign.setDateStart(later);
            Assertions.assertEquals(15, campaign.getDaysBeforeStart());
        }
        {
            LocalDateTime earlier = LocalDateTime.now().plusDays(-20);
            campaign.setDateStart(earlier);
            Assertions.assertEquals(0, campaign.getDaysBeforeStart());
        }
    }

    /**
     * @see Campaign#isHasEnded()
     * @verifies return false if dateEnd null
     */
    @Test
    void isHasEnded_shouldReturnFalseIfDateEndNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setTimePeriodEnabled(true);
        Assertions.assertFalse(campaign.isHasEnded());
    }

    /**
     * @see Campaign#isHasEnded()
     * @verifies return true if dateEnd before now
     */
    @Test
    void isHasEnded_shouldReturnTrueIfDateEndBeforeNow() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime earlier = LocalDateTime.now().plusDays(-20);
        campaign.setDateEnd(earlier);
        campaign.setTimePeriodEnabled(true);
        Assertions.assertTrue(campaign.isHasEnded());
    }

    /**
     * @see Campaign#isHasEnded()
     * @verifies return false if dateEnd after now
     */
    @Test
    void isHasEnded_shouldReturnFalseIfDateEndAfterNow() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime later = LocalDateTime.now().plusDays(20);
        campaign.setDateEnd(later);
        campaign.setTimePeriodEnabled(true);
        Assertions.assertFalse(campaign.isHasEnded());
    }

    /**
     * @see Campaign#isHasEnded()
     * @verifies return false if timePeriodEnabled false
     */
    @Test
    void isHasEnded_shouldReturnFalseIfTimePeriodEnabledFalse() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime earlier = LocalDateTime.now().plusDays(-20);
        campaign.setDateEnd(earlier);
        Assertions.assertFalse(campaign.isHasEnded());
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if dateStart null
     */
    @Test
    void isHasStarted_shouldReturnTrueIfDateStartNull() throws Exception {
        Campaign campaign = new Campaign();
        Assertions.assertTrue(campaign.isHasStarted());
        campaign.setTimePeriodEnabled(true);
        campaign.setTimePeriodEnabled(true);
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if dateStart equals now
     */
    @Test
    void isHasStarted_shouldReturnTrueIfDateStartEqualsNow() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setDateStart(LocalDateTime.now());
        campaign.setTimePeriodEnabled(true);
        Assertions.assertTrue(campaign.isHasStarted());
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if dateStart before now
     */
    @Test
    void isHasStarted_shouldReturnTrueIfDateStartBeforeNow() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime later = LocalDateTime.now().plusDays(-20);
        campaign.setDateStart(later);
        campaign.setTimePeriodEnabled(true);
        Assertions.assertTrue(campaign.isHasStarted());
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return false if dateStart after now
     */
    @Test
    void isHasStarted_shouldReturnFalseIfDateStartAfterNow() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime later = LocalDateTime.now().plusDays(20);
        campaign.setDateStart(later);
        campaign.setTimePeriodEnabled(true);
        Assertions.assertFalse(campaign.isHasStarted());
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if timePeriodEnabled false
     */
    @Test
    void isHasStarted_shouldReturnTrueIfTimePeriodEnabledFalse() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime later = LocalDateTime.now().plusDays(-20);
        campaign.setDateStart(later);
        campaign.setTimePeriodEnabled(true);
        Assertions.assertTrue(campaign.isHasStarted());
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CrowdsourcingStatus)
     * @verifies return true if campaign public
     */
    @Test
    void isUserAllowedAction_shouldReturnTrueIfCampaignPublic() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setVisibility(CampaignVisibility.PUBLIC);
        campaign.getQuestions().add(new Question(campaign));
        Assertions.assertTrue(campaign.isUserAllowedAction(null, CrowdsourcingStatus.ANNOTATE));
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CrowdsourcingStatus)
     * @verifies return false if outside time period
     */
    @Test
    void isUserAllowedAction_shouldReturnFalseIfOutsideTimePeriod() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        campaign.setTimePeriodEnabled(true);
        campaign.setDateStart(LocalDateTime.of(2000, 01, 01, 0, 0));
        campaign.setDateEnd(LocalDateTime.of(2001, 01, 01, 0, 0));
        Assertions.assertFalse(campaign.isUserAllowedAction(user, CrowdsourcingStatus.ANNOTATE));
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CrowdsourcingStatus)
     * @verifies return true if user owner of group
     */
    @Test
    void isUserAllowedAction_shouldReturnTrueIfUserOwnerOfGroup() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.getQuestions().add(new Question(campaign));
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assertions.assertTrue(campaign.isUserAllowedAction(user, CrowdsourcingStatus.ANNOTATE));
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CrowdsourcingStatus)
     * @verifies return true if user member of group
     */
    @Test
    void isUserAllowedAction_shouldReturnTrueIfUserMemberOfGroup() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assertions.assertNotNull(user);
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assertions.assertNotNull(userGroup);
        Role role = DataManager.getInstance().getDao().getRole(1);
        Assertions.assertNotNull(role);

        Campaign campaign = new Campaign();
        campaign.getQuestions().add(new Question(campaign));
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(userGroup);
        campaign.getUserGroup().addMember(user, role);
        Assertions.assertTrue(campaign.isUserAllowedAction(user, CrowdsourcingStatus.ANNOTATE));
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CrowdsourcingStatus)
     * @verifies return false if user not in group
     */
    @Test
    void isUserAllowedAction_shouldReturnFalseIfUserNotInGroup() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        Assertions.assertFalse(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return false if user null
     */
    @Test
    void isUserMayEdit_shouldReturnFalseIfUserNull() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assertions.assertFalse(campaign.isUserMayEdit(null));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return true if user superuser
     */
    @Test
    void isUserMayEdit_shouldReturnTrueIfUserSuperuser() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assertions.assertTrue(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return false if visibility not private
     */
    @Test
    void isUserMayEdit_shouldReturnFalseIfVisibilityNotPrivate() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setVisibility(CampaignVisibility.PUBLIC);
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assertions.assertFalse(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return false if boolean false
     */
    @Test
    void isUserMayEdit_shouldReturnFalseIfBooleanFalse() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assertions.assertFalse(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return false if userGroup not set
     */
    @Test
    void isUserMayEdit_shouldReturnFalseIfUserGroupNotSet() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        Assertions.assertFalse(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return true if user owner
     */
    @Test
    void isUserMayEdit_shouldReturnTrueIfUserOwner() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assertions.assertTrue(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return true if user member
     */
    @Test
    void isUserMayEdit_shouldReturnTrueIfUserMember() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assertions.assertNotNull(user);
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assertions.assertNotNull(userGroup);
        Role role = DataManager.getInstance().getDao().getRole(1);
        Assertions.assertNotNull(role);

        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(userGroup);
        campaign.getUserGroup().addMember(user, role);
        Assertions.assertTrue(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isGroupLimitActive()
     * @verifies return true if boolean true and userGroup not null
     */
    @Test
    void isGroupLimitActive_shouldReturnTrueIfBooleanTrueAndUserGroupNotNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        Assertions.assertTrue(campaign.isGroupLimitActive());
    }

    /**
     * @see Campaign#isGroupLimitActive()
     * @verifies return false if boolean false
     */
    @Test
    void isGroupLimitActive_shouldReturnFalseIfBooleanFalse() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(false);
        campaign.setUserGroup(new UserGroup());
        Assertions.assertFalse(campaign.isGroupLimitActive());
    }

    /**
     * @see Campaign#isGroupLimitActive()
     * @verifies return false if userGroup null
     */
    @Test
    void isGroupLimitActive_shouldReturnFalseIfUserGroupNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        Assertions.assertFalse(campaign.isGroupLimitActive());
    }

    /**
     * @see Campaign#getNumRecordsForStatus(String)
     * @verifies do record-based count correctly
     */
    @Test
    void getNumRecordsForStatus_shouldDoRecordbasedCountCorrectly() throws Exception {
        Campaign campaign = new Campaign();
        {
            CampaignRecordStatistic statistic = new CampaignRecordStatistic();
            statistic.setStatus(CrowdsourcingStatus.FINISHED);
            campaign.getStatistics().put("PI1", statistic);
        }
        {
            CampaignRecordStatistic statistic = new CampaignRecordStatistic();
            statistic.setStatus(CrowdsourcingStatus.FINISHED);
            campaign.getStatistics().put("PI2", statistic);
        }
        {
            CampaignRecordStatistic statistic = new CampaignRecordStatistic();
            statistic.setStatus(CrowdsourcingStatus.REVIEW);
            campaign.getStatistics().put("PI3", statistic);
        }
        Assertions.assertEquals(2, campaign.getNumRecordsForStatus(CrowdsourcingStatus.FINISHED.name()));
    }

    /**
     * @see Campaign#getNumRecordsForStatus(String)
     * @verifies do page-based count correctly
     */
    @Test
    void getNumRecordsForStatus_shouldDoPagebasedCountCorrectly() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setStatisticMode(StatisticMode.PAGE);
        {
            CampaignRecordStatistic statistic = new CampaignRecordStatistic();
            {
                CampaignRecordPageStatistic pageStatistic = new CampaignRecordPageStatistic();
                pageStatistic.setPage(1);
                pageStatistic.setStatus(CrowdsourcingStatus.FINISHED);
                statistic.getPageStatistics().put("PI1_1", pageStatistic);
            }
            {
                CampaignRecordPageStatistic pageStatistic = new CampaignRecordPageStatistic();
                pageStatistic.setPage(2);
                pageStatistic.setStatus(CrowdsourcingStatus.FINISHED);
                statistic.getPageStatistics().put("PI1_2", pageStatistic);
            }
            campaign.getStatistics().put("PI1", statistic);
        }
        {
            CampaignRecordStatistic statistic = new CampaignRecordStatistic();
            {
                CampaignRecordPageStatistic pageStatistic = new CampaignRecordPageStatistic();
                pageStatistic.setPage(1);
                pageStatistic.setStatus(CrowdsourcingStatus.FINISHED);
                statistic.getPageStatistics().put("PI2_1", pageStatistic);
            }
            campaign.getStatistics().put("PI2", statistic);
        }
        {
            CampaignRecordStatistic statistic = new CampaignRecordStatistic();
            {
                CampaignRecordPageStatistic pageStatistic = new CampaignRecordPageStatistic();
                pageStatistic.setPage(1);
                pageStatistic.setStatus(CrowdsourcingStatus.FINISHED);
                statistic.getPageStatistics().put("PI3_1", pageStatistic);
            }
            {
                CampaignRecordPageStatistic pageStatistic = new CampaignRecordPageStatistic();
                pageStatistic.setPage(2);
                pageStatistic.setStatus(CrowdsourcingStatus.REVIEW);
                statistic.getPageStatistics().put("PI3_2", pageStatistic);
            }
            campaign.getStatistics().put("PI3", statistic);
        }
        Assertions.assertEquals(4, campaign.getNumRecordsForStatus(CrowdsourcingStatus.FINISHED.name()));
        Assertions.assertEquals(1, campaign.getNumRecordsForStatus(CrowdsourcingStatus.REVIEW.name()));
    }

    @Test
    void testIsRecordStatus() {
        Campaign campaign = Mockito.spy(Campaign.class);
        campaign.setStatisticMode(StatisticMode.PAGE);
        Map<String, CampaignRecordStatistic> statisticsMap = new HashMap<>();
        CampaignRecordStatistic recordStatistic = new CampaignRecordStatistic();
        recordStatistic.setTotalPages(2);
        Map<String, CampaignRecordPageStatistic> pageStatistics = new HashMap<>();
        statisticsMap.put("PI1", recordStatistic );
        campaign.setStatistics(statisticsMap );

        assertTrue(campaign.isRecordStatus("PI1", CrowdsourcingStatus.ANNOTATE));
        assertFalse(campaign.isRecordStatus("PI1", CrowdsourcingStatus.REVIEW));
        campaign.setRecordPageStatus("PI1", 1, CrowdsourcingStatus.REVIEW, Optional.empty());
        assertTrue(campaign.isRecordStatus("PI1", CrowdsourcingStatus.ANNOTATE));
        assertTrue(campaign.isRecordStatus("PI1", CrowdsourcingStatus.REVIEW));
        campaign.setRecordPageStatus("PI1", 2, CrowdsourcingStatus.REVIEW, Optional.empty());
        assertFalse(campaign.isRecordStatus("PI1", CrowdsourcingStatus.ANNOTATE));
        assertTrue(campaign.isRecordStatus("PI1", CrowdsourcingStatus.REVIEW));

    }
}
