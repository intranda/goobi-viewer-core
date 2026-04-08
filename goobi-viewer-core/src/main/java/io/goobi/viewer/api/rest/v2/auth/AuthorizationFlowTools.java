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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.intranda.api.iiif.auth.v2.AuthAccessService2;
import de.intranda.api.iiif.auth.v2.AuthAccessTokenService2;
import de.intranda.api.iiif.auth.v2.AuthLogoutService2;
import de.intranda.api.iiif.auth.v2.AuthProbeService2;
import de.intranda.api.services.Service;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Utility class for building IIIF Authorization Flow v2 service descriptions. Assembles
 * probe, access, token, and logout service objects for protected resources and attaches
 * them to IIIF manifests or content resources.
 */
public final class AuthorizationFlowTools {

    public static final String PATH_PROBE = "/probe/";

    private AuthorizationFlowTools() {
    }

    /**
     * Wrapper method.
     * 
     * @param pi persistent identifier of the record
     * @param fileName file name of the resource to protect
     * @return List<Service>
     */
    public static List<Service> getAuthServices(String pi, String fileName) {
        return Collections.singletonList(getAuthServicesEmbedded(PATH_PROBE + pi + "/" + fileName + "/"));
    }

    public static List<Service> getAuthServices(String path) {
        return Collections.singletonList(getAuthServicesEmbedded(path));
    }

    static AuthProbeService2 getAuthServicesEmbedded(String pi, String fileName) {
        return getAuthServicesEmbedded(PATH_PROBE + pi + "/" + fileName + "/");
    }

    public static List<Service> getAuthServicesRecord(String pi, String privilege) {
        return Collections.singletonList(getAuthServicesEmbedded(PATH_PROBE + pi + "/privilege/" + privilege));
    }

    public static List<Service> getAuthServicesStructure(String pi, String logId, String privilege) {
        return Collections.singletonList(getAuthServicesEmbedded(PATH_PROBE + pi + "/section/" + logId + "/privilege/" + privilege));
    }

    /**
     * 
     * @param path API endpoint path
     * @return {@link AuthProbeService2}
     */
    static AuthProbeService2 getAuthServicesEmbedded(String path) {
        String baseUrl = DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "api/v2" + AUTH;
        AuthProbeService2 ret = new AuthProbeService2(URI.create(baseUrl + path),
                Collections
                        .singletonList(
                                new AuthAccessService2(URI.create(baseUrl + AUTH_LOGIN), AuthAccessService2.Profile.ACTIVE, new HashMap<>(),
                                        new AuthAccessTokenService2(URI.create(baseUrl + AUTH_ACCESS_TOKEN)),
                                        new AuthLogoutService2(URI.create(baseUrl + AUTH_LOGOUT)))));

        for (Locale locale : ViewerResourceBundle.getAllLocales()) {
            AuthAccessService2 loginService = ret.getService().get(0);
            loginService.getLabel().put(locale.getLanguage(), ViewerResourceBundle.getTranslation("login", locale));
            loginService.getConfirmLabel().put(locale.getLanguage(), ViewerResourceBundle.getTranslation("login", locale));
            loginService.getLogoutService().addLabel(locale.getLanguage(), ViewerResourceBundle.getTranslation("logout", locale));
        }

        return ret;
    }
}
