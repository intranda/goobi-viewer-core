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
package io.goobi.viewer.api.rest.model.ner;

import java.util.List;

/**
 * Common interface for NER result containers that group {@link TagCount} entries by page scope — either a single page ({@link PageReference})
 * or a contiguous page range ({@link MultiPageReference}).
 * Extends {@link Comparable} so that groups can be sorted by page order.
 */
public interface TagGroup extends Comparable<TagGroup> {

    /**
     * getPageOrder.
     *
     * @return the page order of the first (or only) page in this group
     */
    public Integer getPageOrder();

    /**
     * getPages.
     *
     * @return the number of pages in this group
     */
    public int getPages();

    /**
     * addTags.
     *
     * @param tags list of tag counts to add to this group
     */
    public void addTags(List<TagCount> tags);

    /**
     * getTags.
     *
     * @return a list of NER tag counts contained in this group
     */
    public List<TagCount> getTags();
}
