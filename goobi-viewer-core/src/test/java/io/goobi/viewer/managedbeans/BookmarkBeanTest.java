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
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.exceptions.IllegalUrlParameterException;

class BookmarkBeanTest extends AbstractDatabaseEnabledTest {

    /**
     * @see BookmarkBean#setCurrentBookmarkListId(String)
     * @verifies throw IllegalUrlParameterException for non-numeric id
     */
    @Test
    void setCurrentBookmarkListId_shouldThrowForNonNumericId() {
        BookmarkBean bean = new BookmarkBean();
        assertThrows(IllegalUrlParameterException.class, () -> bean.setCurrentBookmarkListId("abc"));
    }

    /**
     * @see BookmarkBean#setCurrentBookmarkListId(String)
     * @verifies throw IllegalUrlParameterException if bookmark list not found
     */
    @Test
    void setCurrentBookmarkListId_shouldThrowIfBookmarkListNotFound() {
        BookmarkBean bean = new BookmarkBean();
        // DAO returns null for a non-existent ID; bl == null short-circuits before userBean.getUser()
        assertThrows(IllegalUrlParameterException.class, () -> bean.setCurrentBookmarkListId("99999"));
    }

    /**
     * @see BookmarkBean#setCurrentBookmarkListId(String)
     * @verifies not throw for null id
     */
    @Test
    void setCurrentBookmarkListId_shouldNotThrowForNull() {
        BookmarkBean bean = new BookmarkBean();
        assertDoesNotThrow(() -> bean.setCurrentBookmarkListId(null));
    }

}
