package io.goobi.viewer.model.security;


import org.junit.Assert;
import org.junit.Test;

public class CopyrightIndicatorStatusTest {
    
    /**
    * @see CopyrightIndicatorStatus#getByName(String)
    * @verifies return correct value
    */
    @Test
    public void getByName_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(CopyrightIndicatorStatus.OPEN, CopyrightIndicatorStatus.getByName("OPEN"));
        Assert.assertEquals(CopyrightIndicatorStatus.PARTIAL, CopyrightIndicatorStatus.getByName("PARTIAL"));
        Assert.assertEquals(CopyrightIndicatorStatus.LOCKED, CopyrightIndicatorStatus.getByName("LOCKED"));
    }
}