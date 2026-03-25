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
package io.goobi.viewer.api.rest.v1.bookmarks;

import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;

/**
 * @author florian
 *
 */
class BookmarkResourceTest extends AbstractRestApiTest {

    @Test
    void testDeserializeBookmark() throws JsonMappingException, JsonProcessingException {
        String jsonString = "{\"name\": \"Test Bookmark\", \"description\": \"some testing...\", \"pi\": \"PPN743674162\"}";
        ObjectMapper mapper = new ObjectMapper();
        Bookmark bookmark = mapper.readValue(jsonString, Bookmark.class);
        assertNotNull(bookmark);
        assertEquals("Test Bookmark", bookmark.getName());
    }

    /**
     * Verify that POST /bookmarks returns 400 when called without a logged-in user.
     * Session-based bookmark lists do not support adding additional lists.
     * The success path (201 Created) requires authentication and is verified at the source level
     * in BookmarkResource.addBookmarkList() which wraps the result in Response.status(CREATED).
     */
    @Test
    void testAddBookmarkList_returns400WhenNotLoggedIn() {
        BookmarkList list = new BookmarkList();
        list.setName("Test List");
        try (Response response = target(USERS_BOOKMARKS)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(list, MediaType.APPLICATION_JSON))) {
            assertEquals(400, response.getStatus(), "POST /bookmarks without login should return 400 Bad Request");
        }
    }

}
