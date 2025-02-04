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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.authentication.model.UserPasswordAuthenticationRequest;
import io.goobi.viewer.model.security.authentication.model.VuAuthenticationResponse;
import io.goobi.viewer.model.security.user.User;

/**
 * <p>
 * VuFindProvider class.
 * </p>
 *
 * @author Florian Alpers
 */
public class VuFindProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LogManager.getLogger(VuFindProvider.class);
    private static final String USER_GROUP_ROLE_MEMBER = "member";

    private VuAuthenticationResponse authenticationResponse;

    /**
     * <p>
     * Constructor for VuFindProvider.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param image a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     * @param timeoutMillis a long.
     */
    public VuFindProvider(String name, String label, String url, String image, long timeoutMillis) {
        super(name, label, TYPE_USER_PASSWORD, url, image, timeoutMillis);
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
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
        try {
            UserPasswordAuthenticationRequest request = new UserPasswordAuthenticationRequest(loginName, password);
            String response = post(new URI(getUrl()), serialize(request));
            this.authenticationResponse = deserialize(response);
            Optional<User> user = getUser(request);
            LoginResult result =
                    new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), user, !this.authenticationResponse.getUser().getIsValid());
            return CompletableFuture.completedFuture(result);
        } catch (URISyntaxException e) {
            throw new AuthenticationProviderException("Cannot resolve authentication api url " + getUrl(), e);
        } catch (IOException | WebApplicationException e) {
            throw new AuthenticationProviderException("Error requesting authorizazion for user " + loginName, e);
        }
    }

    private static String serialize(UserPasswordAuthenticationRequest object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

    private static VuAuthenticationResponse deserialize(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper.readValue(json, VuAuthenticationResponse.class);
    }

    /**
     * @param request
     * @return Optional<User>
     * @throws AuthenticationProviderException
     */
    private Optional<User> getUser(UserPasswordAuthenticationRequest request) throws AuthenticationProviderException {

        if (request == null || StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword())
                || !Boolean.TRUE.equals(authenticationResponse.getUser().getExists())) {
            return Optional.empty();
        }

        User user = null;
        try {
            user = DataManager.getInstance().getDao().getUserByNickname(request.getUsername());
            if (user != null) {
                logger.debug("Found user {} via vuFind username '{}'.", user.getId(), request.getUsername());
            }
            // If not found, try email
            if (user == null) {
                user = DataManager.getInstance().getDao().getUserByEmail(request.getUsername());
                if (user != null) {
                    logger.debug("Found user {} via vuFind username '{}'.", user.getId(), request.getUsername());
                }
            }

            // If still not found, create a new user
            if (user == null) {
                user = new User();
                user.setNickName(request.getUsername());
                user.setActive(true);
                user.setEmail(DEFAULT_EMAIL.replace("{username}", request.getUsername()));
                logger.debug("Created new user with nickname {}", request.getUsername());
            }

            // set user status
            if (!user.isSuspended()) {
                user.setSuspended(!authenticationResponse.getUser().getIsValid());
            }

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

            // Add user to user group contained in the VuFind response
            if (authenticationResponse.getUser().getGroup() != null
                    && StringUtils.isNotBlank(authenticationResponse.getUser().getGroup().getDesc())) {
                String userGroupName = authenticationResponse.getUser().getGroup().getDesc();
                //                UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(userGroupName);
                //                if (userGroup != null && !userGroup.getMembers().contains(user)) {
                //                    //                DataManager.getInstance().getDao().updateUserGroup(userGroup);
                //                    Role role = DataManager.getInstance().getDao().getRole(USER_GROUP_ROLE_MEMBER);
                //                    if (role != null) {
                //                        userGroup.addMember(user, role);
                //                    }
                //                }
                if (!addUserToGroups.contains(userGroupName)) {
                    addUserToGroups.add(userGroupName);
                }
            }
        } catch (DAOException e) {
            throw new AuthenticationProviderException(e);
        }
        return Optional.ofNullable(user);
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
    /** {@inheritDoc} */
    @Override
    public boolean allowsNicknameChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsEmailChange() {
        return true;
    }

}
