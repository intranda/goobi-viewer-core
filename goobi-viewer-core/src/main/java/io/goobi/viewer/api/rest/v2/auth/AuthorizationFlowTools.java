package io.goobi.viewer.api.rest.v2.auth;

import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_ACCESS_TOKEN;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_LOGIN;
import static io.goobi.viewer.api.rest.v2.ApiUrls.AUTH_LOGOUT;

import java.net.URI;
import java.util.ArrayList;
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

public final class AuthorizationFlowTools {

    public static final String PATH_PROBE = "/probe/";

    private AuthorizationFlowTools() {
    }

    /**
     * Wrapper method.
     * 
     * @param pi
     * @param fileName
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

    /**
     * 
     * @param pi
     * @param fileName
     * @return List<Service>
     * @deprecated Added for testing, remove when finished.
     */
    @Deprecated
    private static List<Service> getAuthServicesFlat(String pi, String fileName) {
        String baseUrl = DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "api/v2" + AUTH;

        List<Service> ret = new ArrayList<>();
        AuthProbeService2 probeService = new AuthProbeService2(URI.create(baseUrl + PATH_PROBE + pi + "/" + fileName + "/"), null);
        ret.add(probeService);
        AuthAccessService2 loginService =
                new AuthAccessService2(URI.create(baseUrl + AUTH_LOGIN), AuthAccessService2.Profile.ACTIVE, new HashMap<>(), null, null);
        ret.add(loginService);
        AuthAccessTokenService2 tokenService = new AuthAccessTokenService2(URI.create(baseUrl + AUTH_ACCESS_TOKEN));
        ret.add(tokenService);
        AuthLogoutService2 logoutService = new AuthLogoutService2(URI.create(baseUrl + AUTH_LOGOUT));
        ret.add(logoutService);

        for (Locale locale : ViewerResourceBundle.getAllLocales()) {
            loginService.getLabel().put(locale.getLanguage(), ViewerResourceBundle.getTranslation("login", locale));
            loginService.getConfirmLabel().put(locale.getLanguage(), ViewerResourceBundle.getTranslation("login", locale));
            logoutService.addLabel(locale.getLanguage(), ViewerResourceBundle.getTranslation("logout", locale));
        }

        return ret;
    }
}
