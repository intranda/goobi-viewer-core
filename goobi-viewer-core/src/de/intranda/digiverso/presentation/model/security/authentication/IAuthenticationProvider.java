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
     * Returns an optional containing the viewer user if the login succeeded, or an empty optional if the login was rejected
     * If an error occurs and the request can not be processed, an {@link AUthenticationException} must be thrown.
     * If a login has been refused, the exact reasons can be determined using the methods 
     * {@link isActive}, {@link isSuspended} and {@link isRefused}
     *
     * @param loginName  A string used to identify the user to the provider, e.g. a user name or email address. 
     * If the provider does not require such a string, this can be left empty or null
     * @param password  A string to be used as a password or similar for login.
     * If the provider does not require such a string, this can be left empty or null
     * @return  The user if login was successful, an empty optional if login was refused. 
     * @throws AuthenticationProviderException  If an error occurred during login evaluation 
     * or if the request could not be processed
     */
    public Optional<User> login(String loginName, String password) throws AuthenticationProviderException;
    
    /**
     * Logs the user out
     * 
     * @throws AuthenticationProviderException  if an error occurred when processing the logout request
     */
    public void logout() throws AuthenticationProviderException;
    
    /**
     * Checks whether the user exists as an active account
     * 
     * @return  true if the user exists and has an active account
     */
    public boolean isActive();

    /**
     * Checks whether an active user is suspended, i.e. disallowed to log in
     * 
     * @return  true if the user exists but is suspended
     */
    public boolean isSuspended();
    
    /**
     * Checks whether an existing and not suspended user was blocked from logging in, usually because of
     * invalid login data (password, or ip range etc.)
     * 
     * @return  true if the user exists and is not suspended, but was blocked from logging in, usually because of
     * invalid login data (password, or ip range etc.)
     */
    public boolean isRefused();
    
    /**
     * If the authentication service provides a user group for the user, it is returned as an optional. 
     * Otherwise, an empty optional is returned
     * 
     * @return  An optional containing a possible user group provided by the authentication service
     */
    public Optional<String> getUserGroup();
    
    /**
     * Check whether this authentication service allows user to edit their password or to reset it
     * 
     * @return true if the authentication service provides means to change or reset the user password
     */
    public boolean allowsPasswordChange();
    
    
}
