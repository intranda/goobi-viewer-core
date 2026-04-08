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
package io.goobi.viewer.model.search.query;

import java.util.Map;
import java.util.Set;

/**
 * Immutable value object holding the result of a parsed search query, containing the user-facing
 * display string, the internal Solr query, extracted search terms, and the proximity distance.
 */
public final class QueryResult {

    private final String displaySearchString;
    private final String internalQuery;
    private final int proximityDistance;
    private final Map<String, Set<String>> searchTerms;

    public QueryResult(String displaySearchString,
            String internalQuery, Map<String, Set<String>> searchTerms,
            int proximityDistance) {
        this.displaySearchString = displaySearchString;
        this.internalQuery = internalQuery;
        this.searchTerms = searchTerms;
        this.proximityDistance = proximityDistance;
    }

    public String getDisplaySearchString() {
        return displaySearchString;
    }

    public String getInternalQuery() {
        return internalQuery;
    }

    public int getProximityDistance() {
        return proximityDistance;
    }

    public Map<String, Set<String>> getSearchTerms() {
        return searchTerms;
    }
}
