package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

public class CampaignTest extends AbstractTest {

    /**
     * @see Campaign#getDaysLeft()
     * @verifies return -1 if no dateEnd
     */
    @Test
    public void getDaysLeft_shouldReturn1IfNoDateEnd() throws Exception {
        Campaign campaign = new Campaign();
        Assert.assertEquals(-1, campaign.getDaysLeft());
    }

    /**
     * @see Campaign#getDaysLeft()
     * @verifies calculate days correctly
     */
    @Test
    public void getDaysLeft_shouldCalculateDaysCorrectly() throws Exception {
        Campaign campaign = new Campaign();
        {
            LocalDateTime later = LocalDateTime.now().plusDays(99);
            campaign.setDateEnd(later);
            Assert.assertEquals(99, campaign.getDaysLeft());
        }
        {
            LocalDateTime earlier = LocalDateTime.now().plusDays(-20);
            campaign.setDateEnd(earlier);
            Assert.assertEquals(0, campaign.getDaysLeft());
        }
    }

    /**
     * @see Campaign#getDaysBeforeStart()
     * @verifies return -1 if no dateStart
     */
    @Test
    public void getDaysBeforeStart_shouldReturn1IfNoDateStart() throws Exception {
        Campaign campaign = new Campaign();
        Assert.assertEquals(-1, campaign.getDaysBeforeStart());
    }

    /**
     * @see Campaign#getDaysBeforeStart()
     * @verifies calculate days correctly
     */
    @Test
    public void getDaysBeforeStart_shouldCalculateDaysCorrectly() throws Exception {
        Campaign campaign = new Campaign();
        {
            LocalDateTime later = LocalDateTime.now().plusDays(15);
            campaign.setDateStart(later);
            Assert.assertEquals(15, campaign.getDaysBeforeStart());
        }
        {
            LocalDateTime earlier = LocalDateTime.now().plusDays(-20);
            campaign.setDateStart(earlier);
            Assert.assertEquals(0, campaign.getDaysBeforeStart());
        }
    }

    /**
     * @see Campaign#isHasEnded()
     * @verifies return false if dateEnd null
     */
    @Test
    public void isHasEnded_shouldReturnFalseIfDateEndNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setTimePeriodEnabled(true);
        Assert.assertFalse(campaign.isHasEnded());
    }

    /**
     * @see Campaign#isHasEnded()
     * @verifies return true if dateEnd before now
     */
    @Test
    public void isHasEnded_shouldReturnTrueIfDateEndBeforeNow() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime earlier = LocalDateTime.now().plusDays(-20);
        campaign.setDateEnd(earlier);
        campaign.setTimePeriodEnabled(true);
        Assert.assertTrue(campaign.isHasEnded());
    }

    /**
     * @see Campaign#isHasEnded()
     * @verifies return false if dateEnd after now
     */
    @Test
    public void isHasEnded_shouldReturnFalseIfDateEndAfterNow() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime later = LocalDateTime.now().plusDays(20);
        campaign.setDateEnd(later);
        campaign.setTimePeriodEnabled(true);
        Assert.assertFalse(campaign.isHasEnded());
    }

    /**
     * @see Campaign#isHasEnded()
     * @verifies return false if timePeriodEnabled false
     */
    @Test
    public void isHasEnded_shouldReturnFalseIfTimePeriodEnabledFalse() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime earlier = LocalDateTime.now().plusDays(-20);
        campaign.setDateEnd(earlier);
        Assert.assertFalse(campaign.isHasEnded());
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if dateStart null
     */
    @Test
    public void isHasStarted_shouldReturnTrueIfDateStartNull() throws Exception {
        Campaign campaign = new Campaign();
        Assert.assertTrue(campaign.isHasStarted());
        campaign.setTimePeriodEnabled(true);
        campaign.setTimePeriodEnabled(true);
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if dateStart equals now
     */
    @Test
    public void isHasStarted_shouldReturnTrueIfDateStartEqualsNow() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setDateStart(LocalDateTime.now());
        campaign.setTimePeriodEnabled(true);
        Assert.assertTrue(campaign.isHasStarted());
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if dateStart before now
     */
    @Test
    public void isHasStarted_shouldReturnTrueIfDateStartBeforeNow() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime later = LocalDateTime.now().plusDays(-20);
        campaign.setDateStart(later);
        campaign.setTimePeriodEnabled(true);
        Assert.assertTrue(campaign.isHasStarted());
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return false if dateStart after now
     */
    @Test
    public void isHasStarted_shouldReturnFalseIfDateStartAfterNow() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime later = LocalDateTime.now().plusDays(20);
        campaign.setDateStart(later);
        campaign.setTimePeriodEnabled(true);
        Assert.assertFalse(campaign.isHasStarted());
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if timePeriodEnabled false
     */
    @Test
    public void isHasStarted_shouldReturnTrueIfTimePeriodEnabledFalse() throws Exception {
        Campaign campaign = new Campaign();
        LocalDateTime later = LocalDateTime.now().plusDays(-20);
        campaign.setDateStart(later);
        campaign.setTimePeriodEnabled(true);
        Assert.assertTrue(campaign.isHasStarted());
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CampaignRecordStatus)
     * @verifies return true if campaign public
     */
    @Test
    public void isUserAllowedAction_shouldReturnTrueIfCampaignPublic() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setVisibility(CampaignVisibility.PUBLIC);
        Assert.assertTrue(campaign.isUserAllowedAction(null, CampaignRecordStatus.ANNOTATE));
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CampaignRecordStatus)
     * @verifies return false if outside time period
     */
    @Test
    public void isUserAllowedAction_shouldReturnFalseIfOutsideTimePeriod() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        campaign.setTimePeriodEnabled(true);
        campaign.setDateStart(LocalDateTime.of(2000, 01, 01, 0, 0));
        campaign.setDateEnd(LocalDateTime.of(2001, 01, 01, 0, 0));
        Assert.assertFalse(campaign.isUserAllowedAction(user, CampaignRecordStatus.ANNOTATE));
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CampaignRecordStatus)
     * @verifies return true if user owner of group
     */
    @Test
    public void isUserAllowedAction_shouldReturnTrueIfUserOwnerOfGroup() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assert.assertTrue(campaign.isUserAllowedAction(user, CampaignRecordStatus.ANNOTATE));
    }

    /**
     * @see Campaign#isUserAllowedAction(User,CampaignRecordStatus)
     * @verifies return false if user not in group
     */
    @Test
    public void isUserAllowedAction_shouldReturnFalseIfUserNotInGroup() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        Assert.assertFalse(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return false if user null
     */
    @Test
    public void isUserMayEdit_shouldReturnFalseIfUserNull() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assert.assertFalse(campaign.isUserMayEdit(null));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return true if user superuser
     */
    @Test
    public void isUserMayEdit_shouldReturnTrueIfUserSuperuser() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assert.assertTrue(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return false if visibility not private
     */
    @Test
    public void isUserMayEdit_shouldReturnFalseIfVisibilityNotPrivate() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setVisibility(CampaignVisibility.PUBLIC);
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assert.assertFalse(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return false if boolean false
     */
    @Test
    public void isUserMayEdit_shouldReturnFalseIfBooleanFalse() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assert.assertFalse(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return false if userGroup not set
     */
    @Test
    public void isUserMayEdit_shouldReturnFalseIfUserGroupNotSet() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        Assert.assertFalse(campaign.isUserMayEdit(user));
    }

    /**
     * @see Campaign#isUserMayEdit(User)
     * @verifies return true if user owner
     */
    @Test
    public void isUserMayEdit_shouldReturnTrueIfUserOwner() throws Exception {
        User user = new User();
        Campaign campaign = new Campaign();
        campaign.setLimitToGroup(true);
        campaign.setUserGroup(new UserGroup());
        campaign.getUserGroup().setOwner(user);
        Assert.assertTrue(campaign.isUserMayEdit(user));
    }
}