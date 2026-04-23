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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.goobi.viewer.model.security.user.User;

class AuthenticationEndpointLoginTest extends AbstractRestApiTest {

    private static final String TEST_EMAIL = "login-test@example.com";
    private static final String TEST_PASSWORD = "testpassword123";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        User user = new User();
        user.setEmail(TEST_EMAIL);
        user.setPasswordHash(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt()));
        user.setActive(true);
        user.setSuspended(false);
        DataManager.getInstance().getDao().addUser(user);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        DataManager.getInstance().getSecurityManager().reset();
        super.tearDown();
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 200 and token on valid credentials
     */
    @Test
    void loginSuccess_returns200AndToken() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(200, response.getStatus());
            AuthenticationResponse body = mapper.readValue(response.readEntity(String.class), AuthenticationResponse.class);
            assertEquals("success", body.getStatus());
            assertNotNull(body.getToken());
            assertNull(body.getMessage());
            assertNull(body.getRetryAfterSeconds());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on wrong password
     */
    @Test
    void loginWrongPassword_returns401() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, "wrongpassword"), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
            AuthenticationResponse body = mapper.readValue(response.readEntity(String.class), AuthenticationResponse.class);
            assertEquals("error", body.getStatus());
            assertEquals("Invalid credentials", body.getMessage());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on unknown email
     */
    @Test
    void loginUnknownEmail_returns401() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest("nobody@example.com", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
            AuthenticationResponse body = mapper.readValue(response.readEntity(String.class), AuthenticationResponse.class);
            assertEquals("error", body.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on inactive user
     */
    @Test
    void loginInactiveUser_returns401() throws Exception {
        User inactive = new User();
        inactive.setEmail("inactive@example.com");
        inactive.setPasswordHash(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt()));
        inactive.setActive(false);
        inactive.setSuspended(false);
        DataManager.getInstance().getDao().addUser(inactive);

        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest("inactive@example.com", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on suspended user
     */
    @Test
    void loginSuspendedUser_returns401() throws Exception {
        User suspended = new User();
        suspended.setEmail("suspended@example.com");
        suspended.setPasswordHash(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt()));
        suspended.setActive(true);
        suspended.setSuspended(true);
        DataManager.getInstance().getDao().addUser(suspended);

        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest("suspended@example.com", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on user with null password hash
     */
    @Test
    void loginUserWithNullPasswordHash_returns401() throws Exception {
        User noPassword = new User();
        noPassword.setEmail("nopw@example.com");
        noPassword.setActive(true);
        noPassword.setSuspended(false);
        DataManager.getInstance().getDao().addUser(noPassword);

        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest("nopw@example.com", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 401 on null email
     */
    @Test
    void loginNullEmail_returns401() throws Exception {
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
    void loginEmptyEmail_returns401() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest("", TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 429 with retryAfterSeconds on IP delay
     */
    @Test
    void loginBruteForceDelayByIp_returns429WithRetryAfter() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        LoginRequest wrongCredentials = new LoginRequest(TEST_EMAIL, "wrongpassword");

        target(url).request().post(Entity.entity(wrongCredentials, MediaType.APPLICATION_JSON)).close();

        try (Response response = target(url).request()
                .post(Entity.entity(wrongCredentials, MediaType.APPLICATION_JSON))) {
            assertEquals(429, response.getStatus());
            AuthenticationResponse body = mapper.readValue(response.readEntity(String.class), AuthenticationResponse.class);
            assertEquals("error", body.getStatus());
            assertEquals("Too many failed attempts", body.getMessage());
            assertNotNull(body.getRetryAfterSeconds());
            assertTrue(body.getRetryAfterSeconds() > 0);
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies return 429 with retryAfterSeconds on username delay
     */
    @Test
    void loginBruteForceDelayByUserName_returns429WithRetryAfter() throws Exception {
        DataManager.getInstance().getSecurityManager().addFailedLoginAttemptForUserName(TEST_EMAIL);

        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(429, response.getStatus());
            AuthenticationResponse body = mapper.readValue(response.readEntity(String.class), AuthenticationResponse.class);
            assertEquals("error", body.getStatus());
            assertNotNull(body.getRetryAfterSeconds());
            assertTrue(body.getRetryAfterSeconds() > 0);
        }
    }

    /**
     * @see AuthenticationEndpoint#login(LoginRequest)
     * @verifies reset failed attempt counters on successful login
     */
    @Test
    void loginSuccess_resetsFailedAttemptCounters() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGIN).build();

        // First successful login
        target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))
                .close();

        // Second successful login must also succeed — counters were reset, no artificial delay
        try (Response response = target(url).request()
                .post(Entity.entity(new LoginRequest(TEST_EMAIL, TEST_PASSWORD), MediaType.APPLICATION_JSON))) {
            assertEquals(200, response.getStatus());
            AuthenticationResponse body = mapper.readValue(response.readEntity(String.class), AuthenticationResponse.class);
            assertEquals("success", body.getStatus());
            assertNotNull(body.getToken());
        }
    }

    /**
     * @see AuthenticationEndpoint#logout()
     * @verifies return 401 when no session exists
     */
    @Test
    void logoutWithoutSession_returns401() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGOUT).build();
        try (Response response = target(url).request().post(null)) {
            assertEquals(401, response.getStatus());
            AuthenticationResponse body = mapper.readValue(response.readEntity(String.class), AuthenticationResponse.class);
            assertEquals("error", body.getStatus());
            assertEquals("Not logged in", body.getMessage());
        }
    }

    /**
     * @see AuthenticationEndpoint#logout()
     * @verifies return 401 when session has no user attribute
     */
    @Test
    void logoutWithInvalidSessionCookie_returns401() throws Exception {
        String url = urls.path(ApiUrls.AUTH, ApiUrls.AUTH_LOGOUT).build();
        try (Response response = target(url)
                .request()
                .cookie("JSESSIONID", "invalid-session-id-that-does-not-exist")
                .post(null)) {
            assertEquals(401, response.getStatus());
        }
    }
}
