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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.authentication.model.XServiceAuthenticationRequest;
import de.intranda.digiverso.presentation.model.security.authentication.model.XServiceAuthenticationResponse;
import de.intranda.digiverso.presentation.model.security.user.User;

public class XServiceProvider extends VuFindProvider {

    private static final Logger logger = LoggerFactory.getLogger(XServiceProvider.class);

    public XServiceProvider(String name, String url, String image, long timeoutMillis) {
        super(name, url, image, timeoutMillis);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    @Override
    public CompletableFuture<LoginResult> login(String borID, String password) throws AuthenticationProviderException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(url).append("&bor_id=").append(borID).append("&verification=").append(password);

        XServiceAuthenticationRequest request = new XServiceAuthenticationRequest(borID, password);
        String[] resp = Helper.callUrlGET(sbUrl.toString());

        LoginResult result = new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), null, true);
        if ("200".equals(resp[0])) {
            try {
                XServiceAuthenticationResponse response = new XServiceAuthenticationResponse(resp[1], Helper.DEFAULT_ENCODING);
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
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
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
    private static Optional<User> getUser(XServiceAuthenticationRequest request, XServiceAuthenticationResponse response)
            throws AuthenticationProviderException {
        if (request == null || StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword()) || response == null
                || StringUtils.isEmpty(response.getId())) {
            return Optional.empty();
        }

        User user = null;
        try {
            user = DataManager.getInstance().getDao().getUserByNickname(request.getUsername());
            if (user != null) {
                logger.debug("Found user {} via X-Service user name '{}'.", user.getId(), request.getUsername());
            }
            // If not found, try email
            if (user == null) {
                user = DataManager.getInstance().getDao().getUserByEmail(request.getUsername());
                if (user != null) {
                    logger.debug("Found user {} via X-Service user name '{}'.", user.getId(), request.getUsername());
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
