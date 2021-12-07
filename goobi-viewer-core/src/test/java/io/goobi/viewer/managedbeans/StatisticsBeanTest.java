package io.goobi.viewer.managedbeans;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;

public class StatisticsBeanTest extends AbstractSolrEnabledTest {
    
    /**
     * @see StatisticsBean#getTopStructTypesByNumber()
     * @verifies return list of docstruct types
     */
    @Test
    public void getTopStructTypesByNumber_shouldReturnListOfDocstructTypes() throws Exception {
        StatisticsBean bean = new StatisticsBean();
        List<String> result = bean.getTopStructTypesByNumber();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    /**
     * @see StatisticsBean#getImportedPages()
     * @verifies return a non zero number
     */
    @Test
    public void getImportedPages_shouldReturnANonZeroNumber() throws Exception {
        StatisticsBean bean = new StatisticsBean();
        Long num = bean.getImportedPages();
        Assert.assertNotNull(num);
        Assert.assertTrue(num > 0);
    }

    /**
     * @see StatisticsBean#getImportedFullTexts()
     * @verifies return a non zero number
     */
    @Test
    public void getImportedFullTexts_shouldReturnANonZeroNumber() throws Exception {
        StatisticsBean bean = new StatisticsBean();
        Long num = bean.getImportedFullTexts();
        Assert.assertNotNull(num);
        Assert.assertTrue(num > 0);
    }

    /**
     * @see StatisticsBean#isIndexEmpty()
     * @verifies return false if index online
     */
    @Test
    public void isIndexEmpty_shouldReturnFalseIfIndexOnline() throws Exception {
        StatisticsBean bean = new StatisticsBean();
        Assert.assertFalse(bean.isIndexEmpty());
    }
}