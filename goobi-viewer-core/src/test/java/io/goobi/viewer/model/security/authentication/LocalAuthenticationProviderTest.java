/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.security.authentication;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.security.authentication.AuthenticationProviderException;
import io.goobi.viewer.model.security.authentication.LocalAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.LoginResult;

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
    @BeforeEach
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
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void testLogin_valid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        DataManager.getInstance().getSecurityManager().reset();
        CompletableFuture<LoginResult> future = provider.login(userActive_email, userActive_pwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().getUser().get().isActive());
        Assertions.assertFalse(future.get().getUser().get().isSuspended());
    }

    @Test
    public void testLogin_invalid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        DataManager.getInstance().getSecurityManager().reset();
        CompletableFuture<LoginResult> future = provider.login(userActive_email, userSuspended_pwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().isRefused());
    }

    @Test
    public void testLogin_unknown() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        DataManager.getInstance().getSecurityManager().reset();
        CompletableFuture<LoginResult> future = provider.login(userActive_email + "test", userActive_pwHash);
        Assertions.assertFalse(future.get().getUser().isPresent());
    }

    @Test
    public void testLogin_suspended() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        DataManager.getInstance().getSecurityManager().reset();
        CompletableFuture<LoginResult> future = provider.login(userSuspended_email, userSuspended_pwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().getUser().get().isSuspended());
    }

}
