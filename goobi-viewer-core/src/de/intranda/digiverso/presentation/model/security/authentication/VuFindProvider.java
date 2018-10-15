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

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
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
            LoginResult result = new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), user, !this.authenticationResponse.getUser().getIsValid());
            return CompletableFuture.completedFuture(result);
        } catch (URISyntaxException e) {
            throw new AuthenticationProviderException("Cannot resolve authentication api url " + getUrl(), e);
        } catch(WebApplicationException e) {
            throw new AuthenticationProviderException("Error requesting authorizazion for user " + loginName, e);
        }
    }


    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    protected VuAuthenticationResponse post(URI url, VuAuthenticationRequest request) throws WebApplicationException{
        Client client = ClientBuilder.newClient();
        try {            
            client.property(ClientProperties.CONNECT_TIMEOUT, (int)getTimeoutMillis());
            client.property(ClientProperties.READ_TIMEOUT, (int)getTimeoutMillis());
            WebTarget vuFindAuthenticationApi = client.target(url);
            Entity<VuAuthenticationRequest> ent = Entity.entity(request, MediaType.APPLICATION_JSON);
            VuAuthenticationResponse response = vuFindAuthenticationApi.request().post(ent, VuAuthenticationResponse.class);
            return response;
        } catch(ClientErrorException e) {
            logger.debug("Authentication request returned error " + e.toString());
            VuAuthenticationResponse.User user = new VuAuthenticationResponse.User();
            VuAuthenticationResponse response = new VuAuthenticationResponse();
            response.setUser(user);
            if(e instanceof ForbiddenException || e instanceof NotAllowedException || e instanceof NotAuthorizedException) {
                user.setExists(null);
                user.setIsValid(false);
            } else if(e instanceof NotFoundException) {
                user.setExists(false);
                user.setIsValid(false);
            } else {
                throw e;
            }
            return response;
        } finally {
            client.close();
        }
    }

    /**
     * @param request
     * @param response
     * @return
     * @throws AuthenticationProviderException
     */
    private Optional<User> getUser(VuAuthenticationRequest request) throws AuthenticationProviderException {

        if (request == null || StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword()) || !Boolean.TRUE.equals(authenticationResponse.getUser().getExists())) {
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
            user.setEmail(DEFAULT_EMAIL.replace("{username}",request.getUsername()));
            logger.debug("Created new user with nickname " + request.getUsername());
        }

        //set user status
        if(!user.isSuspended()) {            
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
        
        //add to user group
        if(authenticationResponse.getUser().getGroup() != null && StringUtils.isNotBlank(authenticationResponse.getUser().getGroup().getDesc())) {
            UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(authenticationResponse.getUser().getGroup().getDesc());
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
