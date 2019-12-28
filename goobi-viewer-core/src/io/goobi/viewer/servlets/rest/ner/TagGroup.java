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
package io.goobi.viewer.servlets.rest.ner;

import java.util.List;

/**
 * <p>TagGroup interface.</p>
 */
public interface TagGroup extends Comparable<TagGroup> {

    /**
     * <p>getPageOrder.</p>
     *
     * @return the page order of the first (or only) page in this group
     */
    public Integer getPageOrder();

    /**
     * <p>getPages.</p>
     *
     * @return the number of pages in this group
     */
    public int getPages();

    /**
     * <p>addTags.</p>
     *
     * @param tags a {@link java.util.List} object.
     */
    public void addTags(List<TagCount> tags);

    /**
     * <p>getTags.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<TagCount> getTags();
}
