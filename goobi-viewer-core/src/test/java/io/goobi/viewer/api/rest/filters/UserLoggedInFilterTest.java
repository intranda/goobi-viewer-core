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
package io.goobi.viewer.api.rest.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SecurityManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

class UserLoggedInFilterTest extends AbstractRestApiTest {

    private static final String TEST_EMAIL = "filtertest@test.org";
    private static final String TEST_PASSWORD = "filterpassword123";
    private static final String PROTECTED_URL_PATH = ApiUrls.CMS_MEDIA + ApiUrls.CMS_MEDIA_FILES;

    private User testUser;

    @BeforeEach
    void setUpFilterTests() throws DAOException {
        testUser = new User();
        testUser.setEmail(TEST_EMAIL);
        testUser.setPasswordHash(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt()));
        testUser.setActive(true);
        testUser.setSuspended(false);
        DataManager.getInstance().getDao().addUser(testUser);
    }

    @AfterEach
    void tearDownFilterTests() throws DAOException {
        if (testUser != null) {
            DataManager.getInstance().getDao().deleteUser(testUser);
            testUser = null;
        }
    }

    /**
     * @see UserLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies pass request through when valid bearer token provided
     */
    @Test
    void filter_shouldPassRequestThroughWhenValidBearerTokenProvided() throws Exception {
        String plaintext = "valid-test-token-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(testUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().plusDays(1));
        DataManager.getInstance().getDao().addUserToken(token);

        try (Response response = target(PROTECTED_URL_PATH).request()
                .header("Authorization", "Bearer " + plaintext)
                .get()) {
            assertNotEquals(401, response.getStatus(),
                    "Filter should pass valid token; got 401 which means it was rejected");
        }
    }

    /**
     * @see UserLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies return 401 with token_expired when expired bearer token provided
     */
    @Test
    void filter_shouldReturn401WithTokenExpiredWhenExpiredBearerTokenProvided() throws Exception {
        String plaintext = "expired-test-token-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(testUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().minusSeconds(1));
        DataManager.getInstance().getDao().addUserToken(token);

        try (Response response = target(PROTECTED_URL_PATH).request()
                .header("Authorization", "Bearer " + plaintext)
                .get()) {
            assertEquals(401, response.getStatus());
            String body = response.readEntity(String.class);
            assertTrue(body.contains("token_expired"));
        }
    }

    /**
     * @see UserLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies return 401 with invalid_token when unknown bearer token provided
     */
    @Test
    void filter_shouldReturn401WithInvalidTokenWhenUnknownBearerTokenProvided() throws Exception {
        try (Response response = target(PROTECTED_URL_PATH).request()
                .header("Authorization", "Bearer totally-unknown-token-xyz")
                .get()) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see UserLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies return 401 when no bearer token and no session
     */
    @Test
    void filter_shouldReturn401WhenNoBearerTokenAndNoSession() throws Exception {
        try (Response response = target(PROTECTED_URL_PATH).request().get()) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see UserLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies return 401 when bearer token belongs to inactive user
     */
    @Test
    void filter_shouldReturn401WhenBearerTokenBelongsToInactiveUser() throws Exception {
        testUser.setActive(false);
        DataManager.getInstance().getDao().updateUser(testUser);

        String plaintext = "inactive-user-token-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(testUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().plusDays(1));
        DataManager.getInstance().getDao().addUserToken(token);

        try (Response response = target(PROTECTED_URL_PATH).request()
                .header("Authorization", "Bearer " + plaintext)
                .get()) {
            assertEquals(401, response.getStatus());
            String body = response.readEntity(String.class);
            assertTrue(body.contains("user_inactive"));
        }
    }

    /**
     * @see UserLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies return 401 when bearer token belongs to suspended user
     */
    @Test
    void filter_shouldReturn401WhenBearerTokenBelongsToSuspendedUser() throws Exception {
        testUser.setSuspended(true);
        DataManager.getInstance().getDao().updateUser(testUser);

        String plaintext = "suspended-user-token-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(testUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().plusDays(1));
        DataManager.getInstance().getDao().addUserToken(token);

        try (Response response = target(PROTECTED_URL_PATH).request()
                .header("Authorization", "Bearer " + plaintext)
                .get()) {
            assertEquals(401, response.getStatus());
            String body = response.readEntity(String.class);
            assertTrue(body.contains("user_inactive"));
        }
    }

    /**
     * @see UserLoggedInFilter#getValidUserToken(HttpServletRequest)
     * @verifies return empty optional for expired token
     */
    @Test
    void getValidUserToken_shouldReturnEmptyForExpiredToken() throws Exception {
        String plaintext = "expired-valid-token-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(testUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().minusSeconds(1));
        DataManager.getInstance().getDao().addUserToken(token);

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + plaintext);

        Optional<UserToken> result = UserLoggedInFilter.getValidUserToken(mockRequest);
        assertFalse(result.isPresent());
    }

    /**
     * @see UserLoggedInFilter#getValidUserToken(HttpServletRequest)
     * @verifies return token for valid non-expired token
     */
    @Test
    void getValidUserToken_shouldReturnTokenForValidNonExpiredToken() throws Exception {
        String plaintext = "valid-token-check-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(testUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().plusDays(1));
        DataManager.getInstance().getDao().addUserToken(token);

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + plaintext);

        Optional<UserToken> result = UserLoggedInFilter.getValidUserToken(mockRequest);
        assertTrue(result.isPresent());
    }

    /**
     * @see UserLoggedInFilter#getValidUserToken(HttpServletRequest)
     * @verifies return empty optional when no bearer header present
     */
    @Test
    void getValidUserToken_shouldReturnEmptyWhenNoBearerHeader() throws Exception {
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getHeader("Authorization")).thenReturn(null);

        Optional<UserToken> result = UserLoggedInFilter.getValidUserToken(mockRequest);
        assertFalse(result.isPresent());
    }
}
