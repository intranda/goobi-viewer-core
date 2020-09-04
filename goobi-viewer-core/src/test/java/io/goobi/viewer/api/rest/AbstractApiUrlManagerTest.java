package io.goobi.viewer.api.rest;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class AbstractApiUrlManagerTest extends AbstractTest {

    /**
     * @see AbstractApiUrlManager#replaceApiPathParams(String,Object[])
     * @verifies remove trailing slash if file name contains period
     */
    @Test
    public void replaceApiPathParams_shouldRemoveTrailingSlashIfFileNameContainsPeriod() throws Exception {
        Assert.assertEquals("http://example.com/with space/info.json",
                AbstractApiUrlManager.replaceApiPathParams("http://example.com/with space/info.json/", new Object[] {}));
    }
}