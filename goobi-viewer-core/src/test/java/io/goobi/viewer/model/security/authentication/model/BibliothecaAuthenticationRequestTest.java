package io.goobi.viewer.model.security.authentication.model;

import org.junit.Assert;
import org.junit.Test;

public class BibliothecaAuthenticationRequestTest {

    /**
     * @see BibliothecaAuthenticationRequest#normalizeUsername(String)
     * @verifies normalize value correctly
     */
    @Test
    public void normalizeUsername_shouldNormalizeValueCorrectly() throws Exception {
        Assert.assertEquals("00001234567", BibliothecaAuthenticationRequest.normalizeUsername("1234567"));
    }
}