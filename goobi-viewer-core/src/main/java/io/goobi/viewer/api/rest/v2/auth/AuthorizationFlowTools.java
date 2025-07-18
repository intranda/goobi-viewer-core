package io.goobi.viewer.api.rest.v2.auth;

import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_ACCESS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_ACCESS_TOKEN;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_LOGOUT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_PROBE;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;

import de.intranda.api.iiif.auth.v2.AuthAccessService2;
import de.intranda.api.iiif.auth.v2.AuthAccessTokenService2;
import de.intranda.api.iiif.auth.v2.AuthLogoutService2;
import de.intranda.api.iiif.auth.v2.AuthProbeService2;
import io.goobi.viewer.controller.DataManager;

public final class AuthorizationFlowTools {

    private AuthorizationFlowTools() {
    }

    /**
     * 
     * @param pi
     * @param fileName
     * @return {@link AuthProbeService2}
     */
    public static AuthProbeService2 getAuthServices(String pi, String fileName) {
        String baseUrl = DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "api/v2" + AUTH;
        return new AuthProbeService2(URI.create(baseUrl + AUTH_PROBE + "/" + pi + "/" + fileName + "/"),
                Collections
                        .singletonList(
                                new AuthAccessService2(URI.create(baseUrl + AUTH_ACCESS), AuthAccessService2.Profile.ACTIVE, new HashMap<>(),
                                        new AuthAccessTokenService2(URI.create(baseUrl + AUTH_ACCESS_TOKEN)),
                                        new AuthLogoutService2(URI.create(baseUrl + AUTH_LOGOUT)).addLabel("en", "Logout"))));
    }
}
