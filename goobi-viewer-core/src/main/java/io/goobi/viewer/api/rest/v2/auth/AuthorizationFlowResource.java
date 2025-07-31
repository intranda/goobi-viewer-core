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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.authentication.AuthenticationProviderException;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path(AUTH)
@ViewerRestServiceBinding
@CORSBinding
public class AuthorizationFlowResource {

    private static final Logger logger = LogManager.getLogger(AuthorizationFlowResource.class);

    private static final String KEY_ORIGIN = "IIIF_origin";
    private static final String KEY_TOKENS = "IIIF_AuthAccessTokens";

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
        logger.debug("local session id: {}", servletRequest.getSession().getId());
        return AuthorizationFlowTools.getAuthServicesEmbedded("PPN123", "00000001.xml");
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_LOGIN)
    @Produces({ MediaType.TEXT_HTML })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public Response loginService(@QueryParam("origin") String origin) throws ServletException, IOException {
        logger.debug("accessService");
        servletRequest.getSession(true);
        logger.debug("local session id: {}", servletRequest.getSession().getId());
        if (StringUtils.isEmpty(origin)) {
            logger.debug("origin missing");
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "origin missing").build();
        }
        logger.debug("origin: {}", origin);
        if (!addOriginToSession(origin)) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "Could not add origin to session").build();
        }

        // servletRequest.getRequestDispatcher("/user/").forward(servletRequest, servletResponse);
        servletResponse.sendRedirect(DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "user/");
        // TODO Make sure new tab is closed after logging in

        return Response.ok("").build();
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_ACCESS_TOKEN)
    @Produces({ MediaType.TEXT_HTML })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public String accessTokenService(@QueryParam("messageId") String messageId, @QueryParam("origin") String origin,
            @CookieParam("SESSION_ID") String sessionId) throws JsonProcessingException {
        logger.debug("accessTokenService");
        logger.debug("messageId: {}", messageId);
        logger.debug("origin: {}", origin);
        logger.debug("sessionId: {}", sessionId);
        logger.debug("local session id: {}", servletRequest.getSession().getId());
        if (StringUtils.isNotEmpty(messageId) && StringUtils.isNotEmpty(origin)) {

            // Validate origin
            if (!origin.equals(getOriginFromSession())) {
                logger.debug("Invalid origin, expected: {}", getOriginFromSession());
                // return JsonTools.getAsJson(new AuthAccessTokenError2(messageId, Profile.INVALID_ORIGIN)); // TODO re-enabled
            }

            //            if (sessionId.equals(servletRequest.getSession().getId())) {
            AuthAccessToken2 token = new AuthAccessToken2(messageId, 300);
            addTokenToSession(token);
            StringBuilder sb = new StringBuilder();
            sb.append("<html><body><script>window.parent.postMessage(")
                    .append(JsonTools.getAsJson(token))
                    .append(");</script></body></html>");
            return sb.toString();
            //            }
        }

        return JsonTools.getAsJson(new AuthAccessTokenError2(messageId, Profile.INVALID_REQUEST));
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_PROBE_REQUEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public AuthProbeResult2 probeResource(@Parameter(description = "Record identifier") @PathParam("pi") String pi,
            @Parameter(description = "Content file name") @PathParam("filename") String filename) {
        logger.debug("probeResource: {}/{}", pi, filename);
        logger.debug("local session id: {}", servletRequest.getSession().getId());
        AuthProbeResult2 ret = new AuthProbeResult2();

        String authHeader = servletRequest.getHeader("Authorization");
        //        if (StringUtils.isEmpty(authHeader)) {
        //            ret.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
        //            ret.getHeading().put("en", "Authorization: bad format");
        //            ret.getNote().put("en", "Authorization: bad format");
        //            return ret;
        //        }
        logger.trace("Authorization: {}", authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String tokenValue = authHeader.substring(7);
            logger.debug("Token: {}", tokenValue);
            AuthAccessToken2 token = getTokenFromSession(tokenValue);
            if (token != null) {
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
                            access = AccessConditionUtils.checkAccess(servletRequest, baseMimeType.getName(), pi, filename, false).isGranted();
                        }
                        token.addPermission(key, access);
                    } catch (IndexUnreachableException | DAOException | IOException e) {
                        logger.error(e.getMessage());
                        ret.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                        ret.getHeading().put("en", "Error");
                        ret.getNote().put("en", e.getMessage());
                        access = false;
                    }
                }
                if (access) {
                    ret.setStatus(Response.Status.OK.getStatusCode());
                    logger.debug("access granted");
                } else {
                    ret.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
                    logger.debug("access denied");
                }
            } else {
                logger.debug("Token not found in session.");
                ret.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
            }

            return ret;
        }

        return ret;
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_LOGOUT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public Response logout() {
        logger.trace("logout");
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
     * @param token
     * @return true if successful; false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean addTokenToSession(AuthAccessToken2 token) {
        if (token == null) {
            throw new IllegalArgumentException("token may not be null");
        }
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            //  Add token to bean or session
            Map<String, AuthAccessToken2> tokenMap = (Map<String, AuthAccessToken2>) session.getAttribute(KEY_TOKENS);
            if (tokenMap == null) {
                tokenMap = new HashMap<>();
                session.setAttribute(KEY_TOKENS, tokenMap);
            }
            tokenMap.put(token.getAccessToken(), token);
            return true;
        }

        return false;
    }

    /**
     * Retrieves {@link AuthAccessToken2} with the given token value from SessionBean.
     * 
     * @param token Token string
     * @return {@link AuthAccessToken2}; null if none found
     */
    @SuppressWarnings("unchecked")
    private AuthAccessToken2 getTokenFromSession(String token) {
        if (token == null) {
            throw new IllegalArgumentException("token may not be null");
        }
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            Map<String, AuthAccessToken2> tokenMap = (Map<String, AuthAccessToken2>) session.getAttribute(KEY_TOKENS);
            if (tokenMap == null) {
                tokenMap = new HashMap<>();
                session.setAttribute(KEY_TOKENS, tokenMap);
            }
            return tokenMap.get(token);
        }

        return null;
    }

    /**
     * 
     * @return
     */
    private String getOriginFromSession() {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            logger.debug("session: {}", session);
            return (String) session.getAttribute(KEY_ORIGIN);
        }

        return null;
    }

    /**
     * 
     * @param origin
     * @return
     */
    private boolean addOriginToSession(String origin) {
        if (origin == null) {
            throw new IllegalArgumentException("origin may not be null");
        }
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            logger.debug("session: {}", session);
            session.setAttribute(KEY_ORIGIN, origin);
            logger.debug("origin added to session: {}", origin);
            return true;
        }

        return false;
    }
}
