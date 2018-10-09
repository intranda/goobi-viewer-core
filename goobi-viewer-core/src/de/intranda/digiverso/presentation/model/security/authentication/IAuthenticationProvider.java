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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.intranda.digiverso.presentation.exceptions.AuthenticationException;
import de.intranda.digiverso.presentation.model.security.user.User;

/**
 * Interface of all user authentication related actions, particularly logging in and out of a viewer user account
 * 
 * @author Florian Alpers
 *
 */
public interface IAuthenticationProvider {

    /**
     * Returns an unique name for the authentication provider implementation
     * @return  The name of the provider
     */
    public String getName();
    
    /**
     * Returns a future containing the login result upon completion. The result optionally contains the logged in {@link User} 
     * as well as the {@link HttpServletRequest} and {@link HttpServletResponse} to be used to complete the login and possible request forwarding
     * If an error occurs and the request can not be processed, an {@link AuthenticationException} must be thrown.
     * If a login has been refused, the exact reasons can be determined using the methods 
     * {@link isActive}, {@link isSuspended} and {@link isRefused}
     *
     * @param loginName  A string used to identify the user to the provider, e.g. a user name or email address. 
     * If the provider does not require such a string, this can be left empty or null
     * @param password  A string to be used as a password or similar for login.
     * If the provider does not require such a string, this can be left empty or null
     * @return  A {@link CompletableFuture} which is resolved once login is completed and contains a {@link LoginResult}
     * @throws AuthenticationProviderException  If an error occurred during login evaluation 
     * or if the request could not be processed
     */
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException;
    
    /**
     * Logs the user out
     * 
     * @throws AuthenticationProviderException  if an error occurred when processing the logout request
     */
    public void logout() throws AuthenticationProviderException;
    
    /**
     * Check whether this authentication service allows user to edit their password or to reset it
     * 
     * @return true if the authentication service provides means to change or reset the user password
     */
    public boolean allowsPasswordChange();
    
    /**
     * The provider type. This should either be "local", "userpassword" or "openId".
     * This value is used to determine where this provider is displayed. Providers with the same type are displayed together
     * 
     * @return  The type of the provider
     */
    public String getType();
}
