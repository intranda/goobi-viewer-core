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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of all user authentication related actions, particularly logging in and out of a viewer user account
 *
 * @author Florian Alpers
 */
public interface IAuthenticationProvider {

    /**
     * Returns an unique name for the authentication provider implementation
     *
     * @return The name of the provider
     */
    public String getName();

    /**
     * Returns a future containing the login result upon completion. The result optionally contains the logged in
     * {@link io.goobi.viewer.model.security.user.User} as well as the {@link javax.servlet.http.HttpServletRequest} and
     * {@link javax.servlet.http.HttpServletResponse} to be used to complete the login and possible request forwarding If an error occurs and the
     * request can not be processed, an {@link io.goobi.viewer.exceptions.AuthenticationException} must be thrown. If a login has been refused, the
     * exact reasons can be determined using the methods {@link io.goobi.viewer.model.security.user.User#isActive},
     * {@link io.goobi.viewer.model.security.user.User#isSuspended} and {@link io.goobi.viewer.model.security.authentication.LoginResult#isRefused}
     *
     * @param password A string to be used as a password or similar for login. If the provider does not require such a string, this can be left empty
     *            or null
     * @return A {@link java.util.concurrent.CompletableFuture} which is resolved once login is completed and contains a
     *         {@link io.goobi.viewer.model.security.authentication.LoginResult}
     * @param loginName a {@link java.lang.String} object.
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     */
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException;

    /**
     * Logs the user out
     *
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     */
    public void logout() throws AuthenticationProviderException;

    /**
     * Check whether this authentication service allows user to edit their password or to reset it
     *
     * @return true if the authentication service provides means to change or reset the user password
     */
    public boolean allowsPasswordChange();

    /**
     * The provider type. This should either be "local", "userpassword" or "openId". This value is used to determine where this provider is displayed.
     * Providers with the same type are displayed together
     *
     * @return The type of the provider
     */
    public String getType();

    /**
     * <p>
     * allowsNicknameChange.
     * </p>
     *
     * @return true if the nickname may be changed and is not essential for user identification
     */
    public boolean allowsNicknameChange();

    /**
     * <p>
     * allowsEmailChange.
     * </p>
     *
     * @return true if the email may be changed and is not essential for user identification
     */
    public boolean allowsEmailChange();

    /**
     * <p>
     * getAddUserToGroups.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getAddUserToGroups();

    /**
     * <p>
     * setAddUserToGroups.
     * </p>
     *
     * @param addUserToGroups a {@link java.util.List} object.
     */
    public void setAddUserToGroups(List<String> addUserToGroups);

    /**
     * @return the redirectUrl
     */
    public String getRedirectUrl();

    /**
     * @param redirectUrl the redirectUrl to set
     */
    public void setRedirectUrl(String redirectUrl);
}
