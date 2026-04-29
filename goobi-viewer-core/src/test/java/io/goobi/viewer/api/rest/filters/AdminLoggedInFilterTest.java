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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SecurityManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserToken;

class AdminLoggedInFilterTest extends AbstractRestApiTest {

    // GET /temp/files/{folder} — TempMediaFileResource has class-level @AdminLoggedInBinding
    private static final String ADMIN_URL_PATH = ApiUrls.TEMP_MEDIA_FILES + "/testfolder";

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUpAdminFilterTests() throws DAOException {
        adminUser = new User();
        adminUser.setEmail("adminfiltertest@test.org");
        adminUser.setPasswordHash(BCrypt.hashpw("adminpass123", BCrypt.gensalt()));
        adminUser.setActive(true);
        adminUser.setSuspended(false);
        adminUser.setSuperuser(true);
        DataManager.getInstance().getDao().addUser(adminUser);

        regularUser = new User();
        regularUser.setEmail("regularfiltertest@test.org");
        regularUser.setPasswordHash(BCrypt.hashpw("regularpass123", BCrypt.gensalt()));
        regularUser.setActive(true);
        regularUser.setSuspended(false);
        regularUser.setSuperuser(false);
        DataManager.getInstance().getDao().addUser(regularUser);
    }

    @AfterEach
    void tearDownAdminFilterTests() throws DAOException {
        if (adminUser != null) {
            DataManager.getInstance().getDao().deleteUser(adminUser);
        }
        if (regularUser != null) {
            DataManager.getInstance().getDao().deleteUser(regularUser);
        }
    }

    /**
     * @see AdminLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies pass request through when valid admin bearer token provided
     */
    @Test
    void filter_shouldPassRequestThroughWhenValidAdminBearerTokenProvided() throws Exception {
        String plaintext = "admin-token-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(adminUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().plusDays(1));
        DataManager.getInstance().getDao().addUserToken(token);

        try (Response response = target(ADMIN_URL_PATH).request()
                .header("Authorization", "Bearer " + plaintext)
                .get()) {
            assertNotEquals(401, response.getStatus());
        }
    }

    /**
     * @see AdminLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies return 401 when bearer token belongs to non-admin user
     */
    @Test
    void filter_shouldReturn401WhenBearerTokenBelongsToNonAdminUser() throws Exception {
        String plaintext = "regular-token-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(regularUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().plusDays(1));
        DataManager.getInstance().getDao().addUserToken(token);

        try (Response response = target(ADMIN_URL_PATH).request()
                .header("Authorization", "Bearer " + plaintext)
                .get()) {
            assertEquals(401, response.getStatus());
        }
    }

    /**
     * @see AdminLoggedInFilter#filter(jakarta.ws.rs.container.ContainerRequestContext)
     * @verifies return 401 with token_expired when expired admin bearer token provided
     */
    @Test
    void filter_shouldReturn401WithTokenExpiredWhenExpiredAdminBearerTokenProvided() throws Exception {
        String plaintext = "expired-admin-token-" + System.nanoTime();
        UserToken token = new UserToken();
        token.setUser(adminUser);
        token.setTokenHash(SecurityManager.hashToken(plaintext));
        token.setExpirationDate(LocalDateTime.now().minusSeconds(1));
        DataManager.getInstance().getDao().addUserToken(token);

        try (Response response = target(ADMIN_URL_PATH).request()
                .header("Authorization", "Bearer " + plaintext)
                .get()) {
            assertEquals(401, response.getStatus());
            String body = response.readEntity(String.class);
            assertTrue(body.contains("token_expired"));
        }
    }
}
