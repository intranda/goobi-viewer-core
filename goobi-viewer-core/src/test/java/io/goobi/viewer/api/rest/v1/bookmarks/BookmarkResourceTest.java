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
import io.goobi.viewer.api.rest.v1.bookmarks.BookmarkResource;

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

    /**
     * Tests for the parseMaxHits() helper that handles ?max=null and other invalid values.
     * This guards against NumberFormatException when a client sends the literal string "null"
     * as the value of the max query parameter.
     */
    /**
     * requireValidListId() is private but enforced in all list-specific endpoints.
     * A listId of 0 must be rejected with HTTP 400 before any business logic runs.
     */
    @Test
    void getBookmarkList_zeroListId_returns400() {
        try (Response response = target(USERS_BOOKMARKS + "/0")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(400, response.getStatus(), "listId=0 should be rejected with HTTP 400");
        }
    }

    /**
     * Negative listIds must also be rejected with HTTP 400.
     */
    @Test
    void getBookmarkList_negativeListId_returns400() {
        try (Response response = target(USERS_BOOKMARKS + "/-1")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(400, response.getStatus(), "listId=-1 should be rejected with HTTP 400");
        }
    }

    @Test
    void testParseMaxHits() {
        // null input → null output
        assertNull(BookmarkResource.parseMaxHits(null), "null string should parse to null");
        // literal "null" (sent by some clients) → null output
        assertNull(BookmarkResource.parseMaxHits("null"), "string 'null' should parse to null");
        assertNull(BookmarkResource.parseMaxHits("NULL"), "string 'NULL' should parse to null");
        // blank string → null output
        assertNull(BookmarkResource.parseMaxHits(""), "empty string should parse to null");
        assertNull(BookmarkResource.parseMaxHits("   "), "blank string should parse to null");
        // non-numeric → null output (no exception)
        assertNull(BookmarkResource.parseMaxHits("abc"), "non-numeric string should parse to null");
        // valid integer → correct Integer value
        assertEquals(Integer.valueOf(10), BookmarkResource.parseMaxHits("10"), "valid integer string should parse correctly");
        assertEquals(Integer.valueOf(0), BookmarkResource.parseMaxHits("0"), "zero should parse correctly");
    }

}
