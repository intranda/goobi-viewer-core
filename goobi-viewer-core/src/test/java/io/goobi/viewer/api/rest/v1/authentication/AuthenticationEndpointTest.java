package io.goobi.viewer.api.rest.v1.authentication;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.junit.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.security.authentication.HttpHeaderProvider;

public class AuthenticationEndpointTest extends AbstractRestApiTest {

    /**
     * @see AuthenticationEndpoint#headerParameterLogin(String)
     * @verifies return status 403 if redirectUrl external
     */
    @Test
    public void headerParameterLogin_shouldReturnStatus403IfRedirectUrlExternal() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_HEADER).build();
        try (Response response = target(url).queryParam("redirectUrl", "https://example.com")
                .request()
                .get()) {
            assertEquals("Should return status 403", 403, response.getStatus());
            assertEquals(AuthenticationEndpoint.REASON_PHRASE_ILLEGAL_REDIRECT_URL, response.getStatusInfo().getReasonPhrase());
        }
    }

    /**
     * @see AuthenticationEndpoint#headerParameterLogin(String)
     * @verifies return status 403 if no httpHeader type provider configured
     */
    @Test
    public void headerParameterLogin_shouldReturnStatus403IfNoHttpHeaderTypeProviderConfigured() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_HEADER).build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals("Should return status 403", 403, response.getStatus());
            assertEquals(AuthenticationEndpoint.REASON_PHRASE_NO_PROVIDERS_CONFIGURED, response.getStatusInfo().getReasonPhrase());
        }
    }

    //    /**
    //     * @see AuthenticationEndpoint#headerParameterLogin(String)
    //     * @verifies return status 403 if no matching provider found
    //     */
    //    @Test
    //    public void headerParameterLogin_shouldReturnStatus403IfNoMatchingProviderFound() throws Exception {
    //        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_HEADER).build();
    //        assertEquals(5, DataManager.getInstance()
    //                .getConfiguration()
    //                .getAuthenticationProviders()
    //                .size());
    //        DataManager.getInstance()
    //                .getConfiguration()
    //                .getAuthenticationProviders()
    //                .add(new HttpHeaderProvider("HTTP", "HTTP", "https://locahost:8080/viewer/index", null, 60000, "header", "someOtherParameter"));
    //        assertEquals(6, DataManager.getInstance()
    //                .getConfiguration()
    //                .getAuthenticationProviders()
    //                .size());
    //        try (Response response = target(url)
    //                .request()
    //                .get()) {
    //            assertEquals("Should return status 403", 403, response.getStatus());
    //            assertEquals(AuthenticationEndpoint.REASON_PHRASE_NO_PROVIDER_FOUND, response.getStatusInfo().getReasonPhrase());
    //        }
    //    }
}