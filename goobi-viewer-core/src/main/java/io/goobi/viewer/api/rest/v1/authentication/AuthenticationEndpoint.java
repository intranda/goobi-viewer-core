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
package io.goobi.viewer.api.rest.v1.authentication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.AuthenticationException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.authentication.AuthResponseListener;
import io.goobi.viewer.model.security.authentication.HttpAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.HttpHeaderProvider;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.security.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * AuthenticationEndpoint class.
 * </p>
 */
@Path(ApiUrls.AUTH)
public class AuthenticationEndpoint {

    private static final Logger logger = LogManager.getLogger(AuthenticationEndpoint.class);

    static final String REASON_PHRASE_ILLEGAL_REDIRECT_URL = "Illegal redirect URL or URL cannot be checked.";
    static final String REASON_PHRASE_NO_PROVIDERS_CONFIGURED = "No authentication providers of type 'httpHeader' configured.";
    static final String REASON_PHRASE_NO_PROVIDER_FOUND = "No matching provider found.";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * This future gets fulfilled once the {@link UserBean} has finished setting up the session and redirecting the request. Completing the request
     * should wait after the redirect, otherwise it will not have any effect
     */
    private static Future<Boolean> redirected = null;

    /**
     * <p>
     * authenticateUser.
     * </p>
     *
     * @param email a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @return a {@link jakarta.ws.rs.core.Response} object.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response authenticateUser(@FormParam("email") String email, @FormParam("password") String password) {
        try {

            // Authenticate the user using the credentials provided
            authenticate(email, password);

            // Issue a token for the user
            String token = issueToken(email);

            // Return the token on the response
            return Response.ok(token).build();

        } catch (AuthenticationException e) {
            logger.debug(e.getMessage());
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (DAOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     *
     * @param email
     * @param password
     * @throws AuthenticationException
     * @throws DAOException
     */
    private static void authenticate(String email, String password) throws AuthenticationException, DAOException {
        User user = new User().auth(email, password);
        if (!user.isSuperuser()) {
            throw new AuthenticationException("Superuser access required");
        }
    }

    /**
     *
     * @param email
     * @return email
     */
    private static String issueToken(String email) {
        // Issue a token (can be a random String persisted to a database or a JWT token)
        // The issued token must be associated to a user
        // Return the issued token

        return email;
    }

    /**
     * 
     * @param redirectUrl
     * @return {@link Response}
     * @should return status 403 if redirectUrl external
     * @should return status 403 if no httpHeader type provider configured
     * @should return status 403 if no matching provider found
     */
    @GET
    @Path(ApiUrls.AUTH_HEADER)
    @Operation(summary = "Header login", description = "Checks a configurable header for a username and logs in the user if it is found in the DB")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "500", description = "Internal error")
    public Response headerParameterLogin(@QueryParam("redirectUrl") String redirectUrl) {
        logger.debug("headerParameterLogin");
        Optional<NavigationHelper> nh = BeanUtils.getBeanFromSession(servletRequest.getSession(), "navigationHelper", NavigationHelper.class);
        if (redirectUrl != null && (!nh.isPresent() || !redirectUrl.startsWith(nh.get().getApplicationUrl()))) {
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), REASON_PHRASE_ILLEGAL_REDIRECT_URL)
                    .build();
        }

        HttpHeaderProvider useProvider = null;

        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getAuthenticationProvider() instanceof HttpHeaderProvider httpHeaderProvider) {
            useProvider = httpHeaderProvider;
        }

        String ssoId = null;

        // If the login wasn't triggered by the user, find an appropriate provider
        if (useProvider == null) {
            List<HttpHeaderProvider> providers = new ArrayList<>();
            for (HttpAuthenticationProvider provider : DataManager.getInstance().getAuthResponseListener().getProviders()) {
                if (provider instanceof HttpHeaderProvider httpHeaderProvider) {
                    providers.add(httpHeaderProvider);
                    break;
                }
            }
            if (providers.isEmpty()) {
                logger.warn("No providers configured.");
                return Response.status(Response.Status.FORBIDDEN.getStatusCode(), REASON_PHRASE_NO_PROVIDERS_CONFIGURED)
                        .build();
            }

            for (HttpHeaderProvider provider : providers) {
                logger.trace(provider.getName());
                boolean headerType = HttpHeaderProvider.PARAMETER_TYPE_HEADER.equalsIgnoreCase(provider.getParameterType());
                String value = headerType ? servletRequest.getHeader(provider.getParameterName())
                        : (String) servletRequest.getAttribute(provider.getParameterName());
                if (StringUtils.isNotEmpty(value)) {
                    logger.debug("Found request {}: {}:{}", (headerType ? "HEADER" : "ATTRIBUTE"), provider.getParameterName(), value);
                    useProvider = provider;
                    if (userBean != null) {
                        userBean.setAuthenticationProvider(provider);
                    }
                    break;
                }
            }
        }

        if (useProvider == null) {
            logger.warn("No matching authentication provider found.");
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), REASON_PHRASE_NO_PROVIDER_FOUND).build();
        }
        logger.debug("Provider selected: {}", useProvider.getName());

        // Set ssoId
        boolean headerType = HttpHeaderProvider.PARAMETER_TYPE_HEADER.equalsIgnoreCase(useProvider.getParameterType());
        ssoId = headerType ? servletRequest.getHeader(useProvider.getParameterName())
                : (String) servletRequest.getAttribute(useProvider.getParameterName());

        Future<Boolean> loginSuccess = useProvider.completeLogin(ssoId, servletRequest, servletResponse);
        DataManager.getInstance().getAuthResponseListener().unregister(useProvider);
        try {
            // Before sending response, wait until UserBean.completeLogin() has finished and released the result
            if (Boolean.FALSE.equals(loginSuccess.get())) {
                if (StringUtils.isNotEmpty(redirectUrl)) {
                    logger.debug("Redirecting to redirectUrl");
                    servletResponse.sendRedirect(redirectUrl); //NOSONAR redirectUrl is verified at this point
                } else if (BeanUtils.getNavigationHelper() != null) {
                    logger.debug("No redirect URL found, redirecting to home");
                    servletResponse.sendRedirect(BeanUtils.getNavigationHelper().getApplicationUrl());
                }
            }
        } catch (ExecutionException | IOException e) {
            logger.error(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.trace("endpoint end");
        return Response.ok("").build();
    }

    /**
     * 
     * @param error
     * @param authCode
     * @param accessToken
     * @param state
     * @return {@link Response}
     * @throws IOException
     */
    @GET
    @Path(ApiUrls.AUTH_OAUTH)
    @Operation(summary = "OpenID Connect callback (GET method)", description = "Verifies an openID claim and starts a session for the user")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "500", description = "Internal error")
    //    @Tag(name = "login")
    public Response openIdLoginGET(@QueryParam("error") String error, @QueryParam("code") String authCode,
            @QueryParam("id_token") String accessToken, @QueryParam("state") String state) throws IOException {
        logger.trace("openIdLoginGET");
        //        for (String key : servletRequest.getParameterMap().keySet()) {
        //            logger.trace("{}:{}", key, servletRequest.getParameterMap().get(key));
        //        }
        return openIdLogin(state, error, authCode, accessToken);
    }

    /**
     * 
     * @param error
     * @param authCode
     * @param state
     * @return {@link Response}
     * @throws IOException
     */
    @POST
    @Path(ApiUrls.AUTH_OAUTH)
    @Operation(summary = "OpenID Connect callback (POST method)", description = "Verifies an openID claim and starts a session for the user")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "500", description = "Internal error")
    @Tag(name = "login")
    public Response openIdLoginPOST(@FormParam("error") String error, @FormParam("code") String authCode, @FormParam("state") String state)
            throws IOException {
        logger.trace("openIdLoginPOST");
        return openIdLogin(state, error, authCode, null);
    }

    /**
     * 
     * @param state
     * @param error
     * @param authCode
     * @param accessToken
     * @return {@link Response}
     * @throws IOException
     */
    private Response openIdLogin(String state, String error, String authCode, String accessToken) throws IOException {
        if (error != null) {
            //( logger.trace("Error: {}", error); //NOSONAR User-controlled data, only comment in for debugging
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), error).build();
        }

        if (authCode == null && accessToken == null) {
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "No auth code or token received.").build();
        }

        AuthResponseListener<HttpAuthenticationProvider> listener = DataManager.getInstance().getAuthResponseListener();
        OpenIdProvider provider = null;
        try {
            for (HttpAuthenticationProvider p : DataManager.getInstance().getAuthResponseListener().getProviders()) {
                if (p instanceof OpenIdProvider openIdProvider && state != null && state.equals(openIdProvider.getoAuthState())) {
                    provider = openIdProvider;
                    listener.unregister(openIdProvider);
                    break;
                }
            }
            if (provider == null) {
                logger.trace("No provider found for given state.");
                return Response.status(Response.Status.FORBIDDEN.getStatusCode(), REASON_PHRASE_NO_PROVIDER_FOUND).build();
            }

            String idTokenEncoded = accessToken;
            if (idTokenEncoded == null) {
                // Fetch token from token endpoint
                Map<String, String> headers = HashMap.newHashMap(2);
                headers.put("Accept-Charset", "utf-8");
                headers.put("Content-Type", "application/x-www-form-urlencoded");

                Map<String, String> params = HashMap.newHashMap(5);
                params.put("grant_type", "authorization_code");
                params.put("code", authCode);
                params.put("client_id", provider.getClientId());
                params.put("client_secret", provider.getClientSecret());
                params.put("redirect_uri", provider.getRedirectionEndpoint());

                String responseBody = NetTools.getWebContentPOST(provider.getTokenEndpoint(), headers, params, null, null, null, null);
                // logger.trace(responseBody);

                JSONObject responseObj = new JSONObject(responseBody);
                idTokenEncoded = responseObj.getString("id_token");
            }

            // Artificial delay because sometimes token validity starts after the current time
            if (provider.getTokenCheckDelay() > 0) {
                logger.debug("Applying token check delay of {} ms", provider.getTokenCheckDelay());
                try {
                    Thread.sleep(provider.getTokenCheckDelay());
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            DecodedJWT jwt = verifyOpenIdToken(idTokenEncoded, provider.getJwksUri(), provider.getIssuer());
            if (jwt == null) {
                return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "Could not verify authentication token.").build();
            }

            // now check if the nonce is the same as in the old session
            String nonce = (String) servletRequest.getSession().getAttribute("openIDNonce");
            if (!nonce.equals(jwt.getClaim("nonce").asString())) {
                logger.error("nonce does not match. Not logging user in");
                return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "Nonce mismatch.").build();
            }
            if (!provider.getClientId().equals(jwt.getClaim("aud").asString())) {
                logger.error("clientId does not match aud. Not logging user in");
                return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "cliendId mismatch.").build();
            }

            redirected = provider.completeLogin(jwt, servletRequest, servletResponse);
            if (redirected != null) {
                try {
                    // redirected has an internal timeout, so this get() should never run into a timeout, but you never know
                    redirected.get(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Waiting for redirect after login interrupted unexpectedly");
                } catch (TimeoutException e) {
                    logger.error("Waiting for redirect after login took longer than a minute. This should not happen. Redirect will not take place");
                } catch (ExecutionException e) {
                    logger.error("Unexpected error while waiting for redirect", e);
                }
            }

            // Redirect to original URL, if not yet happened
            if (!servletResponse.isCommitted()) {
                logger.debug("Provider did not commit redirect; performing fallback redirect.");
                return Response.seeOther(URI.create(provider.getRedirectUrl())).build();
            }

            return Response.ok("").build();
        } finally {
            if (provider != null) {
                listener.unregister(provider);
            }
        }
    }

    /**
     * 
     * @param token
     * @param jwksUri
     * @param issuer
     * @return {@link DecodedJWT}
     */
    static DecodedJWT verifyOpenIdToken(String token, String jwksUri, String issuer) {
        // logger.trace(token);
        RSAKeyProvider keyProvider = null;
        try {
            final JwkProvider provider = new UrlJwkProvider(new URI(jwksUri).toURL());

            keyProvider = new RSAKeyProvider() {
                @Override
                public RSAPublicKey getPublicKeyById(String kid) {
                    //Received 'kid' value might be null if it wasn't defined in the Token's header
                    PublicKey publicKey;
                    try {
                        publicKey = provider.get(kid).getPublicKey();
                        return (RSAPublicKey) publicKey;
                    } catch (JwkException e) {
                        logger.error(e.getMessage(), e);
                    }
                    return null;
                }

                @Override
                public RSAPrivateKey getPrivateKey() {
                    return null;
                }

                @Override
                public String getPrivateKeyId() {
                    return null;
                }
            };
        } catch (MalformedURLException | URISyntaxException e) {
            logger.error(e.getMessage(), e);
        }

        DecodedJWT decodedJwt = JWT.decode(token);
        String strAlgorithm = decodedJwt.getAlgorithm();

        Algorithm algorithm = null;
        if ("RS256".equals(strAlgorithm)) {
            algorithm = Algorithm.RSA256(keyProvider);
        } else {
            logger.error("JWT algorithm not supported: {}", strAlgorithm);
            return null;
        }

        try {
            JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();
            return verifier.verify(decodedJwt);
        } catch (JWTVerificationException exception) {
            logger.error(exception);
            return null;
        }
    }
}
