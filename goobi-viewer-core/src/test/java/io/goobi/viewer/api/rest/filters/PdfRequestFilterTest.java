package io.goobi.viewer.api.rest.filters;

import org.junit.Assert;
import org.junit.Test;

public class PdfRequestFilterTest {
    
    /**
     * @see PdfRequestFilter#getNumAllowedPages(int,int)
     * @verifies return 0 if percentage 0
     */
    @Test
    public void getNumAllowedPages_shouldReturn0IfPercentage0() throws Exception {
        Assert.assertEquals(0, PdfRequestFilter.getNumAllowedPages(0, 10));
    }

    /**
     * @see PdfRequestFilter#getNumAllowedPages(int,int)
     * @verifies return 0 if number of pages 0
     */
    @Test
    public void getNumAllowedPages_shouldReturn0IfNumberOfPages0() throws Exception {
        Assert.assertEquals(0, PdfRequestFilter.getNumAllowedPages(50, 0));
    }

    /**
     * @see PdfRequestFilter#getNumAllowedPages(int,int)
     * @verifies return number of pages if percentage 100
     */
    @Test
    public void getNumAllowedPages_shouldReturnNumberOfPagesIfPercentage100() throws Exception {
        Assert.assertEquals(10, PdfRequestFilter.getNumAllowedPages(100, 10));
    }

    /**
     * @see PdfRequestFilter#getNumAllowedPages(int,int)
     * @verifies calculate number correctly
     */
    @Test
    public void getNumAllowedPages_shouldCalculateNumberCorrectly() throws Exception {
        Assert.assertEquals(35, PdfRequestFilter.getNumAllowedPages(35, 100));
        Assert.assertEquals(3, PdfRequestFilter.getNumAllowedPages(35, 10));
        Assert.assertEquals(1, PdfRequestFilter.getNumAllowedPages(19, 10));
        Assert.assertEquals(0, PdfRequestFilter.getNumAllowedPages(9, 10));
    }
}