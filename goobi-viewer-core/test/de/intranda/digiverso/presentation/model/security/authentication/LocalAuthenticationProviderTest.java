/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.security.authentication;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.controller.BCrypt;

/**
 * @author Florian Alpers
 *
 */
public class LocalAuthenticationProviderTest extends AbstractDatabaseEnabledTest {

    LocalAuthenticationProvider provider;
    
    String userActive_nickname = "nick 1";
    String userActive_email = "1@users.org";
    String userActive_pwHash = "abcdef1";
    
    String userSuspended_nickname = "nick 3";
    String userSuspended_email = "3@users.org";
    String userSuspended_pwHash = "abcdef3";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        provider = new LocalAuthenticationProvider("Goobi viewer");
        provider.setBcrypt(new BCrypt() {
            @Override
            public boolean checkpw(String a, String b) {
                return a.equals(b);
            }
        });
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLogin_valid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future =  provider.login(userActive_email, userActive_pwHash);
        Assert.assertTrue(future.get().getUser().isPresent());
        Assert.assertTrue(future.get().getUser().get().isActive());
        Assert.assertFalse(future.get().getUser().get().isSuspended());
    }
    
    @Test
    public void testLogin_invalid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future =  provider.login(userActive_email, userSuspended_pwHash);
        Assert.assertTrue(future.get().getUser().isPresent());
        Assert.assertTrue(future.get().isRefused());
    }
    
    @Test
    public void testLogin_unknown() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future =  provider.login(userActive_email + "test", userActive_pwHash);
        Assert.assertFalse(future.get().getUser().isPresent());
    }
    
    @Test
    public void testLogin_suspended() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future =  provider.login(userSuspended_email, userSuspended_pwHash);
        Assert.assertTrue(future.get().getUser().isPresent());
        Assert.assertTrue(future.get().getUser().get().isSuspended());
    }

}
