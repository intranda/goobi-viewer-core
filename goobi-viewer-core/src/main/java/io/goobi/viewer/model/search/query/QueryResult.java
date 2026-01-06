package io.goobi.viewer.model.search.query;

import java.util.Map;
import java.util.Set;

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
