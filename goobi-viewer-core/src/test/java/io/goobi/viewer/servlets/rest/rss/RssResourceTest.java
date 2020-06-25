package io.goobi.viewer.servlets.rest.rss;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.servlets.rest.rss.RssResource;

public class RssResourceTest extends AbstractTest {

    /**
     * @see RssResource#createQuery(String,Long,String)
     * @verifies augment given query correctly
     */
    @Test
    public void createQuery_shouldAugmentGivenQueryCorrectly() throws Exception {
        Assert.assertEquals("(PI:*)", RssResource.createQuery("PI:*", null, null, null, false));
    }

    /**
     * @see RssResource#createQuery(String,Long,String)
     * @verifies create basic query correctly
     */
    @Test
    public void createQuery_shouldCreateBasicQueryCorrectly() throws Exception {
        Assert.assertEquals("(ISWORK:true)", RssResource.createQuery(null, null, null, null, false));
    }

    /**
     * @see RssResource#createQuery(String,Long,String,HttpServletRequest,boolean)
     * @verifies add suffixes if requested
     */
    @Test
    public void createQuery_shouldAddSuffixesIfRequested() throws Exception {
        Assert.assertEquals("(PI:*) -BOOL_HIDE:true -DC:collection1 -DC:collection2", RssResource.createQuery("PI:*", null, null, null, true));
        Assert.assertEquals("(PI:*)", RssResource.createQuery("PI:*", null, null, null, false));
    }
}