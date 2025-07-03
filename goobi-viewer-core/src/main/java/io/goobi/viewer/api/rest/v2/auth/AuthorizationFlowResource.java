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
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_ACCESS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_ACCESS_TOKEN;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_LOGOUT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_PROBE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_PROBE_REQUEST;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.intranda.api.iiif.auth.v2.AuthAccessService2;
import de.intranda.api.iiif.auth.v2.AuthAccessToken2;
import de.intranda.api.iiif.auth.v2.AuthAccessTokenError2;
import de.intranda.api.iiif.auth.v2.AuthAccessTokenError2.Profile;
import de.intranda.api.iiif.auth.v2.AuthAccessTokenService2;
import de.intranda.api.iiif.auth.v2.AuthLogoutService2;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private static final String BASE_URL = DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "api/v2" + AUTH;

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public AuthorizationFlowResource(@Context HttpServletRequest request) {
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_ACCESS_TOKEN)
    @Produces({ MediaType.TEXT_HTML })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public String accessTokenService(@QueryParam("messageId") String messageId, @QueryParam("origin") String origin,
            @CookieParam("SESSION_ID") String sessionId) throws JsonProcessingException {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(messageId) && StringUtils.isNotEmpty(origin) && StringUtils.isNotEmpty(sessionId)) {
            logger.trace("messageId: {}", messageId);
            logger.trace("origin: {}", origin);
            logger.trace("sessionId: {}", sessionId);
            // TODO Validate origin
            // TODO Check auth status via cookie here
            String token = "";
            sb.append("<html><body><script>window.parent.postMessage(")
                    .append(JsonTools.getAsJson(new AuthAccessToken2(messageId, token).setExpiresIn(300)))
                    .append(");</script></body></html>");
        } else {
            return JsonTools.getAsJson(new AuthAccessTokenError2(messageId, Profile.INVALID_REQUEST));
        }

        return sb.toString();
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_PROBE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public AuthProbeService2 getProbeServiceDescription() {
        return new AuthProbeService2(URI.create(BASE_URL + AUTH_PROBE),
                Collections.singletonList(new AuthAccessService2(URI.create(BASE_URL + AUTH_ACCESS), AuthAccessService2.Profile.ACTIVE,
                        new AuthAccessTokenService2(URI.create(BASE_URL + AUTH_ACCESS_TOKEN)),
                        new AuthLogoutService2(URI.create(BASE_URL + AUTH_LOGOUT)))));
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_PROBE_REQUEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public AuthProbeResult2 probeResource(@Parameter(description = "Record identifier") @PathParam("pi") String pi,
            @Parameter(description = "Content file name") @PathParam("filename") String filename) {
        logger.trace("probeResource: {}/{}", pi, filename);
        AuthProbeResult2 ret = new AuthProbeResult2();

        String authHeader = servletRequest.getHeader("Authorization");
        if (StringUtils.isNotEmpty(authHeader)) {
            logger.trace("Authorization: {}", authHeader);
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                logger.trace("Token: {}", token);
                // TODO Check token, return 200 if access granted via token
            } else {
                ret.setStatus(Response.Status.BAD_REQUEST.getStatusCode()).setHeading(new HashMap<>()).setNote(new HashMap<>());
                ret.getHeading().put("en", "Authorization: bad format");
                ret.getNote().put("en", "Authorization: bad format");
            }

            return ret;
        }

        try {
            BaseMimeType baseMimeType = FileTools.getBaseMimeType(FileTools.getMimeTypeFromFile(Paths.get(filename)));
            logger.trace("Base mime type: {}", baseMimeType);
            boolean access = AccessConditionUtils.checkAccess(servletRequest, baseMimeType.getName(), pi, filename, false).isGranted();
            if (access) {
                ret.setStatus(Response.Status.OK.getStatusCode());
                logger.trace("access granted");
            } else {
                ret.setStatus(Response.Status.FORBIDDEN.getStatusCode());
                logger.trace("access denied");
            }
        } catch (IndexUnreachableException | DAOException | IOException e) {
            ret.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).setHeading(new HashMap<>()).setNote(new HashMap<>());
            ret.getHeading().put("en", "Error");
            ret.getNote().put("en", e.getMessage());
        }

        return ret;
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_LOGOUT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "")
    public Response logout() {
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null) {
            try {
                userBean.logout();
                // TODO delete tokens
                return Response.ok("").build();
            } catch (AuthenticationProviderException e) {
                logger.error(e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
            }
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
}
