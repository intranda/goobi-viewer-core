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

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;

class AuthenticationEndpointTest extends AbstractRestApiTest {

    /**
     * @see AuthenticationEndpoint#headerParameterLogin(String)
     * @verifies return status 403 if redirectUrl external
     */
    @Test
    void headerParameterLogin_shouldReturnStatus403IfRedirectUrlExternal() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_HEADER).build();
        try (Response response = target(url).queryParam("redirectUrl", "https://example.com")
                .request()
                .get()) {
            assertEquals(403, response.getStatus(), "Should return status 403");
            assertEquals(AuthenticationEndpoint.REASON_PHRASE_ILLEGAL_REDIRECT_URL, response.getStatusInfo().getReasonPhrase());
        }
    }

    /**
     * @see AuthenticationEndpoint#headerParameterLogin(String)
     * @verifies return status 403 if no httpHeader type provider configured
     */
    @Test
    void headerParameterLogin_shouldReturnStatus403IfNoHttpHeaderTypeProviderConfigured() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_HEADER).build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(403, response.getStatus(), "Should return status 403");
            assertEquals(AuthenticationEndpoint.REASON_PHRASE_NO_PROVIDERS_CONFIGURED, response.getStatusInfo().getReasonPhrase());
        }
    }

    /**
     * @see AuthenticationEndpoint#headerParameterLogin(String)
     * @verifies return status 403 if no matching provider found
     */
    @Test
    void headerParameterLogin_shouldReturnStatus403IfNoMatchingProviderFound() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_HEADER).build();
        DataManager.getInstance().getConfiguration().overrideValue("user.authenticationProviders.provider(6)[@enabled]", "true");
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(403, response.getStatus(), "Should return status 403");
            assertEquals(AuthenticationEndpoint.REASON_PHRASE_NO_PROVIDERS_CONFIGURED, response.getStatusInfo().getReasonPhrase());
        }
    }
}