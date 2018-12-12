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
package de.intranda.digiverso.presentation.model.search;

import java.util.List;

import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;

/**
 * Interface that all classes must implement that may be used in jsf search masks
 * {@link SearchBean} is the default implementation
 * 
 * @author Florian Alpers
 *
 */
public interface SearchInterface {

    public String searchSimple();
    
    public String searchAdvanced();
    
    public String resetSearch();
    
    public int getCurrentPage();
    
    public boolean isSearchInDcFlag();

    public SearchFacets getFacets();
    
    public List<String> autocomplete(String suggestion) throws IndexUnreachableException;
    
    public String getSearchString();
    
    public String getExactSearchString();
    
    public void setSearchString(String searchString);
    
    public List<SearchFilter> getSearchFilters();
    
    public String getCurrentSearchFilterString();
    
    public void setCurrentSearchFilterString(String filter);
    
    public int getActiveSearchType();
    
    public void setActiveSearchType(int type);

    public boolean isSearchPerformed();
    
    public long getHitsCount();
    
    public String getCurrentSearchUrlRoot();
}
