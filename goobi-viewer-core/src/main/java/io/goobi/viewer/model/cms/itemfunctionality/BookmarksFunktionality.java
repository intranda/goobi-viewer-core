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
package io.goobi.viewer.model.cms.itemfunctionality;

import java.util.Collections;
import java.util.List;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.bookmark.BookmarkList;

/**
 * @author florian
 *
 */
public class BookmarksFunktionality implements Functionality {

    private int pageNo = 1;

    @Override
    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.itemfunctionality.Functionality#getPageNo()
     */
    @Override
    public int getPageNo() {
        return pageNo;
    }

    public List<BookmarkList> getBookmarkLists() throws DAOException {
        List<BookmarkList> lists = DataManager.getInstance().getDao().getPublicBookmarkLists();
        if (lists == null) {
            return Collections.emptyList();
        }
        return lists;
    }

}
