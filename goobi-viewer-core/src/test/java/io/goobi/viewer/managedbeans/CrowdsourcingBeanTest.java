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
package io.goobi.viewer.managedbeans;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * @author florian
 *
 */
public class CrowdsourcingBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    private CrowdsourcingBean bean = new CrowdsourcingBean();

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        UserBean userBean = new UserBean();
        userBean.setUser(DataManager.getInstance().getDao().getUser(1l));
        bean.userBean = userBean;
        bean.init();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetCampaignCount() throws DAOException {
        long numPublic = bean.getCampaignCount(CampaignVisibility.PUBLIC);
        long numPrivate = bean.getCampaignCount(CampaignVisibility.PRIVATE);
        Assert.assertEquals(1, numPublic);
        Assert.assertEquals(1, numPrivate);
    }

    @Test
    public void testGetAllCampaigns() throws DAOException {
        List<Campaign> campaigns = bean.getAllCampaigns();
        Assert.assertEquals(2, campaigns.size());

    }

    @Test
    public void testSaveSelectedCampaign() throws DAOException, PresentationException, IndexUnreachableException {
        bean.setSelectedCampaignId("1");
        Assert.assertNotNull(bean.getSelectedCampaign());

        LocalDateTime created = LocalDateTime.now();
        bean.getSelectedCampaign().setDateCreated(created);
        Assert.assertEquals("Date created does not match after setting", created, bean.getSelectedCampaign().getDateCreated());
        bean.saveSelectedCampaignAction();

        bean.setSelectedCampaignId("1");
        Assert.assertEquals("Date created does not match in database", created, bean.getSelectedCampaign().getDateCreated());
    }

    /**
     * @see CrowdsourcingBean#getAllowedCampaigns(User,List)
     * @verifies return all public campaigns if user not logged in
     */
    @Test
    public void getAllowedCampaigns_shouldReturnAllPublicCampaignsIfUserNotLoggedIn() throws Exception {
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
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Long.valueOf(1), result.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), result.get(1).getId());
    }

    /**
     * @see CrowdsourcingBean#getAllowedCampaigns(User,List)
     * @verifies return private campaigns within time period if user not logged in
     */
    @Test
    public void getAllowedCampaigns_shouldReturnPrivateCampaignsWithinTimePeriodIfUserNotLoggedIn() throws Exception {
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
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(3), result.get(0).getId());
    }

    /**
     * @see CrowdsourcingBean#getAllowedCampaigns(User,List)
     * @verifies return private campaigns within time period if user logged in
     */
    @Test
    public void getAllowedCampaigns_shouldReturnPrivateCampaignsWithinTimePeriodIfUserLoggedIn() throws Exception {
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
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(Long.valueOf(3), result.get(0).getId());
        Assert.assertEquals(Long.valueOf(5), result.get(1).getId());
        Assert.assertEquals(Long.valueOf(7), result.get(2).getId());
    }

}
