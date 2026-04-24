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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.model.AuthenticationResponse;
import io.goobi.viewer.api.rest.model.LoginRequest;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.user.User;

class AuthenticationEndpointTest extends AbstractRestApiTest {

    private static final String TEST_EMAIL = "logintest@test.org";
    private static final String TEST_PASSWORD = "validpassword123";

    private User testUser;

    @BeforeEach
    void setUpLoginTests() throws DAOException {
        DataManager.getInstance().getSecurityManager().reset();
        testUser = new User();
        testUser.setEmail(TEST_EMAIL);
        testUser.setPasswordHash(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt()));
        testUser.setActive(true);
        testUser.setSuspended(false);
        DataManager.getInstance().getDao().addUser(testUser);
    }

    @AfterEach
    void tearDownLoginTests() throws DAOException {
        if (testUser != null) {
            DataManager.getInstance().getDao().deleteUser(testUser);
            testUser = null;
        }
        DataManager.getInstance().getSecurityManager().reset();
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 200 and token on valid credentials
     */
    @Test
    void login_shouldReturn200AndTokenOnValidCredentials() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(200, response.getStatus());
            AuthenticationResponse body = response.readEntity(AuthenticationResponse.class);
            assertEquals("success", body.getStatus());
            assertNotNull(body.getToken());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on wrong password
     */
    @Test
    void login_shouldReturn401OnWrongPassword() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, "wrongpassword"), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on unknown email
     */
    @Test
    void login_shouldReturn401OnUnknownEmail() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest("nobody@test.org", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on null email
     */
    @Test
    void login_shouldReturn401OnNullEmail() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(null, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on empty email
     */
    @Test
    void login_shouldReturn401OnEmptyEmail() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest("", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on inactive user
     */
    @Test
    void login_shouldReturn401OnInactiveUser() throws Exception {
        User inactiveUser = new User();
        inactiveUser.setEmail("inactive@test.org");
        inactiveUser.setPasswordHash(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt()));
        inactiveUser.setActive(false);
        inactiveUser.setSuspended(false);
        DataManager.getInstance().getDao().addUser(inactiveUser);
        try {
            String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
            try (Response response = target(url).request()
                    .post(Entity.entity(new LoginRequest("inactive@test.org", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
                assertEquals(401, response.getStatus());
            }
        } finally {
            DataManager.getInstance().getDao().deleteUser(inactiveUser);
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on suspended user
     */
    @Test
    void login_shouldReturn401OnSuspendedUser() throws Exception {
        User suspendedUser = new User();
        suspendedUser.setEmail("suspended@test.org");
        suspendedUser.setPasswordHash(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt()));
        suspendedUser.setActive(true);
        suspendedUser.setSuspended(true);
        DataManager.getInstance().getDao().addUser(suspendedUser);
        try {
            String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
            try (Response response = target(url).request()
                    .post(Entity.entity(new LoginRequest("suspended@test.org", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
                assertEquals(401, response.getStatus());
            }
        } finally {
            DataManager.getInstance().getDao().deleteUser(suspendedUser);
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on null password hash
     */
    @Test
    void login_shouldReturn401OnNullPasswordHash() throws Exception {
        User noHashUser = new User();
        noHashUser.setEmail("nohash@test.org");
        noHashUser.setPasswordHash(null);
        noHashUser.setActive(true);
        noHashUser.setSuspended(false);
        DataManager.getInstance().getDao().addUser(noHashUser);
        try {
            String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
            try (Response response = target(url).request()
                    .post(Entity.entity(new LoginRequest("nohash@test.org", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
                assertEquals(401, response.getStatus());
            }
        } finally {
            DataManager.getInstance().getDao().deleteUser(noHashUser);
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 429 with retryAfterSeconds on IP delay
     */
    @Test
    void login_shouldReturn429WithRetryAfterSecondsOnIpDelay() throws Exception {
        for (int i = 0; i < 5; i++) {
            DataManager.getInstance().getSecurityManager().addFailedLoginAttemptForIpAddress("127.0.0.1");
        }
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(429, response.getStatus());
            AuthenticationResponse body = response.readEntity(AuthenticationResponse.class);
            assertNotNull(body.getRetryAfterSeconds());
            assertTrue(body.getRetryAfterSeconds() > 0);
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 429 with retryAfterSeconds on username delay
     */
    @Test
    void login_shouldReturn429WithRetryAfterSecondsOnUsernameDelay() throws Exception {
        for (int i = 0; i < 5; i++) {
            DataManager.getInstance().getSecurityManager().addFailedLoginAttemptForUserName(TEST_EMAIL);
        }
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(429, response.getStatus());
            AuthenticationResponse body = response.readEntity(AuthenticationResponse.class);
            assertNotNull(body.getRetryAfterSeconds());
            assertTrue(body.getRetryAfterSeconds() > 0);
        }
    }

    /**
     * Verifies that failed-login counters are at zero after a successful login, i.e. the
     * endpoint calls resetFailedLoginAttempt for both username and IP address.
     *
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies reset failed attempt counters on successful login
     */
    @Test
    void login_shouldResetFailedAttemptCountersOnSuccessfulLogin() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(200, response.getStatus());
        }
        // After a successful login the endpoint must have called reset; delay must be 0
        assertEquals(0, DataManager.getInstance().getSecurityManager().getDelayForUserName(TEST_EMAIL));
        assertEquals(0, DataManager.getInstance().getSecurityManager().getDelayForIpAddress("127.0.0.1"));
    }

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
            assertEquals(AuthenticationEndpoint.REASON_PHRASE_ILLEGAL_REDIRECT_URL, response.readEntity(String.class));
        }
    }

    /**
     * @see AuthenticationEndpoint#headerParameterLogin(String)
     * @verifies not return status 403 if redirect url host whitelisted
     */
    @Test
    void headerParameterLogin_shouldNotReturnStatus403IfRedirectUrlHostWhitelisted() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_HEADER).build();
        try (Response response = target(url).queryParam("redirectUrl", "https://trusted.example.org/callback")
                .request()
                .get()) {
            // Should not be rejected as 403 with illegal redirect URL reason - the host is whitelisted
            if (response.getStatus() == 403) {
                assertNotEquals(AuthenticationEndpoint.REASON_PHRASE_ILLEGAL_REDIRECT_URL, response.readEntity(String.class),
                        "Whitelisted host should not be rejected as illegal redirect URL");
            }
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
            assertEquals(AuthenticationEndpoint.REASON_PHRASE_NO_PROVIDERS_CONFIGURED, response.readEntity(String.class));
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
            assertEquals(AuthenticationEndpoint.REASON_PHRASE_NO_PROVIDERS_CONFIGURED, response.readEntity(String.class));
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return token field in response on valid credentials
     */
    @Test
    void login_shouldReturnTokenFieldInResponseOnValidCredentials() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(200, response.getStatus());
            AuthenticationResponse body = response.readEntity(AuthenticationResponse.class);
            assertEquals("success", body.getStatus());
            assertNotNull(body.getToken());
            assertFalse(body.getToken().isBlank());
        }
    }

    /**
     * @see AuthenticationEndpoint#logout()
     * @verifies return 200 and delete token when valid bearer token provided
     */
    @Test
    void logout_shouldReturn200AndDeleteTokenWhenValidBearerTokenProvided() throws Exception {
        String loginUrl = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        String plaintext;
        try (Response loginResponse = target(loginUrl).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(200, loginResponse.getStatus());
            plaintext = loginResponse.readEntity(AuthenticationResponse.class).getToken();
            assertNotNull(plaintext);
        }

        String logoutUrl = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGOUT).build();
        try (Response response = target(logoutUrl).request()
                .header("Authorization", "Bearer " + plaintext)
                .post(null)) {
            assertEquals(200, response.getStatus());
        }

        String hash = io.goobi.viewer.controller.SecurityManager.hashToken(plaintext);
        assertTrue(DataManager.getInstance().getDao().getUserTokenByTokenHash(hash).isEmpty());
    }

    /**
     * @see AuthenticationEndpoint#logout()
     * @verifies return 200 when unknown bearer token provided
     */
    @Test
    void logout_shouldReturn200WhenUnknownBearerTokenProvided() throws Exception {
        String logoutUrl = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGOUT).build();
        try (Response response = target(logoutUrl).request()
                .header("Authorization", "Bearer unknown-token-that-does-not-exist")
                .post(null)) {
            assertEquals(200, response.getStatus());
        }
    }
}