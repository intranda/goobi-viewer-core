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
package io.goobi.viewer.model.iiif.search.model;

import java.util.ArrayList;
import java.util.Collection;

import de.intranda.api.iiif.search.SearchTerm;

/**
 * A collection of search terms. If a {@link de.intranda.api.iiif.search.SearchTerm} is to be added which already exists in the list, the {@link de.intranda.api.iiif.search.SearchTerm#getCount()} is increased by one instead
 *
 * @author florian
 */
public class SearchTermList extends ArrayList<SearchTerm> {

    private static final long serialVersionUID = -8451140510669249168L;

    /**
     * <p>Constructor for SearchTermList.</p>
     */
    public SearchTermList() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * Adds the given term to the list if no term with the same {@link SearchTerm#getMatch()} exists. Otherwise add the
     * {@link SearchTerm#getCount()} of the given term to the existing term
     */
    @Override
    public boolean add(SearchTerm term) {
        int index = this.indexOf(term);
        if (index > -1) {
            this.get(index).incrementCount(term.getCount());
            return true;
        } else {
            return super.add(term);
        }
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#addAll(java.util.Collection)
     */
    /** {@inheritDoc} */
    @Override
    public boolean addAll(Collection<? extends SearchTerm> c) {
        for (SearchTerm searchTerm : c) {
            this.add(searchTerm);
        }
        return true;
    }
}
