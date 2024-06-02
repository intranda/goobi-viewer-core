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
package io.goobi.viewer.managedbeans;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * @author florian
 *
 */
class CrowdsourcingBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    private CrowdsourcingBean bean = new CrowdsourcingBean();

    /** {@inheritDoc} */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        UserBean userBean = new UserBean();
        userBean.setUser(DataManager.getInstance().getDao().getUser(1l));
        bean.userBean = userBean;
        bean.init();
    }

    /** {@inheritDoc} */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testGetCampaignCount() throws DAOException {
        long numPublic = bean.getCampaignCount(CampaignVisibility.PUBLIC);
        long numPrivate = bean.getCampaignCount(CampaignVisibility.PRIVATE);
        Assertions.assertEquals(2, numPublic);
        Assertions.assertEquals(1, numPrivate);
    }

    @Test
    void testGetAllCampaigns() throws DAOException {
        List<Campaign> campaigns = bean.getAllCampaigns();
        Assertions.assertEquals(3, campaigns.size());

    }

    @Test
    void testSaveSelectedCampaign() throws DAOException, PresentationException, IndexUnreachableException {
        bean.setSelectedCampaignId("1");
        Assertions.assertNotNull(bean.getSelectedCampaign());

        LocalDateTime created = LocalDateTime.now();
        bean.getSelectedCampaign().setDateCreated(created);
        Assertions.assertEquals(created, bean.getSelectedCampaign().getDateCreated(), "Date created does not match after setting");
        bean.saveSelectedCampaignAction();

        bean.setSelectedCampaignId("1");
        Assertions.assertEquals(created, bean.getSelectedCampaign().getDateCreated(), "Date created does not match in database");
    }

    /**
     * @see CrowdsourcingBean#getAllowedCampaigns(User,List)
     * @verifies return all public campaigns if user not logged in
     */
    @Test
    void getAllowedCampaigns_shouldReturnAllPublicCampaignsIfUserNotLoggedIn() throws Exception {
        List<Campaign> allCampaigns = new ArrayList<>(3);
        {
            Campaign campaign = new Campaign();
            campaign.setId(1L);
            campaign.setVisibility(CampaignVisibility.PUBLIC);
            allCampaigns.add(campaign);
        }
        {
            Campaign campaign = new Campaign();
            campaign.setId(2L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            allCampaigns.add(campaign);
        }
        {
            Campaign campaign = new Campaign();
            campaign.setId(3L);
            campaign.setVisibility(CampaignVisibility.PUBLIC);
            allCampaigns.add(campaign);
        }

        List<Campaign> result = bean.getAllowedCampaigns(null, allCampaigns);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(Long.valueOf(1), result.get(0).getId());
        Assertions.assertEquals(Long.valueOf(3), result.get(1).getId());
    }

    /**
     * @see CrowdsourcingBean#getAllowedCampaigns(User,List)
     * @verifies return private campaigns within time period if user not logged in
     */
    @Test
    void getAllowedCampaigns_shouldReturnPrivateCampaignsWithinTimePeriodIfUserNotLoggedIn() throws Exception {
        List<Campaign> allCampaigns = new ArrayList<>(4);
        {
            // No time period
            Campaign campaign = new Campaign();
            campaign.setId(1L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            allCampaigns.add(campaign);
        }
        {
            // Expired time period
            Campaign campaign = new Campaign();
            campaign.setId(2L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2010, 1, 1, 0, 0));
            allCampaigns.add(campaign);
        }
        {
            // Current time period (update before 2323!)
            Campaign campaign = new Campaign();
            campaign.setId(3L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2323, 1, 1, 0, 0));
            campaign.setTimePeriodEnabled(true);
            allCampaigns.add(campaign);
        }
        {
            // Current time period, but boolean not set
            Campaign campaign = new Campaign();
            campaign.setId(4L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2323, 1, 1, 0, 0));
            allCampaigns.add(campaign);
        }

        List<Campaign> result = bean.getAllowedCampaigns(null, allCampaigns);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Long.valueOf(3), result.get(0).getId());
    }

    /**
     * @see CrowdsourcingBean#getAllowedCampaigns(User,List)
     * @verifies return private campaigns within time period if user logged in
     */
    @Test
    void getAllowedCampaigns_shouldReturnPrivateCampaignsWithinTimePeriodIfUserLoggedIn() throws Exception {
        User user = new User();
        List<Campaign> allCampaigns = new ArrayList<>(6);
        {
            // No time period (NOT ALLOWED)
            Campaign campaign = new Campaign();
            campaign.setId(1L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            allCampaigns.add(campaign);
        }
        {
            // Expired time period (NOT ALLOWED)
            Campaign campaign = new Campaign();
            campaign.setId(2L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2010, 1, 1, 0, 0));
            allCampaigns.add(campaign);
        }
        {
            // Current time period (ALLOWED)
            Campaign campaign = new Campaign();
            campaign.setId(3L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2323, 1, 1, 0, 0));
            campaign.setTimePeriodEnabled(true);
            allCampaigns.add(campaign);
        }
        {
            // Current time period, but boolean not set (NOT ALLOWED)
            Campaign campaign = new Campaign();
            campaign.setId(4L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2323, 1, 1, 0, 0));
            campaign.setUserGroup(new UserGroup());
            allCampaigns.add(campaign);
        }
        {
            // Current time period and user group, of which the user is owner (ALLOWED)
            Campaign campaign = new Campaign();
            campaign.setId(5L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2323, 1, 1, 0, 0));
            campaign.setTimePeriodEnabled(true);
            campaign.setUserGroup(new UserGroup());
            campaign.getUserGroup().setOwner(user);
            allCampaigns.add(campaign);
        }
        {
            // Current time period and user group, to which the user does not belong (NOT ALLOWED)
            Campaign campaign = new Campaign();
            campaign.setId(6L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2323, 1, 1, 0, 0));
            campaign.setTimePeriodEnabled(true);
            campaign.setUserGroup(new UserGroup());
            campaign.setLimitToGroup(true);
            allCampaigns.add(campaign);
        }
        {
            // Current time period and user group, to which the user does not belong, but boolean not set (ALLOWED)
            Campaign campaign = new Campaign();
            campaign.setId(7L);
            campaign.setVisibility(CampaignVisibility.PRIVATE);
            campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
            campaign.setDateEnd(LocalDateTime.of(2323, 1, 1, 0, 0));
            campaign.setTimePeriodEnabled(true);
            campaign.setUserGroup(new UserGroup());
            allCampaigns.add(campaign);
        }

        List<Campaign> result = bean.getAllowedCampaigns(user, allCampaigns);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(Long.valueOf(3), result.get(0).getId());
        Assertions.assertEquals(Long.valueOf(5), result.get(1).getId());
        Assertions.assertEquals(Long.valueOf(7), result.get(2).getId());
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return true for public campaigns
     */
    @Test
    void isAllowed_shouldReturnTrueForPublicCampaigns() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PUBLIC);
        Assertions.assertTrue(CrowdsourcingBean.isAllowed(null, campaign));
        Assertions.assertTrue(CrowdsourcingBean.isAllowed(new User(), campaign));
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return false if private campaign within time period but boolean false
     */
    @Test
    void isAllowed_shouldReturnFalseIfPrivateCampaignWithinTimePeriodButBooleanFalse() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PRIVATE);
        campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
        campaign.setDateEnd(LocalDateTime.now().plusMonths(3));
        Assertions.assertFalse(CrowdsourcingBean.isAllowed(null, campaign));
        Assertions.assertFalse(CrowdsourcingBean.isAllowed(new User(), campaign));
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return true if private campaign within time period and user null
     */
    @Test
    void isAllowed_shouldReturnTrueIfPrivateCampaignWithinTimePeriodAndUserNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PRIVATE);
        campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
        campaign.setDateEnd(LocalDateTime.now().plusMonths(3));
        campaign.setTimePeriodEnabled(true);
        Assertions.assertTrue(CrowdsourcingBean.isAllowed(null, campaign));
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return true if private campaign within time period and user not null
     */
    @Test
    void isAllowed_shouldReturnTrueIfPrivateCampaignWithinTimePeriodAndUserNotNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PRIVATE);
        campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
        campaign.setDateEnd(LocalDateTime.now().plusMonths(3));
        campaign.setTimePeriodEnabled(true);
        Assertions.assertTrue(CrowdsourcingBean.isAllowed(new User(), campaign));
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return false if private campaign outside time period
     */
    @Test
    void isAllowed_shouldReturnFalseIfPrivateCampaignOutsideTimePeriod() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PRIVATE);
        campaign.setDateStart(LocalDateTime.of(2000, 1, 1, 0, 0));
        campaign.setDateEnd(LocalDateTime.of(2010, 1, 1, 0, 0));
        campaign.setTimePeriodEnabled(true);
        Assertions.assertFalse(CrowdsourcingBean.isAllowed(null, campaign));
        Assertions.assertFalse(CrowdsourcingBean.isAllowed(new User(), campaign));
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return false if user group set and user null
     */
    @Test
    void isAllowed_shouldReturnFalseIfUserGroupSetAndUserNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PRIVATE);
        campaign.setUserGroup(new UserGroup());
        campaign.setLimitToGroup(true);
        Assertions.assertFalse(CrowdsourcingBean.isAllowed(null, campaign));
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return false if user group set and user not member
     */
    @Test
    void isAllowed_shouldReturnFalseIfUserGroupSetAndUserNotMember() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PRIVATE);
        campaign.setUserGroup(new UserGroup());
        campaign.setLimitToGroup(true);
        Assertions.assertFalse(CrowdsourcingBean.isAllowed(new User(), campaign));
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return true if user group set and user owner
     */
    @Test
    void isAllowed_shouldReturnTrueIfUserGroupSetAndUserOwner() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PRIVATE);
        campaign.setUserGroup(new UserGroup());
        User user = new User();
        campaign.getUserGroup().setOwner(user);
        campaign.setLimitToGroup(true);
        Assertions.assertTrue(CrowdsourcingBean.isAllowed(user, campaign));
    }

    /**
     * @see CrowdsourcingBean#isAllowed(User,Campaign)
     * @verifies return false if user group set but boolean false
     */
    @Test
    void isAllowed_shouldReturnFalseIfUserGroupSetButBooleanFalse() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setVisibility(CampaignVisibility.PRIVATE);
        campaign.setUserGroup(new UserGroup());
        User user = new User();
        campaign.getUserGroup().setOwner(user);
        Assertions.assertFalse(CrowdsourcingBean.isAllowed(user, campaign));
    }

    @Test
    void test_ItemOrderConfiguration() throws PresentationException, IndexUnreachableException, DAOException {
        IDAO dao = Mockito.mock(IDAO.class);
        Configuration config = Mockito.mock(Configuration.class);
        Campaign campaign = Mockito.spy(Campaign.class);
        CrowdsourcingBean bean = new CrowdsourcingBean(config, dao);
        bean.setTargetCampaign(campaign);

        UserBean userBean = new UserBean();
        userBean.setUser(DataManager.getInstance().getDao().getUser(1l));
        bean.userBean = userBean;
        bean.init();

        Mockito.when(config.getCrowdsourcingCampaignItemOrder()).thenReturn("fixed");
        bean.setNextIdentifierForAnnotation();
        Mockito.verify(campaign, Mockito.times(1)).getNextTarget(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(campaign, Mockito.times(0)).getRandomizedTarget(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.when(config.getCrowdsourcingCampaignItemOrder()).thenReturn("random");
        bean.setNextIdentifierForAnnotation();
        Mockito.verify(campaign, Mockito.times(1)).getRandomizedTarget(Mockito.any(), Mockito.any(), Mockito.any());
        //unchanged
        Mockito.verify(campaign, Mockito.times(1)).getNextTarget(Mockito.any(), Mockito.any(), Mockito.any());
    }
}
