package io.goobi.viewer.managedbeans;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.solr.SolrConstants;

public class CmsCollectionsBeanTest extends AbstractSolrEnabledTest {

    /**
     * @see CmsCollectionsBean#isDisplayTranslationWidget()
     * @verifies return false if solrField not among configured translation groups
     */
    @Test
    public void isDisplayTranslationWidget_shouldReturnFalseIfSolrFieldNotAmongConfiguredTranslationGroups() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        bean.solrField = "MD_NOPE"; // Do not use the setter, that'd require more test infrastructure
        Assert.assertFalse(bean.isDisplayTranslationWidget());
    }

    /**
     * @see CmsCollectionsBean#isDisplayTranslationWidget()
     * @verifies return false if solrField values fully translated
     */
    @Test
    public void isDisplayTranslationWidget_shouldReturnFalseIfSolrFieldValuesFullyTranslated() throws Exception {
        CmsCollectionsBean bean = new CmsCollectionsBean();
        bean.solrField = SolrConstants.DC; // Do not use the setter, that'd require more test infrastructure
        Assert.assertFalse(bean.isDisplayTranslationWidget());
    }
}