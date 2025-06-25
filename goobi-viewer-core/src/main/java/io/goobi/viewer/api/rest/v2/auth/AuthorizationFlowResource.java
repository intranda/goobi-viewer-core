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

import java.net.URI;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.auth.v2.AuthAccessService2;
import de.intranda.api.iiif.auth.v2.AuthAccessTokenService2;
import de.intranda.api.iiif.auth.v2.AuthLogoutService2;
import de.intranda.api.iiif.auth.v2.AuthProbeService2;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@jakarta.ws.rs.Path(AUTH)
@ViewerRestServiceBinding
@CORSBinding
public class AuthorizationFlowResource {

    private static final Logger logger = LogManager.getLogger(AuthorizationFlowResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public AuthorizationFlowResource(@Context HttpServletRequest request) {
    }

    @GET
    @jakarta.ws.rs.Path(AUTH_PROBE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 manifest for record")
    @IIIFPresentationBinding
    public AuthProbeService2 getProbeServiceDescription() {
        String baseUrl = DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "api/v2" + AUTH;
        return new AuthProbeService2(URI.create(baseUrl + AUTH_PROBE),
                Collections.singletonList(new AuthAccessService2(URI.create(baseUrl + AUTH_ACCESS), AuthAccessService2.Profile.ACTIVE,
                        new AuthAccessTokenService2(URI.create(baseUrl + AUTH_ACCESS_TOKEN)),
                        new AuthLogoutService2(URI.create(baseUrl + AUTH_LOGOUT)))));
    }

}
