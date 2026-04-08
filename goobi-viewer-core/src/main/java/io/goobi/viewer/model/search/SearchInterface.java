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

import java.io.IOException;
import java.util.List;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.maps.GeoMap;

/**
 * Interface that all classes must implement that may be used in jsf search masks {@link io.goobi.viewer.managedbeans.SearchBean} is the default
 * implementation.
 *
 * @author Florian Alpers
 */
public interface SearchInterface {

    /**
     * Perform a simple Search.
     *
     * @return the url to navigate to, or an empty string if naviation is handled internally
     */
    public String searchSimple();

    /**
     * Perform an advanced Search.
     *
     * @return the url to navigate to, or an empty string if naviation is handled internally
     */
    public String searchAdvanced();

    /**
     * Resets the current search including all results and search parameters.
     *
     * @return the url to navigate to, or an empty string if naviation is handled internally
     */
    public String resetSearch();

    /**
     * Returns the current result list page number.
     *
     * @return the current result list page number
     */
    public int getCurrentPage();

    /**
     * Returns the last result list page number.
     *
     * @return the last result list page number
     */
    public int getLastPage();
    
    /**
     * @return The selected result group name or advanced search template
     */
    public String getActiveContext();

    /**
     * @param activeContext result group name or advanced search template to activate
     */
    public void setActiveContext(String activeContext);

    /**
     * Returns the search string for the selected sort option.
     *
     * @return the search string for the selected sort option
     */
    public String getSortString();

    /**
     * Sets the sorting search string.
     *
     * @param sortString sort field and direction encoded as search string
     */
    public void setSortString(String sortString);

    /**
     * Returns if search is performed only within a DC.
     *
     * @return a boolean.
     */
    public boolean isSearchInDcFlag();

    /**
     * Returns if search is performed only within a certain facet field.
     *
     * @param fieldName Solr facet field name to check
     * @return a boolean.
     */
    public boolean isSearchInFacetFieldFlag(String fieldName);

    /**
     * List all current {@link io.goobi.viewer.model.search.SearchFacets}.
     *
     * @return all current {@link io.goobi.viewer.model.search.SearchFacets}
     */
    public SearchFacets getFacets();

    /**
     * Returns suggestions for autocomplete.
     *
     * @param suggestion partial input string to complete
     * @return suggestions for autocomplete
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> autocomplete(String suggestion) throws IndexUnreachableException;

    /**
     * Gets the current search string for display.
     *
     * @return the current search string for display
     */
    public String getSearchString();

    /**
     * Gets the actual search string.
     *
     * @return the actual search string
     */
    public String getExactSearchString();

    /**
     * Sets the actual search string.
     *
     * @param searchString raw search query to set
     */
    public void setSearchString(String searchString);

    /**
     * Gets a list of all available search filters.
     *
     * @return a list of all available search filters
     */
    public List<SearchFilter> getSearchFilters();

    /**
     * Returns the current search filter as string.
     *
     * @return the current search filter as string
     */
    public String getCurrentSearchFilterString();

    /**
     * Sets the current search filter as string.
     *
     * @param filter search filter identifier string to activate
     */
    public void setCurrentSearchFilterString(String filter);

    /**
     * Get the currently active search type. The possible types are defined in {@link io.goobi.viewer.model.search.SearchHelper}
     *
     * @return the active search type
     */
    public int getActiveSearchType();

    /**
     * Set the search type to use. The possible types are defined in {@link io.goobi.viewer.model.search.SearchHelper}
     *
     * @param type search type constant as defined in SearchHelper
     */
    public void setActiveSearchType(int type);

    /**
     * Checks if a search has been performed and any results are to be excepted (provided the search yielded any).
     *
     * @return whether a search has been performed after the last reset
     */
    public boolean isSearchPerformed();

    /**
     * Checks if a search has been triggered by the user and not yet been reset.
     *
     * @return if a search has been triggered by the user and not yet been reset
     */
    public boolean isExplicitSearchPerformed();

    /**
     * Get total number of hits of the last search.
     *
     * @return the total number of hits of the last search
     */
    public long getHitsCount();

    /**
     * Action to execute to change the sort order of hits.
     * 
     * @return Navigation outcome
     * @throws IOException
     */
    public String changeSorting() throws IOException;

    /**
     * Returns the base url of the current search page, without any search parameters.
     *
     * @return the base url of the current search page, without any search parameters
     */
    public String getCurrentSearchUrlRoot();

    public boolean hasGeoLocationHits();

    public GeoMap getHitsMap();
}
