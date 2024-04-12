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
package io.goobi.viewer.api.rest.model.search;

import java.util.Collections;
import java.util.List;

import io.goobi.viewer.model.search.SearchHit;

/**
 * <p>
 * SearchHitChildList class.
 * </p>
 */
public class SearchHitChildList {

    private final boolean hasMoreChildren;
    private final int hitsDisplayed;
    private final List<SearchHit> children;

    /**
     * <p>
     * Constructor for SearchHitChildList.
     * </p>
     *
     * @param children a {@link java.util.List} object.
     * @param hits a int.
     * @param hasMoreChildren a boolean.
     */
    public SearchHitChildList(List<SearchHit> children, int hits, boolean hasMoreChildren) {
        this.hasMoreChildren = hasMoreChildren;
        this.hitsDisplayed = hits;
        this.children = Collections.unmodifiableList(children);
    }

    /**
     * <p>
     * Getter for the field <code>hitsDisplayed</code>.
     * </p>
     *
     * @return the hitsDisplayed
     */
    public int getHitsDisplayed() {
        return hitsDisplayed;
    }

    /**
     * <p>
     * isHasMoreChildren.
     * </p>
     *
     * @return the hasMoreChildren
     */
    public boolean isHasMoreChildren() {
        return hasMoreChildren;
    }

    /**
     * <p>
     * Getter for the field <code>children</code>.
     * </p>
     *
     * @return the children
     */
    public List<SearchHit> getChildren() {
        return children;
    }

}
