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
package io.goobi.viewer.model.bookmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.ViewerConfigurationException;

class BookmarkListTest extends AbstractSolrEnabledTest {

    AbstractApiUrlManager urls;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        urls = DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .orElseThrow(() -> new ViewerConfigurationException("Must configure current rest api url ('/api/v1'in urls.rest"));
    }

    /**
     * @see BookmarkList#generateSolrQueryForItems()
     * @verifies return correct query
     */
    @Test
    void generateSolrQueryForItems_shouldReturnCorrectQuery() throws Exception {
        BookmarkList bookmarkList = new BookmarkList();
        List<Bookmark> items = new ArrayList<>();

        Bookmark item = new Bookmark();
        item.setPi("PI1");
        items.add(item);

        item = new Bookmark();
        item.setPi("PI2");
        item.setLogId("LOG1");
        items.add(item);

        item = new Bookmark();
        item.setUrn("URN1");
        items.add(item);

        bookmarkList.setItems(items);
        String query = bookmarkList.generateSolrQueryForItems();
        Assertions.assertEquals("(PI:PI1) OR (PI_TOPSTRUCT:PI2 AND LOGID:LOG1 AND DOCTYPE:DOCSTRCT) OR (URN:URN1 OR IMAGEURN:URN1)", query);
    }

    /**
     * @see BookmarkList#getMiradorJsonObject()
     * @verifies generate JSON object correctly
     */
    @Test
    void getMiradorJsonObject_shouldGenerateJSONObjectCorrectly() throws Exception {
        BookmarkList bookmarkList = new BookmarkList();
        for (int i = 1; i <= 16; ++i) {
            Bookmark item = new Bookmark();
            item.setPi("PI" + i);
            bookmarkList.getItems().add(item);
        }

        String json = bookmarkList.getMiradorJsonObject(urls.getApplicationUrl(), urls.getApiUrl());
        Assertions.assertFalse(StringUtils.isBlank(json));

        JSONObject miradorConfig = new JSONObject(json);
        JSONArray dataList = miradorConfig.getJSONArray("data");
        assertEquals(16, dataList.length());
        JSONArray windowObjects = miradorConfig.getJSONArray("windowObjects");

        assertEquals(16, windowObjects.length());
        assertEquals(urls.getApplicationUrl() + BookmarkList.MIRADOR_LIB_PATH, miradorConfig.getString("buildPath"));

        for (int i = 1; i <= 16; ++i) {
            JSONObject link = dataList.getJSONObject(i - 1);
            JSONObject object = windowObjects.getJSONObject(i - 1);
            String pi = "PI" + i;
            String url = urls.path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_MANIFEST).params(pi).build();
            assertEquals(url, link.getString("manifestUri"));
            assertEquals(url, object.getString("loadedManifest"));
        }
    }

    /**
     * @see BookmarkList#getFilterQuery()
     * @verifies construct query correctly
     */
    @Test
    void getFilterQuery_shouldConstructQueryCorrectly() throws Exception {
        BookmarkList bookmarkList = new BookmarkList();
        for (int i = 1; i <= 4; ++i) {
            Bookmark item = new Bookmark();
            item.setPi("PI" + i);
            bookmarkList.getItems().add(item);
        }

        Assertions.assertEquals("+( PI:PI1 PI:PI2 PI:PI3 PI:PI4)", bookmarkList.getFilterQuery());
    }

    /**
     * @see BookmarkList#sortBookmarkLists(List)
     * @verifies sort lists correctly
     */
    @Test
    void sortBookmarkLists_shouldSortListCorrectly() throws Exception {
        List<BookmarkList> lists = new ArrayList<>();
        {
            BookmarkList list = new BookmarkList();
            list.setId(1L);
            lists.add(list);
        }
        {
            BookmarkList list = new BookmarkList();
            list.setId(2L);
            Bookmark bookmark = new Bookmark();
            bookmark.setDateAdded(LocalDateTime.of(2021, 2, 28, 0, 0));
            list.getItems().add(bookmark);
            lists.add(list);
        }
        {
            BookmarkList list = new BookmarkList();
            list.setId(3L);
            list.setDateUpdated(LocalDateTime.of(2021, 3, 1, 0, 0));
            lists.add(list);
        }

        BookmarkList.sortBookmarkLists(lists);
        Assertions.assertEquals(Long.valueOf(3), lists.get(0).getId());
        Assertions.assertEquals(Long.valueOf(2), lists.get(1).getId());
        Assertions.assertEquals(Long.valueOf(1), lists.get(2).getId());
    }
}
