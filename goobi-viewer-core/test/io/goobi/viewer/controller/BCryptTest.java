package io.goobi.viewer.controller;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class BCryptTest extends AbstractTest {

    /**
     * @see BCrypt#checkpw(String,String)
     * @verifies return true if passwords match
     */
    @Test
    public void checkpw_shouldReturnTrueIfPasswordsMatch() throws Exception {
        Assert.assertTrue(new BCrypt().checkpw("foobar", "$2a$10$riYEc4vydN5ksUpw/c9e0uV643f4qRyeQ2u.NpXW1FOgI4JnIn5dy"));
    }

    /**
     * @see BCrypt#checkpw(String,String)
     * @verifies return false if passwords dont match
     */
    @Test
    public void checkpw_shouldReturnFalseIfPasswordsDontMatch() throws Exception {
        Assert.assertFalse(new BCrypt().checkpw("barfoo", "$2a$10$riYEc4vydN5ksUpw/c9e0uV643f4qRyeQ2u.NpXW1FOgI4JnIn5dy"));
    }
}