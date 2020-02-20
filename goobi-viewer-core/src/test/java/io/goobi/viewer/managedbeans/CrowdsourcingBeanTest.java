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

import java.util.Date;
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

/**
 * @author florian
 *
 */
public class CrowdsourcingBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    private CrowdsourcingBean bean = new CrowdsourcingBean();

    /**
     * @throws java.lang.Exception
     */
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
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetCampaignCount() throws DAOException {
        long numPublic = bean.getCampaignCount(CampaignVisibility.PUBLIC);
        long numPrivate = bean.getCampaignCount(CampaignVisibility.PRIVATE);
        long numRestricted = bean.getCampaignCount(CampaignVisibility.RESTRICTED);
        Assert.assertEquals(1, numPublic);
        Assert.assertEquals(1, numPrivate);
        Assert.assertEquals(0, numRestricted);
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

        Date created = new Date();
        bean.getSelectedCampaign().setDateCreated(created);
        Assert.assertEquals("Date created does not match after setting", created, bean.getSelectedCampaign().getDateCreated());
        bean.saveSelectedCampaign();

        bean.setSelectedCampaignId("1");
        Assert.assertEquals("Date created does not match in database", created, bean.getSelectedCampaign().getDateCreated());
    }

}
