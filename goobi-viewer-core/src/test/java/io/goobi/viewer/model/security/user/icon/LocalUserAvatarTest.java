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
package io.goobi.viewer.model.security.user.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.v1.authentication.UserAvatarResource;
import io.goobi.viewer.model.security.user.User;

class LocalUserAvatarTest extends AbstractTest {

    private static final Long USER_ID = 1L;

    private static User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setAvatarType(UserAvatarOption.LOCAL);
        return user;
    }

    private static HttpServletRequest createRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getContextPath()).thenReturn("/viewer");
        return request;
    }

    /**
     * @verifies return default avatar url if no image uploaded
     * @see LocalUserAvatar#getIconUrl(int, HttpServletRequest)
     */
    @Test
    void getIconUrl_shouldReturnDefaultAvatarUrlIfNoImageUploaded() {
        assertEquals("/viewer/resources/images/backend/thumbnail_goobi_person.svg",
                new LocalUserAvatar(createUser()).getIconUrl(80, createRequest()));
    }

    /**
     * @verifies return avatar api url if image uploaded
     * @see LocalUserAvatar#getIconUrl(int, HttpServletRequest)
     */
    @Test
    void getIconUrl_shouldReturnAvatarApiUrlIfImageUploaded() throws IOException {
        Path avatarFile = UserAvatarResource.getAvatarFilePath("avatar.jpg", USER_ID);
        Files.createDirectories(avatarFile.getParent());
        Files.createFile(avatarFile);
        try {
            String url = new LocalUserAvatar(createUser()).getIconUrl(80, createRequest());
            assertTrue(url.contains("/users/" + USER_ID + "/avatar/"), url);
        } finally {
            Files.deleteIfExists(avatarFile);
        }
    }
}
