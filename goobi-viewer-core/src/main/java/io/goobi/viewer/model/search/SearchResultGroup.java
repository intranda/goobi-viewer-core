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
package io.goobi.viewer.model.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.maps.Location;

/**
 * Used for creating separate groups of search results for a single query. Each group can provide its own filter query for custom results.
 */
public class SearchResultGroup implements Serializable {

    private static final long serialVersionUID = 1421650450703418495L;

    private final String name;
    private final String query;
    private final int previewHitCount;
    /** If true, the name of this group will be used as the advanced search field configuration template name. */
    private final boolean useAsAdvancedSearchTemplate;

    /** Total hits count for the current search. */
    private long hitsCount = 0;

    /** BrowseElement list for the current search result page. */
    private final List<SearchHit> hits = new ArrayList<>();

    /** List of geo-locations found by the last search */
    private List<Location> hitLocationList = new ArrayList<>();

    private boolean hasGeoLocationHits = false;

    /**
     * 
     * @param name
     * @param query
     * @param previewHitCount
     * @param useAsAdvancedSearchTemplate
     */
    public SearchResultGroup(String name, String query, int previewHitCount, boolean useAsAdvancedSearchTemplate) {
        this.name = name;
        this.query = query;
        this.previewHitCount = previewHitCount;
        this.useAsAdvancedSearchTemplate = useAsAdvancedSearchTemplate;
    }

    /**
     * Create a copy of the given SearchResultGroup, without any search results
     * 
     * @param blueprint
     */
    public SearchResultGroup(SearchResultGroup blueprint) {
        this.name = blueprint.name;
        this.query = blueprint.query;
        this.previewHitCount = blueprint.previewHitCount;
        this.useAsAdvancedSearchTemplate = blueprint.useAsAdvancedSearchTemplate;
    }

    /**
     * 
     * @return Created {@link SearchResultGroup}
     */
    public static SearchResultGroup createDefaultGroup() {
        return createDefaultGroup("");
    }

    /**
     * @param query
     * @return Created {@link SearchResultGroup}
     */
    public static SearchResultGroup createDefaultGroup(String query) {
        return new SearchResultGroup(StringConstants.DEFAULT_NAME, query, -1, false);
    }

    /**
     * Returns a list of configured result groups or a default group if none are configured.
     * 
     * @return List<SearchResultGroup>
     * @should return correct groups
     * @should return default group if none are configured
     * @should return default group if groups disabled
     */
    public static List<SearchResultGroup> getConfiguredResultGroups() {
        List<SearchResultGroup> ret = DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled()
                ? DataManager.getInstance().getConfiguration().getSearchResultGroups() : new ArrayList<>(1);
        if (ret.isEmpty()) {
            ret.add(createDefaultGroup());
        }

        return ret;
    }

    /**
     * 
     * @return true if not default group and hitsCount larger than previewHitCount; false otherwise
     * @should return false if default group
     * @should return false if hitsCount not higher than previewHitCount
     * @should return true if hitsCount higher than previewHitCount
     */
    public boolean isDisplayExpandUrl() {
        return !StringConstants.DEFAULT_NAME.equals(name) && hitsCount > previewHitCount;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @return the previewHitCount
     */
    public int getPreviewHitCount() {
        return previewHitCount;
    }

    /**
     * @return the useAsAdvancedSearchTemplate
     */
    public boolean isUseAsAdvancedSearchTemplate() {
        return useAsAdvancedSearchTemplate;
    }

    /**
     * @return the hitsCount
     */
    public long getHitsCount() {
        return hitsCount;
    }

    /**
     * @param hitsCount the hitsCount to set
     */
    public void setHitsCount(long hitsCount) {
        this.hitsCount = hitsCount;
    }

    /**
     * @return the hitLocationList
     */
    public List<Location> getHitLocationList() {
        return hitLocationList;
    }

    /**
     * @param hitLocationList the hitLocationList to set
     */
    public void setHitLocationList(List<Location> hitLocationList) {
        this.hitLocationList = hitLocationList;
    }

    /**
     * @return the hasGeoLocationHits
     */
    public boolean isHasGeoLocationHits() {
        return hasGeoLocationHits;
    }

    /**
     * @param hasGeoLocationHits the hasGeoLocationHits to set
     */
    public void setHasGeoLocationHits(boolean hasGeoLocationHits) {
        this.hasGeoLocationHits = hasGeoLocationHits;
    }

    /**
     * @return the hits
     */
    public List<SearchHit> getHits() {
        return hits;
    }

}
