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
package io.goobi.viewer.api.rest.v2.auth;

import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_ACCESS_TOKEN;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_LOGIN;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_LOGOUT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_PROBE_REQUEST;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.intranda.api.iiif.auth.v2.AuthAccessToken2;
import de.intranda.api.iiif.auth.v2.AuthAccessTokenError2;
import de.intranda.api.iiif.auth.v2.AuthAccessTokenError2.Profile;
import de.intranda.api.iiif.auth.v2.AuthProbeResult2;
import de.intranda.api.iiif.auth.v2.AuthProbeService2;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.authentication.AuthenticationProviderException;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;

@jakarta.ws.rs.Path(AUTH)
@ViewerRestServiceBinding
@CORSBinding
public class AuthorizationFlowResource {

    private static final Logger logger = LogManager.getLogger(AuthorizationFlowResource.class);

    private static final String KEY_ORIGIN = "IIIF_origin";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public AuthorizationFlowResource(@Context HttpServletRequest request) {
    }

    /**
     * For testing purposes.
     * 
     * @return {@link AuthProbeService2}
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public AuthProbeService2 getServiceDescription() {
        logger.debug("session id from request: {}", servletRequest.getSession().getId());
        return AuthorizationFlowTools.getAuthServicesEmbedded("PPN123", "00000001.xml");
    }

    /**
     * Access (login) service.
     * 
     * @param origin Mandatory origin parameter from the client
     * @return {@link Response}
     */
    @GET
    @jakarta.ws.rs.Path(AUTH_LOGIN)
    @Produces({ MediaType.TEXT_HTML })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public Response loginService(@QueryParam("origin") String origin) {
        logger.debug("loginService");
        servletRequest.getSession(true); // Force session creation
        debugRequest();
        if (StringUtils.isEmpty(origin)) {
            logger.debug("origin missing");
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "origin missing").build();
        }
        logger.debug("origin: {}", origin);
        if (!addOriginToSession(origin)) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "Could not add origin to session").build();
        }

        URI loginRedirectUri = URI.create(DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "login/?origin=" + origin);

        return Response.temporaryRedirect(loginRedirectUri)
                .header("Set-Cookie", generateCookie())
                .header("Content-Security-Policy", "frame-ancestors 'self' " + origin)
                .build();
    }

    /**
     * Token service. Issues a bearer token, if conditions are met.
     * 
     * @param messageId Client-generated message ID; Must be included in the response
     * @param origin Mandatory origin parameter from the client
     * @return {@link Response}
     * @throws JsonProcessingException
     */
    @GET
    @jakarta.ws.rs.Path(AUTH_ACCESS_TOKEN)
    @Produces({ MediaType.TEXT_HTML })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public Response accessTokenService(@QueryParam("messageId") String messageId, @QueryParam("origin") String origin)
            throws JsonProcessingException {
        logger.debug("accessTokenService");
        debugRequest();
        logger.debug("messageId: {}", messageId);
        logger.debug("origin: {}", origin);

        if (StringUtils.isNotEmpty(messageId) && StringUtils.isNotEmpty(origin)) {
            // Validate origin
            if (!origin.equals(getOriginFromSession())) {
                logger.debug("Invalid origin, expected: {}", getOriginFromSession());
                return Response
                        .ok(getTokenServiceResponseBody(JsonTools.getAsJson(new AuthAccessTokenError2(messageId, Profile.INVALID_ORIGIN)), origin),
                                MediaType.TEXT_HTML)
                        .build();
            }

            // Check whether someone actually logged in
            if (BeanUtils.getUserFromSession(servletRequest.getSession()) == null) {
                logger.debug("Not logged in");
                AuthAccessTokenError2 error = new AuthAccessTokenError2(messageId, Profile.MISSING_ASPECT);
                for (Locale locale : ViewerResourceBundle.getAllLocales()) {
                    error.getHeading().put(locale.getLanguage(), ViewerResourceBundle.getTranslation("notLoggedIn", locale));
                }
                return Response.ok(getTokenServiceResponseBody(JsonTools.getAsJson(error), origin), MediaType.TEXT_HTML).build();
            }

            //            if (sessionId.equals(servletRequest.getSession().getId())) {
            AuthAccessToken2 token = new AuthAccessToken2(messageId, 300);
            DataManager.getInstance().getBearerTokenManager().addToken(token, servletRequest.getSession());
            return generateOkResponse(getTokenServiceResponseBody(JsonTools.getAsJson(token), origin), MediaType.TEXT_HTML, origin);
            //            }
        }

        return generateOkResponse(
                getTokenServiceResponseBody(JsonTools.getAsJson(new AuthAccessTokenError2(messageId, Profile.INVALID_REQUEST)), origin),
                MediaType.TEXT_HTML, origin);
    }

    /**
     * Builds a postMessage response body.
     * 
     * @param jsonMsg JSON content
     * @param origin Client origin
     * @return {@link String}
     */
    private static String getTokenServiceResponseBody(String jsonMsg, String origin) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><script>window.parent.postMessage(")
                .append(jsonMsg)
                .append(",'")
                .append(origin)
                .append("'")
                .append(");</script></body></html>");

        logger.debug("Token service response body:\n{}", sb);
        return sb.toString();
    }

    /**
     * 
     * @param pi Record identifier
     * @param filename Content file name
     * @param origin Client origin
     * @return {@link Response}
     */
    @OPTIONS
    @jakarta.ws.rs.Path(AUTH_PROBE_REQUEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public Response handleProbePreflight(@Parameter(description = "Record identifier") @PathParam("pi") String pi,
            @Parameter(description = "Content file name") @PathParam("filename") String filename, @HeaderParam("Origin") String origin) {
        logger.debug("handleProbePreflight: {}/{}", pi, filename);
        debugRequest();
        if (StringUtils.isEmpty(origin)) {
            logger.warn("No Origin header found.");
        }
        if (origin != null) {
            return Response.ok()
                    .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
                    .header("Access-Control-Max-Age", "3600")
                    .build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    /**
     * 
     * @param pi Record identifier
     * @param filename Content file name
     * @param origin Client origin
     * @return {@link Response}
     * @throws JsonProcessingException
     */
    @GET
    @jakarta.ws.rs.Path(AUTH_PROBE_REQUEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public Response probeResource(@Parameter(description = "Record identifier") @PathParam("pi") String pi,
            @Parameter(description = "Content file name") @PathParam("filename") String filename, @HeaderParam("Origin") String origin)
            throws JsonProcessingException {
        logger.debug("probeResource: {}/{}", pi, filename);
        debugRequest();
        if (StringUtils.isEmpty(origin)) {
            logger.warn("No Origin header found.");
        }
        String authHeader = servletRequest.getHeader("Authorization");
        if (authHeader == null) {
            // No token? No service!
            return generateOkResponse(JsonTools.getAsJson(new AuthProbeResult2().setStatus(Response.Status.UNAUTHORIZED.getStatusCode())),
                    MediaType.APPLICATION_JSON, origin);
        }

        logger.debug("Authorization: {}", authHeader);
        if (!authHeader.startsWith("Bearer ")) {
            // Invalid token header value
            AuthProbeService2 service = AuthorizationFlowTools.getAuthServicesEmbedded(pi, filename);
            service.getErrorHeading().put("en", "Authorization: bad format");
            service.getErrorNote().put("en", "Authorization: bad format");
            return generateOkResponse(JsonTools.getAsJson(service), MediaType.APPLICATION_JSON, origin);
        }

        String tokenValue = authHeader.substring(7);
        logger.debug("Token: {}", tokenValue);
        AuthAccessToken2 token = DataManager.getInstance().getBearerTokenManager().getTokenMap().get(tokenValue);
        if (token == null) {
            logger.debug("Token not found.");
            AuthProbeService2 service = AuthorizationFlowTools.getAuthServicesEmbedded(pi, filename);
            service.getErrorHeading().put("en", "Token not found");
            service.getErrorNote().put("en", "Token not found");
            return generateOkResponse(JsonTools.getAsJson(service), MediaType.APPLICATION_JSON, origin);
        }
        
        if (token.isExpired()) {
            logger.debug("Token expired.");
            DataManager.getInstance().getBearerTokenManager().purgeExpiredTokens();
            return generateOkResponse(JsonTools.getAsJson(new AuthProbeResult2().setStatus(Response.Status.UNAUTHORIZED.getStatusCode())),
                    MediaType.APPLICATION_JSON, origin);
        }

        String key = pi + "_" + filename;
        Boolean access = token.hasPermission(key);
        if (access == null) {
            try {
                BaseMimeType baseMimeType = FileTools.getBaseMimeType(FileTools.getMimeTypeFromFile(Paths.get(filename)));
                logger.trace("Base mime type: {}", baseMimeType);
                if (BaseMimeType.APPLICATION.equals(baseMimeType) && "pdf".equalsIgnoreCase(FilenameUtils.getExtension(filename))) {
                    // TODO Page PDF access check
                    access = false;
                } else {
                    // Image/text access check
                    access = AccessConditionUtils
                            .checkAccess(DataManager.getInstance().getBearerTokenManager().getTokenSessionMap().get(tokenValue),
                                    baseMimeType.getName(), pi, filename, NetTools.getIpAddress(servletRequest), false)
                            .isGranted();
                }
                token.addPermission(key, access);

            } catch (IndexUnreachableException | DAOException | IOException e) {
                logger.error(e.getMessage());
                AuthProbeService2 service = AuthorizationFlowTools.getAuthServicesEmbedded(pi, filename);
                service.getErrorHeading().put("en", "Error");
                service.getErrorNote().put("en", e.getMessage());
                return generateOkResponse(JsonTools.getAsJson(service), MediaType.APPLICATION_JSON, origin);
            }
        }

        AuthProbeResult2 result = new AuthProbeResult2();
        if (access) {
            result.setStatus(Response.Status.OK.getStatusCode());
            logger.debug("access granted");
        } else {
            result.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
            logger.debug("access denied");
        }

        return generateOkResponse(JsonTools.getAsJson(result), MediaType.APPLICATION_JSON, origin);
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_LOGOUT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public Response logout() {
        logger.debug("logout");
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null) {
            try {
                userBean.logout();
                // Tokens and origin should be purged from SessionBean by logout process
                return Response.ok("").build();
            } catch (AuthenticationProviderException e) {
                logger.error(e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
            }
        }

        logger.debug("UserBean not found");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 
     * @return origin values from the session; null if none found
     */
    private String getOriginFromSession() {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            logger.debug("session id: {}", session.getId());
            return (String) session.getAttribute(KEY_ORIGIN);
        }

        return null;
    }

    /**
     * 
     * @param origin Client origin
     * @return true if origin has been successfully added to the session; false otherwise
     */
    private boolean addOriginToSession(String origin) {
        if (origin == null) {
            throw new IllegalArgumentException("origin may not be null");
        }
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            logger.debug("session id: {}", session.getId());
            session.setAttribute(KEY_ORIGIN, origin);
            logger.debug("origin added to session: {}", origin);
            return true;
        }

        return false;
    }

    /**
     * Generates Response object. Most of the time, the client will expect a HTTP 200 status, with actual error details packaged in the response body.
     * 
     * @param entity JSON entity
     * @param mediaType Response mime type
     * @param origin Client origin
     * @return {@link Response}
     */
    private Response generateOkResponse(Object entity, String mediaType, String origin) {
        return Response.ok(entity, mediaType)
                .header("Set-Cookie", generateCookie())
                .header("Content-Security-Policy", "frame-ancestors 'self' " + origin)
                .build();
    }

    public String generateCookie() {
        NewCookie sessionCookie = new NewCookie.Builder("JSESSIONID")
                .value(servletRequest.getSession().getId())
                .path("/") // Cookie valid for all paths
                .secure(true) // Only sent over HTTPS
                .httpOnly(true) // Not accessible via JavaScript
                .build();

        return RuntimeDelegate.getInstance().createHeaderDelegate(NewCookie.class).toString(sessionCookie) + "; SameSite=None";
    }

    private void debugRequest() {
        if (servletRequest != null) {
            if (servletRequest.getSession() != null) {
                logger.debug("session id from request: {}", servletRequest.getSession().getId());
            }
            Cookie[] cookies = servletRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : servletRequest.getCookies()) {
                    logger.debug("Cookie received: {}={}", cookie.getName(), cookie.getValue());
                }
            } else {
                logger.debug("No cookies received");
            }
        }
    }
}
