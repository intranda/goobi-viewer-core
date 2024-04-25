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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.openid.OAuthServlet;

/**
 * <p>
 * OpenIdProvider class.
 * </p>
 */
public class OpenIdProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LogManager.getLogger(OpenIdProvider.class);

    /** Constant <code>TYPE_OPENID="openId"</code> */
    public static final String TYPE_OPENID = "openId";

    /** OAuth client ID. */
    private String clientId;
    /** OAuth client secret. */
    private String clientSecret;
    /** Token endpoint URI. */
    private String tokenEndpoint = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + OAuthServlet.URL;
    /** OpenID servlet URI. Not to be confused with <code>HttpAuthenticationProvider.redirectUrl</code> */
    private String redirectionEndpoint = url + "/token";
    /** Scope. */
    private String scope = "openid email";

    private String oAuthState = null;
    private String oAuthAccessToken = null;
    private volatile LoginResult loginResult = null; //NOSONAR   LoginResult is immutable, so thread-savety is guaranteed

    /**
     * Lock to be opened once login is completed
     */
    private Object responseLock = new Object();

    /**
     * <p>
     * Constructor for OpenIdProvider.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param image a {@link java.lang.String} object.
     * @param timeoutMillis a long.
     * @param clientId a {@link java.lang.String} object.
     * @param clientSecret a {@link java.lang.String} object.
     */
    public OpenIdProvider(String name, String label, String url, String image, long timeoutMillis, String clientId, String clientSecret) {
        super(name, label, TYPE_OPENID, url, image, timeoutMillis);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * <p>
     * Getter for the field <code>clientId</code>.
     * </p>
     *
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * <p>
     * Getter for the field <code>clientSecret</code>.
     * </p>
     *
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @return the tokenEndpoint
     */
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * @param tokenEndpoint the tokenEndpoint to set
     * @return this
     */
    public OpenIdProvider setTokenEndpoint(String tokenEndpoint) {
        if (tokenEndpoint != null) {
            this.tokenEndpoint = tokenEndpoint;
        }
        return this;
    }

    /**
     * @return the redirectionEndpoint
     */
    public String getRedirectionEndpoint() {
        return redirectionEndpoint;
    }

    /**
     * @param redirectionEndpoint the redirectionEndpoint to set
     * @return this
     */
    public OpenIdProvider setRedirectionEndpoint(String redirectionEndpoint) {
        if (redirectionEndpoint != null) {
            this.redirectionEndpoint = redirectionEndpoint;
        }
        return this;
    }

    /**
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * @param scope the scope to set
     * @return this
     */
    public OpenIdProvider setScope(String scope) {
        if (scope != null) {
            this.scope = scope;
        }
        return this;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
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
                            .setRedirectURI(redirectionEndpoint)
                            .setState(oAuthState)
                            .setScope(scope)
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
                        //                        long startTime = System.currentTimeMillis();
                        //                        while (System.currentTimeMillis() - startTime < getTimeoutMillis()) {
                        responseLock.wait(getTimeoutMillis());
                        //                        }
                        return this.loginResult;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), new AuthenticationProviderException(e));
                    }
                }
            });

        } catch (IOException | OAuthSystemException e) {
            throw new AuthenticationProviderException(e);
        }
    }

    /**
     * Tries to find or create a valid {@link io.goobi.viewer.model.security.user.User} based on the given json object. Generates a
     * {@link io.goobi.viewer.model.security.authentication.LoginResult} containing the given request and response and either an optional containing
     * the user or nothing if no user was found, or a {@link io.goobi.viewer.model.security.authentication.AuthenticationProviderException} if an
     * internal error occured during login If this method is not called within {@link #getTimeoutMillis()} ms after calling {@#login(String, String)},
     * a loginResponse is created containing an appropriate exception. In any case, the future returned by {@link #login(String, String)} is resolved
     *
     * @param json The server response as json object. If null, the login request is resolved as failure
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a {@link java.util.concurrent.Future} object.
     */
    public Future<Boolean> completeLogin(JSONObject json, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (json == null) {
                throw new AuthenticationProviderException("received no json object");
            }
            String email = null;
            String sub = null;
            switch (getName().toLowerCase()) {
                case "google":
                    // Validate id_token
                    if (!json.has("iss")) {
                        logger.error("Google id_token validation failed - 'iss' value missing");
                        break;
                    }
                    if (!"accounts.google.com".equals(json.get("iss"))) {
                        logger.error("Google id_token validation failed - 'iss' value: {}", json.get("iss"));
                        break;
                    }
                    if (!json.has("aud")) {
                        logger.error("Google id_token validation failed - 'aud' value missing");
                        break;
                    }
                    if (!getClientId().equals(json.get("aud"))) {
                        logger.error("Google id_token validation failed - 'aud' value: {}", json.get("aud"));
                        break;
                    }
                    if (json.has("email")) {
                        email = (String) json.get("email");
                    }
                    if (json.has("sub")) {
                        sub = (String) json.get("sub");
                    }
                    break;
                case "facebook":
                    if (json.has("email")) {
                        email = (String) json.get("email");
                    }
                    if (json.has("sub")) {
                        sub = (String) json.get("sub");
                    }
                    break;
                default:
                    if (json.has("email")) {
                        email = (String) json.get("email");
                    }
                    if (json.has("sub")) {
                        sub = (String) json.get("sub");
                    }
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
                        user.getOpenIdAccounts().add(comboSub);
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

    /**
     * <p>
     * Getter for the field <code>oAuthState</code>.
     * </p>
     *
     * @return the oAuthState
     */
    public String getoAuthState() {
        return oAuthState;
    }

    /**
     * <p>
     * Setter for the field <code>oAuthState</code>.
     * </p>
     *
     * @param oAuthState the oAuthState to set
     */
    public void setoAuthState(String oAuthState) {
        this.oAuthState = oAuthState;
    }

    /**
     * <p>
     * Getter for the field <code>oAuthAccessToken</code>.
     * </p>
     *
     * @return the oAuthAccessToken
     */
    public String getoAuthAccessToken() {
        return oAuthAccessToken;
    }

    /**
     * <p>
     * Setter for the field <code>oAuthAccessToken</code>.
     * </p>
     *
     * @param oAuthAccessToken the oAuthAccessToken to set
     */
    public void setoAuthAccessToken(String oAuthAccessToken) {
        this.oAuthAccessToken = oAuthAccessToken;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsNicknameChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsNicknameChange() {
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsEmailChange() {
        return false;
    }

}
