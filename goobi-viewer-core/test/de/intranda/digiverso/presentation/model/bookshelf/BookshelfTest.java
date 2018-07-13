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
package de.intranda.digiverso.presentation.model.bookshelf;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractSolrEnabledTest;

public class BookshelfTest extends AbstractSolrEnabledTest {

    /**
     * @see Bookshelf#generateSolrQueryForItems()
     * @verifies return correct query
     */
    @Test
    public void generateSolrQueryForItems_shouldReturnCorrectQuery() throws Exception {
        Bookshelf bookshelf = new Bookshelf();
        List<BookshelfItem> items = new ArrayList<>();

        BookshelfItem item = new BookshelfItem();
        item.setPi("PI1");
        items.add(item);

        item = new BookshelfItem();
        item.setPi("PI2");
        item.setLogId("LOG1");
        items.add(item);

        item = new BookshelfItem();
        item.setUrn("URN1");
        items.add(item);

        bookshelf.setItems(items);
        String query = bookshelf.generateSolrQueryForItems();
        Assert.assertEquals("(PI:PI1) OR (PI_TOPSTRUCT:PI2 AND LOGID:LOG1) OR (URN:URN1 OR IMAGEURN:URN1)", query);
    }

    /**
     * @see Bookshelf#getMiradorJsonObject()
     * @verifies generate JSON object correctly
     */
    @Test
    public void getMiradorJsonObject_shouldGenerateJSONObjectCorrectly() throws Exception {
        Bookshelf bookshelf = new Bookshelf();
        for (int i = 1; i <= 16; ++i) {
            BookshelfItem item = new BookshelfItem();
            item.setPi("PI" + i);
            bookshelf.getItems().add(item);
        }

        String json = bookshelf.getMiradorJsonObject("/viewer");
        Assert.assertFalse(StringUtils.isBlank(json));
        // TODO check json contents
    }
}