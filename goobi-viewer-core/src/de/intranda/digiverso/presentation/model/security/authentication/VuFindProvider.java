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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.Role;
import de.intranda.digiverso.presentation.model.security.authentication.model.VuAuthenticationRequest;
import de.intranda.digiverso.presentation.model.security.authentication.model.VuAuthenticationResponse;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.security.user.UserGroup;

/**
 * @author Florian Alpers
 *
 */
public class VuFindProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(VuFindProvider.class);
    private static final String DEFAULT_EMAIL = "{username}@nomail.com";
    public static final String TYPE_USER_PASSWORD = "userPassword";
    private static final String USER_GROUP_ROLE_MEMBER = "member";
    
    private VuAuthenticationResponse authenticationResponse;

    /**
     * @param name
     * @param url
     * @param image
     */
    public VuFindProvider(String name, String url, String image, long timeoutMillis) {
        super(name, TYPE_USER_PASSWORD, url, image, timeoutMillis);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    @Override
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
        try {
            VuAuthenticationRequest request = new VuAuthenticationRequest(loginName, password);
            this.authenticationResponse = post(new URI(getUrl()), request);

            Optional<User> user = getUser(request);
            LoginResult result = new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), user);
            return CompletableFuture.completedFuture(result);
        } catch (URISyntaxException e) {
            throw new AuthenticationProviderException("Cannot resolve authentication api url " + getUrl(), e);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isActive()
     */
    @Override
    public boolean isActive() {
        if (authenticationResponse != null) {
            return Boolean.TRUE.equals(authenticationResponse.getUser().getExists());
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isSuspended()
     */
    @Override
    public boolean isSuspended() {
        if (authenticationResponse != null) {
            return !Boolean.TRUE.equals(authenticationResponse.getUser().getIsValid())
                    || Boolean.TRUE.equals(authenticationResponse.getExpired().getIsExpired());
        } else {
            return true;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#isRefused()
     */
    @Override
    public boolean isRefused() {
        if (authenticationResponse != null && authenticationResponse.getBlocks() != null) {
            return Boolean.TRUE.equals(authenticationResponse.getBlocks().getIsBlocked());
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#getUserGroup()
     */
    @Override
    public Optional<String> getUserGroup() {
        if (authenticationResponse != null) {
            return Optional.ofNullable(authenticationResponse.getUser().getGroup());
        } else {
            return Optional.empty();
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    private VuAuthenticationResponse post(URI url, VuAuthenticationRequest request) {
        Client client = ClientBuilder.newClient();
        WebTarget vuFindAuthenticationApi = client.target(url);

        Entity<VuAuthenticationRequest> ent = Entity.entity(request, MediaType.APPLICATION_JSON);
        VuAuthenticationResponse response = vuFindAuthenticationApi.request().post(ent, VuAuthenticationResponse.class);
        return response;
    }

    /**
     * @param request
     * @param response
     * @return
     * @throws AuthenticationProviderException
     */
    private Optional<User> getUser(VuAuthenticationRequest request) throws AuthenticationProviderException {

        if (request == null || !isActive() || isSuspended() || isRefused()) {
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
            user.setActive(true);
            user.setNickName(request.getUsername());
            user.setEmail(DEFAULT_EMAIL.replace("{username}",request.getUsername()));
            logger.debug("Created new user with nickname " + request.getUsername());
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
        
        //add to user group
        if(StringUtils.isNotBlank(authenticationResponse.getUser().getGroup())) {
            UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(authenticationResponse.getUser().getGroup());
            if(userGroup != null && !userGroup.getMembers().contains(user)) {
//                DataManager.getInstance().getDao().updateUserGroup(userGroup);
                Role role = DataManager.getInstance().getDao().getRole(USER_GROUP_ROLE_MEMBER);
                if(role != null) {                    
                    userGroup.addMember(user, role);
                }
            }
        }
        
        } catch(DAOException | PresentationException e) {
            throw new AuthenticationProviderException(e);
        }
        return Optional.ofNullable(user);
    }

}
