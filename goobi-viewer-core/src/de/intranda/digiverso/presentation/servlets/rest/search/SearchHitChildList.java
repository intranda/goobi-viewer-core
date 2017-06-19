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
package de.intranda.digiverso.presentation.servlets.rest.search;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import de.intranda.digiverso.presentation.model.search.SearchHit;

@XmlRootElement(name = "childHits")
public class SearchHitChildList {

    final private boolean hasMoreChildren;
    final private int hitsDisplayed;
    final private List<SearchHit> children;

    public SearchHitChildList(List<SearchHit> children, int hits, boolean hasMoreChildren) {
        this.hasMoreChildren = hasMoreChildren;
        this.hitsDisplayed = hits;
        this.children = Collections.unmodifiableList(children);
    }

    /**
     * @return the hitsDisplayed
     */
    public int getHitsDisplayed() {
        return hitsDisplayed;
    }

    /**
     * @return the hasMoreChildren
     */
    public boolean isHasMoreChildren() {
        return hasMoreChildren;
    }

    /**
     * @return the children
     */
    public List<SearchHit> getChildren() {
        return children;
    }

}
