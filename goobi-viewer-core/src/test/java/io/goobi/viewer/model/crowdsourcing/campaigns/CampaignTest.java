package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.time.LocalDateTime;

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
        Assert.assertEquals(2019, campaign.getDateEnd().getYear());
        Assert.assertEquals(9, campaign.getDateEnd().getMonthValue());
        Assert.assertEquals(1, campaign.getDateEnd().getDayOfMonth());
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
        Assert.assertEquals(2020, campaign.getDateEnd().getYear());
        Assert.assertEquals(8, campaign.getDateEnd().getMonthValue());
        Assert.assertEquals(31, campaign.getDateEnd().getDayOfMonth());
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
    }

    /**
     * @see Campaign#isHasStarted()
     * @verifies return true if dateStart equals now
     */
    @Test
    public void isHasStarted_shouldReturnTrueIfDateStartEqualsNow() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setDateStart(LocalDateTime.now());
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
        Assert.assertFalse(campaign.isHasStarted());
    }
}