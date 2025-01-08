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
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.auth0.jwt.interfaces.DecodedJWT;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;

/**
 * <p>
 * OpenIdProvider class.
 * </p>
 */
public class OpenIdProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LogManager.getLogger(OpenIdProvider.class);

    /** Constant <code>TYPE_OPENID="openId"</code> */
    public static final String TYPE_OPENID = "openId";

    /** OAuth discovery URI. */
    private String discoveryUri;
    /** OAuth client ID. */
    private String clientId;
    /** OAuth client secret. */
    private String clientSecret;
    /** Token endpoint URI. */
    private String tokenEndpoint;
    /** OpenID servlet URI. Not to be confused with <code>HttpAuthenticationProvider.redirectUrl</code> */
    private String redirectionEndpoint =
            BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/api/v1" + ApiUrls.AUTH + ApiUrls.AUTH_OAUTH;
    /** OAuth parameter jwks_uri. */
    private String jwksUri;
    /** OAuth parameter scope. */
    private String scope = "openid email";
    /** OAuth parameter response_type. */
    private String responseType = "code";
    /** OAuth parameter response_mode. */
    private String responseMode;
    /** OAuth parameter issuer. */
    private String issuer;
    /** Token check delay (milliseconds). */
    private long tokenCheckDelay = 0;

    private String thirdPartyLoginUrl;
    private String thirdPartyLoginApiKey;
    private String thirdPartyLoginScope;
    private String thirdPartyLoginReqParamDef;
    private String thirdPartyLoginClaim;
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

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();

        // Generate nonce
        byte[] secureBytes = new byte[64];
        new SecureRandom().nextBytes(secureBytes);
        String nonce = Base64.getUrlEncoder().encodeToString(secureBytes);
        HttpSession session = (HttpSession) ec.getSession(false);
        session.setAttribute("openIDNonce", nonce);

        // Populate parameters via OpenID Discovery, if not yet set
        doDiscovery();

        if (url == null) {
            throw new AuthenticationProviderException("endpoint not configured");
        }

        try {
            oAuthState =
                    new StringBuilder(UUID.randomUUID().toString()).append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).toString();
            DataManager.getInstance().getAuthResponseListener().register(this);

            URIBuilder builder = new URIBuilder(url);
            builder.addParameter("client_id", clientId);
            builder.addParameter("response_type", responseType);
            builder.addParameter("scope", scope);
            builder.addParameter("state", oAuthState);
            builder.addParameter("nonce", nonce);
            if (redirectionEndpoint != null) {
                builder.addParameter("redirect_uri", redirectionEndpoint);
            }
            if (responseMode != null) {
                builder.addParameter("response_mode", responseMode);
            }

            String uri = builder.build().toString();
            // logger.trace("uri: {}", uri);

            ec.redirect(uri);

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

        } catch (IOException | URISyntaxException e) {
            throw new AuthenticationProviderException(e);
        }
    }

    /**
     * Tries to find or create a valid {@link io.goobi.viewer.model.security.user.User} based on the given json object. Generates a
     * {@link io.goobi.viewer.model.security.authentication.LoginResult} containing the given request and response and either an optional containing
     * the user or nothing if no user was found, or a {@link io.goobi.viewer.model.security.authentication.AuthenticationProviderException} if an
     * internal error occured during login If this method is not called within {@link #getTimeoutMillis()} ms after calling
     * {@link #login(String, String)}, a loginResponse is created containing an appropriate exception. In any case, the future returned by
     * {@link #login(String, String)} is resolved.
     *
     * @param jwt {@link DecodedJWT}
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a {@link java.util.concurrent.Future} object.
     */
    public Future<Boolean> completeLogin(DecodedJWT jwt, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (jwt == null) {
                throw new AuthenticationProviderException("received no jwt object");
            }

            String email = null;
            String sub = null;
            if (jwt.getClaim("email") != null) {
                email = jwt.getClaim("email").asString();
            }
            if (jwt.getClaim("sub") != null) {
                sub = jwt.getClaim("sub").asString();
            }

            // Third party fallback
            if (email == null && thirdPartyLoginScope != null && jwt.getClaim(thirdPartyLoginScope) != null && thirdPartyLoginApiKey != null
                    && thirdPartyLoginReqParamDef != null && thirdPartyLoginUrl != null) {
                String data = jwt.getClaim(thirdPartyLoginScope).asString();
                JSONArray array = new JSONArray();
                JSONObject json = new JSONObject();
                array.put(data);
                json.put(thirdPartyLoginReqParamDef, array);
                final StringEntity entity = new StringEntity(json.toString());

                HttpPost externalRequest = new HttpPost(thirdPartyLoginUrl);
                String[] thirdPartyLoginApiKeyParams = thirdPartyLoginApiKey.split(" ");
                externalRequest.addHeader(thirdPartyLoginApiKeyParams[0], thirdPartyLoginApiKeyParams[1]);
                externalRequest.addHeader("content-type", "application/json");
                externalRequest.setEntity(entity);

                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                HttpResponse externalResponse = httpClient.execute(externalRequest);

                JSONObject externalResponseObj = new JSONObject(EntityUtils.toString(externalResponse.getEntity()));
                email = JsonTools.getNestedValue(externalResponseObj, thirdPartyLoginClaim);
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
                        logger.debug("Updated user {} - added OAuth sub '{}'.", user.getId(), comboSub);
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
        } catch (DAOException | IOException e) {
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

    void doDiscovery() {
        if (this.discoveryUri == null) {
            return;
        }

        logger.trace("OpenID discovery URI: {}", this.discoveryUri);
        try {
            String responseBody = NetTools.getWebContentGET(this.discoveryUri);
            JSONObject discoveryObj = new JSONObject(responseBody);
            for (String field : discoveryObj.keySet()) {
                // logger.trace("{}:{}", field, discoveryObj.get(field));
                switch (field) {
                    case "authorization_endpoint":
                        if (this.url == null) {
                            this.url = discoveryObj.getString(field);
                            logger.trace("Using {} from discovery: {}", field, url);
                        }
                        break;
                    case "issuer":
                        if (this.issuer == null) {
                            this.issuer = discoveryObj.getString(field);
                            logger.trace("Using {} from discovery: {}", field, issuer);
                        }
                        break;
                    case "jwks_uri":
                        if (this.jwksUri == null) {
                            this.jwksUri = discoveryObj.getString(field);
                            logger.trace("Using {} from discovery: {}", field, jwksUri);
                        }
                        break;
                    case "token_endpoint":
                        if (this.tokenEndpoint == null) {
                            this.tokenEndpoint = discoveryObj.getString(field);
                            logger.trace("Using {} from discovery: {}", field, tokenEndpoint);
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | HTTPException e) {
            logger.error(e.getMessage());
        }

    }

    /** {@inheritDoc} */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowsNicknameChange() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowsEmailChange() {
        return false;
    }

    /**
     * @return the discoveryUri
     */
    public String getDiscoveryUri() {
        return discoveryUri;
    }

    /**
     * @param discoveryUri the discoveryUri to set
     * @return this
     */
    public OpenIdProvider setDiscoveryUri(String discoveryUri) {
        this.discoveryUri = discoveryUri;
        return this;
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
     * @return the jwksUri
     */
    public String getJwksUri() {
        return jwksUri;
    }

    /**
     * @param jwksUri the jwksUri to set
     * @return this
     */
    public OpenIdProvider setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
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

    /**
     * @return the responseType
     */
    public String getResponseType() {
        return responseType;
    }

    /**
     * @param responseType the responseType to set
     * @return this
     */
    public OpenIdProvider setResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    /**
     * @return the responseMode
     */
    public String getResponseMode() {
        return responseMode;
    }

    /**
     * @param responseMode the responseMode to set
     * @return this
     */
    public OpenIdProvider setResponseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    /**
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * @param issuer the issuer to set
     * @return this
     */
    public OpenIdProvider setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    /**
     * @return the tokenCheckDelay
     */
    public long getTokenCheckDelay() {
        return tokenCheckDelay;
    }

    /**
     * @param tokenCheckDelay the tokenCheckDelay to set
     * @return this
     */
    public OpenIdProvider setTokenCheckDelay(long tokenCheckDelay) {
        this.tokenCheckDelay = tokenCheckDelay;
        return this;
    }

    public String getThirdPartyLoginUrl() {
        return thirdPartyLoginUrl;
    }

    public String getThirdPartyLoginApiKey() {
        return thirdPartyLoginApiKey;
    }

    public String getThirdPartyLoginScope() {
        return thirdPartyLoginScope;
    }

    public String getThirdPartyLoginReqParamDef() {
        return thirdPartyLoginReqParamDef;
    }

    public String getThirdPartyLoginClaim() {
        return thirdPartyLoginClaim;
    }

    public IAuthenticationProvider setThirdPartyVariables(String thirdPartyLoginUrl, String thirdPartyLoginApiKey,
            String thirdPartyLoginScope, String thirdPartyLoginReqParamDef, String thirdPartyLoginClaim) {
        if ((thirdPartyLoginUrl != null) && (thirdPartyLoginApiKey != null) && (thirdPartyLoginScope != null)) {
            this.thirdPartyLoginUrl = thirdPartyLoginUrl;
            this.thirdPartyLoginApiKey = thirdPartyLoginApiKey;
            this.thirdPartyLoginScope = thirdPartyLoginScope;
            this.thirdPartyLoginReqParamDef = thirdPartyLoginReqParamDef;
            this.thirdPartyLoginClaim = thirdPartyLoginClaim;
        }

        return this;
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
}
