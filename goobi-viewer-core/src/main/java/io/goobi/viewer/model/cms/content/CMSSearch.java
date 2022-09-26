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
package io.goobi.viewer.model.cms.content;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.itemfunctionality.SearchFunctionality;
import io.goobi.viewer.model.search.SearchHelper;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;

public class CMSSearch extends CMSContent {

    private static final String BACKEND_COMPONENT_NAME = "search";
    
    @Column(name = "search_prefix")
    private String searchPrefix = "";
    
    @Column(name = "displayEmptySearchResults")
    private boolean displayEmptySearchResults = false;

    @Column(name = "searchType")
    private int searchType = SearchHelper.SEARCH_TYPE_REGULAR;
    
    @Transient
    private final SearchFunctionality search;
    
    public CMSSearch() {
        this.search = initSearch();
    }
    
    public CMSSearch(CMSSearch orig) {
       super(orig);
       this.searchPrefix = orig.searchPrefix;
       this.displayEmptySearchResults = orig.displayEmptySearchResults;
       this.searchType = orig.searchType;
       this.search = initSearch();
    }
    
    private SearchFunctionality initSearch() {
        SearchFunctionality func = new SearchFunctionality(this.searchPrefix, this.getOwningComponent().getOwnerPage().getPageUrl());
        func.setPageNo(this.getOwningComponent().getListPage());
        func.setActiveSearchType(this.searchType);
        return func;
    }
    
    public void setSearchPrefix(String searchPrefix) {
        this.searchPrefix = searchPrefix;
    }
    
    public String getSearchPrefix() {
        return searchPrefix;
    }
    
    public void setDisplayEmptySearchResults(boolean displayEmptySearchResults) {
        this.displayEmptySearchResults = displayEmptySearchResults;
    }
    
    public boolean isDisplayEmptySearchResults() {
        return displayEmptySearchResults;
    }
    
    public void setSearchType(int searchType) {
        this.searchType = searchType;
    }
    
    public int getSearchType() {
        return searchType;
    }
    
    public SearchFunctionality getSearch() {
        return search;
    }

    @Override
    public String getBackendComponentName() {
        return BACKEND_COMPONENT_NAME;
    }

    @Override
    public CMSContent copy() {
        CMSSearch copy = new CMSSearch(this);
        return copy;
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

}
