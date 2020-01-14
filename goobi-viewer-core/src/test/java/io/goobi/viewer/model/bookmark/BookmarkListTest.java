/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.bookmark;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;

public class BookmarkListTest extends AbstractSolrEnabledTest {

    /**
     * @see BookmarkList#generateSolrQueryForItems()
     * @verifies return correct query
     */
    @Test
    public void generateSolrQueryForItems_shouldReturnCorrectQuery() throws Exception {
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
        Assert.assertEquals("(PI:PI1) OR (PI_TOPSTRUCT:PI2 AND LOGID:LOG1) OR (URN:URN1 OR IMAGEURN:URN1)", query);
    }

    /**
     * @see BookmarkList#getMiradorJsonObject()
     * @verifies generate JSON object correctly
     */
    @Test
    public void getMiradorJsonObject_shouldGenerateJSONObjectCorrectly() throws Exception {
        BookmarkList bookmarkList = new BookmarkList();
        for (int i = 1; i <= 16; ++i) {
            Bookmark item = new Bookmark();
            item.setPi("PI" + i);
            bookmarkList.getItems().add(item);
        }

        String json = bookmarkList.getMiradorJsonObject("/viewer");
        Assert.assertFalse(StringUtils.isBlank(json));
        // TODO check json contents
    }

    /**
     * @see BookmarkList#getFilterQuery()
     * @verifies construct query correctly
     */
    @Test
    public void getFilterQuery_shouldConstructQueryCorrectly() throws Exception {
        BookmarkList bookmarkList = new BookmarkList();
        for (int i = 1; i <= 4; ++i) {
            Bookmark item = new Bookmark();
            item.setPi("PI" + i);
            bookmarkList.getItems().add(item);
        }

        Assert.assertEquals("+( PI:PI1 PI:PI2 PI:PI3 PI:PI4)", bookmarkList.getFilterQuery());
    }
}