package io.goobi.viewer.model.termbrowsing;

import org.junit.Assert;
import org.junit.Test;

public class BrowseTermTest {
    
    /**
     * @see BrowseTerm#addToHitCount(int)
     * @verifies add to hit count correctly
     */
    @Test
    public void addToHitCount_shouldAddToHitCountCorrectly() throws Exception {
        BrowseTerm browseTerm = new BrowseTerm("foo", null, null);
        Assert.assertEquals(1, browseTerm.getHitCount());
        
        browseTerm.addToHitCount(1);
        Assert.assertEquals(2, browseTerm.getHitCount());
        
        browseTerm.addToHitCount(2);
        Assert.assertEquals(4, browseTerm.getHitCount());
    }
}