package io.goobi.viewer.model.crowdsourcing.campaigns;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class CampaignTest extends AbstractTest {

    /**
     * @see Campaign#setDateStartString(String)
     * @verifies parse string correctly
     */
    @Test
    public void setDateStartString_shouldParseStringCorrectly() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setDateEndString("2019-09-01");
        Assert.assertNotNull(campaign.getDateEnd());
        DateTime jodaDate = new DateTime(campaign.getDateEnd());
        Assert.assertEquals(2019, jodaDate.getYear());
        Assert.assertEquals(9, jodaDate.getMonthOfYear());
        Assert.assertEquals(1, jodaDate.getDayOfMonth());
    }

    /**
     * @see Campaign#setDateEndString(String)
     * @verifies parse string correctly
     */
    @Test
    public void setDateEndString_shouldParseStringCorrectly() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setDateEndString("2020-08-31");
        Assert.assertNotNull(campaign.getDateEnd());
        DateTime jodaDate = new DateTime(campaign.getDateEnd());
        Assert.assertEquals(2020, jodaDate.getYear());
        Assert.assertEquals(8, jodaDate.getMonthOfYear());
        Assert.assertEquals(31, jodaDate.getDayOfMonth());
    }

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
            MutableDateTime later = new MutableDateTime();
            later.addDays(99);
            campaign.setDateEnd(later.toDate());
            Assert.assertEquals(99, campaign.getDaysLeft());
        }
        {
            MutableDateTime earlier = new MutableDateTime();
            earlier.addDays(-20);
            campaign.setDateEnd(earlier.toDate());
            Assert.assertEquals(-20, campaign.getDaysLeft());
        }
    }
}