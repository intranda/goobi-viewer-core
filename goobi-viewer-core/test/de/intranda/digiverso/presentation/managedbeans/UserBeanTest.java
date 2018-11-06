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
package de.intranda.digiverso.presentation.managedbeans;


import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.controller.BCrypt;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.security.authentication.AuthenticationProviderException;
import de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider;
import de.intranda.digiverso.presentation.model.security.authentication.LoginResult;
import de.intranda.digiverso.presentation.model.security.user.User;

/**
 * @author Florian Alpers
 *
 */
public class UserBeanTest extends AbstractDatabaseEnabledTest {

    UserBean bean = new UserBean();
    
    String userActive_nickname = "nick 1";
    String userActive_email = "1@users.org";
    String userActive_pwHash = "abcdef1";
    
    String userSuspended_nickname = "nick 3";
    String userSuspended_email = "3@users.org";
    String userSuspended_pwHash = "abcdef3";
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        bean.setAuthenticationProvider(new IAuthenticationProvider() {
            
            private User user = null;
            
            @Override
            public void logout() throws AuthenticationProviderException {}
            
            @Override
            public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
                LoginResult result;
                try {
                    user = DataManager.getInstance().getDao().getUserByEmail(loginName);
                    if(user != null && user.getPasswordHash().equals(password)) {
                        result = new LoginResult(null, null, Optional.ofNullable(user), false);
                    } else {
                        result = new LoginResult(null, null, Optional.empty(), true);
                    }
                } catch (DAOException e) {
                    throw new AuthenticationProviderException(e);
                }
                return CompletableFuture.completedFuture(result);
            }
            
            @Override
            public String getType() {
                return "test";
            }
            
            @Override
            public String getName() {
                return "test";
            }
            
            @Override
            public boolean allowsPasswordChange() {
                return false;
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
    public void testLogin_valid() throws IllegalStateException, AuthenticationProviderException, InterruptedException, ExecutionException {
        
        bean.setEmail(userActive_email);
        bean.setPassword(userActive_pwHash); 
        Assert.assertNull(bean.getUser());
        bean.login();
        Assert.assertNotNull(bean.getUser());
        Assert.assertTrue(bean.getUser().isActive());
        Assert.assertFalse(bean.getUser().isSuspended());
    }
    
    @Test
    public void testLogin_invalid() throws IllegalStateException, AuthenticationProviderException, InterruptedException, ExecutionException {

        bean.setEmail(userActive_email);
        bean.setPassword(userSuspended_pwHash); 
        Assert.assertNull(bean.getUser());
        bean.login();
        Assert.assertNull(bean.getUser());
    }
    
    @Test
    public void testLogin_unknown() throws IllegalStateException, AuthenticationProviderException, InterruptedException, ExecutionException {

        bean.setEmail(userActive_email + "test");
        bean.setPassword(userActive_pwHash); 
        Assert.assertNull(bean.getUser());
        bean.login();
        Assert.assertNull(bean.getUser());
    }
    
    @Test
    public void testLogin_suspended() throws IllegalStateException, AuthenticationProviderException, InterruptedException, ExecutionException {

        bean.setEmail(userSuspended_email);
        bean.setPassword(userSuspended_pwHash); 
        Assert.assertNull(bean.getUser());
        bean.login();
        Assert.assertNull(bean.getUser());
//        Assert.assertTrue(bean.getUser().isActive());
//        Assert.assertTrue(bean.getUser().isSuspended());
    }



}
