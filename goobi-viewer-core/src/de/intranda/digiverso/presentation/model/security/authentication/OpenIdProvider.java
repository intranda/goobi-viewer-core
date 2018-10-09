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
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.servlets.openid.OAuthServlet;

public class OpenIdProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenIdProvider.class);

    public static final String TYPE_OPENID = "openId";
    
    /** OAuth client ID. */
    private String clientId;
    /** OAuth client secret. */
    private String clientSecret;

    private String oAuthState = null;
    private String oAuthAccessToken = null;
    private JSONObject jsonResponse = null;
    private volatile LoginResult loginResult = null;
    /**
     * Lock to be opened once login is completed
     */
    private Object responseLock = new Object();

    public OpenIdProvider(String name, String url, String image, long timeoutMillis, String clientId, String clientSecret) {
        super(name, TYPE_OPENID, url, image, timeoutMillis);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }


    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    @Override
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {

        // Apache Oltu
        try {
            oAuthState =
                    new StringBuilder(String.valueOf(System.nanoTime())).append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).toString();
            OAuthClientRequest request = null;
            switch (getName().toLowerCase()) {
                case "google":
                    request = OAuthClientRequest.authorizationProvider(OAuthProviderType.GOOGLE)
                            .setResponseType(ResponseType.CODE.name().toLowerCase())
                            .setClientId(getClientId())
                            .setRedirectURI(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + OAuthServlet.URL)
                            .setState(oAuthState)
                            .setScope("openid email")
                            .buildQueryMessage();
                    break;
                case "facebook":
                    request = OAuthClientRequest.authorizationProvider(OAuthProviderType.FACEBOOK)
                            .setClientId(getClientId())
                            .setRedirectURI(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + OAuthServlet.URL)
                            .setState(oAuthState)
                            .setScope("email")
                            .buildQueryMessage();
                    break;
                default:
                    // Other providers
                    request = OAuthClientRequest.authorizationLocation(getUrl())
                            .setResponseType(ResponseType.CODE.name().toLowerCase())
                            .setClientId(getClientId())
                            .setRedirectURI(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + OAuthServlet.URL)
                            .setState(oAuthState)
                            .setScope("email")
                            .buildQueryMessage();
                    break;
            }

            DataManager.getInstance().getOAuthResponseListener().register(this);
            if (request != null) {
                BeanUtils.getResponse().sendRedirect(request.getLocationUri());
            }
                return CompletableFuture.supplyAsync(() -> {
                            synchronized (responseLock) {
                                try {
                                    responseLock.wait(getTimeoutMillis());
                                    return this.loginResult;
                                } catch (InterruptedException e) {
                                    return new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), new AuthenticationProviderException(e));
                                }
                            }
                });

        } catch (OAuthSystemException e) {
            throw new AuthenticationProviderException(e);
        } catch (IOException e) {
            throw new AuthenticationProviderException(e);
        }
    }

    /**
     * Tries to find or create a valid {@link User} based on the given json object. Generates a {@link LoginResult}
     * containing the given request and response and either an optional containing the user or nothing if no user was found, or a {@link AuthenticationProviderException} 
     * if an internal error occured during login
     * If this method is not called within {@link #getTimeoutMillis()} ms after calling {@#login(String, String)}, 
     * a loginResponse is created containing an appropriate exception.
     * In any case, the future returned by {@link #login(String, String)} is resolved
     * 
     * @param json  The server response as json object. If null, the login request is resolved as failure
     * @param request
     * @param response
     */
    public Future<Boolean> completeLogin(JSONObject json, HttpServletRequest request, HttpServletResponse response) {
        try {
            if(json == null) {
                throw new AuthenticationProviderException("received no json object");
            }
            String email = null;
            String sub = null;
            switch (getName().toLowerCase()) {
                case "google":
                    // Validate id_token
                    String iss = (String) json.get("iss");
                    if (!"accounts.google.com".equals(iss)) {
                        logger.error("Google id_token validation failed - 'iss' value: " + iss);
                        break;
                    }
                    String aud = (String) json.get("aud");
                    if (!getClientId().equals(aud)) {
                        logger.error("Google id_token validation failed - 'aud' value: " + aud);
                        break;
                    }
                    email = (String) json.get("email");
                    sub = (String) json.get("sub");
                    break;
                case "facebook":
                    email = (String) json.get("email");
                    sub = (String) json.get("id");
                    break;
                default:
                    email = (String) json.get("email");
                    sub = (String) json.get("sub");
                    break;
            }
            User user = null;
            if (email != null) {
                String comboSub = getName().toLowerCase() + ":" + sub;
                // Retrieve user by sub
                if (sub != null) {
                    user = DataManager.getInstance().getDao().getUserByOpenId(comboSub);
                    if (user != null) {
                        logger.debug("Found user {} via OAuth sub '{}'.", user.getId(), comboSub);
                    }
                }
                // If not found, try email
                if (user == null) {
                    user = DataManager.getInstance().getDao().getUserByEmail(email);
                    if (user != null && sub != null) {
                        user.getOpenIdAccounts().add(comboSub);
                        logger.info("Updated user {} - added OAuth sub '{}'.", user.getId(), comboSub);
                    }
                }
                // If still not found, create a new user
                if (user == null) {
                    user = new User();
                    user.setActive(true);
                    user.setEmail(email);
                    if (sub != null) {
                        user.getOpenIdAccounts().add(sub);
                    }
                    logger.debug("Created new user.");
                }
                // Add to bean and persist
                if (user.getId() == null) {
                    if (!DataManager.getInstance().getDao().addUser(user)) {
                        logger.error("Could not add user to DB.");
                    }
                } else {
                    if (!DataManager.getInstance().getDao().updateUser(user)) {
                        logger.error("Could not update user in DB.");
                    }
                }
            }
            this.loginResult = new LoginResult(request, response, Optional.ofNullable(user), false);
        } catch (DAOException e) {
            this.loginResult = new LoginResult(request, response, new AuthenticationProviderException(e));
        } catch (AuthenticationProviderException e) {
            this.loginResult = new LoginResult(request, response, e);
        } finally {
            synchronized (responseLock) {
                responseLock.notifyAll();
            }
        }
        return this.loginResult.isRedirected(getTimeoutMillis());
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
     * @return the oAuthState
     */
    public String getoAuthState() {
        return oAuthState;
    }

    /**
     * @param oAuthState the oAuthState to set
     */
    public void setoAuthState(String oAuthState) {
        this.oAuthState = oAuthState;
    }

    /**
     * @return the oAuthAccessToken
     */
    public String getoAuthAccessToken() {
        return oAuthAccessToken;
    }

    /**
     * @param oAuthAccessToken the oAuthAccessToken to set
     */
    public void setoAuthAccessToken(String oAuthAccessToken) {
        this.oAuthAccessToken = oAuthAccessToken;
    }

    /**
     * @return the jsonResponse
     */
    private Optional<JSONObject> getJsonResponse() {
        return Optional.ofNullable(jsonResponse);
    }

}
