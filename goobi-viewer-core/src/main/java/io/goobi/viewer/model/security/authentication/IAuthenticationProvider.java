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
 * Interface of all user authentication related actions, particularly logging in and out of a viewer user account.
 *
 * @author Florian Alpers
 */
public interface IAuthenticationProvider {

    /**
     * Returns an unique name for the authentication provider implementation.
     *
     * @return The name of the provider
     */
    public String getName();

    /**
     * Returns a future containing the login result upon completion. The result optionally contains the logged in
     * {@link io.goobi.viewer.model.security.user.User} as well as the {@link jakarta.servlet.http.HttpServletRequest} and
     * {@link jakarta.servlet.http.HttpServletResponse} to be used to complete the login and possible request forwarding If an error occurs and the
     * request can not be processed, an {@link io.goobi.viewer.exceptions.AuthenticationException} must be thrown. If a login has been refused, the
     * exact reasons can be determined using the methods {@link io.goobi.viewer.model.security.user.User#isActive},
     * {@link io.goobi.viewer.model.security.user.User#isSuspended} and {@link io.goobi.viewer.model.security.authentication.LoginResult#isRefused}
     *
     * @param password A string to be used as a password or similar for login. If the provider does not require such a string, this can be left empty
     *            or null
     * @param loginName login name or identifier supplied by the user
     * @return A {@link java.util.concurrent.CompletableFuture} which is resolved once login is completed and contains a
     *         {@link io.goobi.viewer.model.security.authentication.LoginResult}
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     */
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException;

    /**
     * Logs the user out.
     *
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     */
    public void logout() throws AuthenticationProviderException;

    /**
     * Checks whether this authentication service allows user to edit their password or to reset it.
     *
     * @return true if the authentication service provides means to change or reset the user password
     */
    public boolean allowsPasswordChange();

    /**
     * The provider type. This should either be "local", "userpassword" or "openId". This value is used to determine where this provider is displayed.
     *
     * <p>Providers with the same type are displayed together
     *
     * @return The type of the provider
     */
    public String getType();

    /**
     * allowsNicknameChange.
     *
     * @return true if the nickname may be changed and is not essential for user identification
     */
    public boolean allowsNicknameChange();

    /**
     * allowsEmailChange.
     *
     * @return true if the email may be changed and is not essential for user identification
     */
    public boolean allowsEmailChange();

    /**
     * getAddUserToGroups.
     *
     * @return list of group names to add the user to on login
     */
    public List<String> getAddUserToGroups();

    /**
     * setAddUserToGroups.
     *
     * @param addUserToGroups group names to assign new or returning users to
     */
    public void setAddUserToGroups(List<String> addUserToGroups);

    /**

     */
    public String getRedirectUrl();

    /**
     * @param redirectUrl the redirectUrl to set
     */
    public void setRedirectUrl(String redirectUrl);
}
