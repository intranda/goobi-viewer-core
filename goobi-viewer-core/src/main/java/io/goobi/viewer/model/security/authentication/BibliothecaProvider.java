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
package io.goobi.viewer.model.security.authentication;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.authentication.model.BibliothecaAuthenticationRequest;
import io.goobi.viewer.model.security.authentication.model.BibliothecaAuthenticationResponse;
import io.goobi.viewer.model.security.user.User;

/**
 * <p>
 * BibliothecaProvider class.
 * </p>
 */
public class BibliothecaProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(BibliothecaProvider.class);

    /**
     * <p>
     * Constructor for XServiceProvider.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param image a {@link java.lang.String} object.
     * @param timeoutMillis a long.
     */
    public BibliothecaProvider(String name, String label, String url, String image, long timeoutMillis) {
        super(name, label, TYPE_USER_PASSWORD, url, image, timeoutMillis);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<LoginResult> login(String readerId, String password) throws AuthenticationProviderException {
        StringBuilder sbUrl = new StringBuilder();

        BibliothecaAuthenticationRequest request = new BibliothecaAuthenticationRequest(readerId, password);
        sbUrl.append(url).append("&sno=").append(request.getUsername()).append("&pwd=").append(request.getPassword());
        String[] resp = NetTools.callUrlGET(sbUrl.toString());

        LoginResult result = new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), null, true);
        if ("200".equals(resp[0])) {
            try {
                BibliothecaAuthenticationResponse response = new BibliothecaAuthenticationResponse(resp[1], Helper.DEFAULT_ENCODING);
                Optional<User> user = getUser(request, response);
                result = new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), user, !response.isValid());
            } catch (JDOMException e) {
                logger.error(e.getMessage(), e);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return CompletableFuture.completedFuture(result);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#logout()
     */
    /** {@inheritDoc} */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsNicknameChange()
     */
    @Override
    public boolean allowsNicknameChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    @Override
    public boolean allowsEmailChange() {
        return false;
    }

    /**
     * Retrieves or creates User object for the given credentials.
     * 
     * @param request
     * @param response
     * @return User object
     * @throws AuthenticationProviderException
     */
    private static Optional<User> getUser(BibliothecaAuthenticationRequest request, BibliothecaAuthenticationResponse response)
            throws AuthenticationProviderException {
        if (request == null || StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword()) || response == null
                || StringUtils.isEmpty(response.getUserid())) {
            return Optional.empty();
        }

        User user = null;
        try {
            user = DataManager.getInstance().getDao().getUserByNickname(response.getUserid());
            if (user != null) {
                logger.debug("Found user {} via Bibliotheca user name '{}'.", user.getId(), request.getUsername());
            }
            // If not found, try email
            if (user == null) {
                String email = DEFAULT_EMAIL.replace("{username}", request.getUsername());
                user = DataManager.getInstance().getDao().getUserByEmail(email);
                if (user != null) {
                    logger.debug("Found user {} via Bibliotheca email '{}'.", user.getId(), email);
                }
            }

            // If still not found, create a new user
            if (user == null) {
                user = new User();
                user.setNickName(request.getUsername());
                user.setActive(true);
                user.setEmail(DEFAULT_EMAIL.replace("{username}", request.getUsername()));
                logger.trace("Created new user with nickname {}", request.getUsername());
            }

            //set user status
            //            if (!user.isSuspended()) {
            //                user.setSuspended(response.isExpired());
            //            }

            // Add to bean and persist
            if (user.getId() == null) {
                if (!DataManager.getInstance().getDao().addUser(user)) {
                    throw new AuthenticationProviderException("Could not add user to DB.");
                }
            } else {
                if (!DataManager.getInstance().getDao().updateUser(user)) {
                    throw new AuthenticationProviderException("Could not update user in DB.");
                }
            }

        } catch (DAOException e) {
            throw new AuthenticationProviderException(e);
        }
        return Optional.ofNullable(user);
    }
}
