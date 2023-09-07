package io.goobi.viewer.servlets;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.servlets.oembed.OEmbedRecord;

public class OEmbedServletTest extends AbstractTest {

    /**
     * @see OEmbedServlet#parseUrl(String)
     * @verifies parse url with page number correctly
     */
    @Test
    public void parseUrl_shouldParseUrlWithPageNumberCorrectly() throws Exception {
        OEmbedRecord rec = OEmbedServlet.parseUrl("/image/PPN517154005/2/");
        Assert.assertNotNull(rec);
        Assert.assertNotNull(rec.getPhysicalElement());
        Assert.assertEquals("PPN517154005", rec.getPhysicalElement().getPi());
        Assert.assertEquals(2, rec.getPhysicalElement().getOrder());

    }

    /**
     * @see OEmbedServlet#parseUrl(String)
     * @verifies parse url without page number correctly
     */
    @Test
    public void parseUrl_shouldParseUrlWithoutPageNumberCorrectly() throws Exception {
        OEmbedRecord rec = OEmbedServlet.parseUrl("/image/PPN517154005/");
        Assert.assertNotNull(rec);
        Assert.assertNotNull(rec.getPhysicalElement());
        Assert.assertEquals(1, rec.getPhysicalElement().getOrder());
    }
}