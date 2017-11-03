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
package de.intranda.digiverso.presentation.model.cms.itemfunctionality;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.search.SearchHelper;

/**
 * @author Florian Alpers
 *
 */
public class SearchFunctionality implements Functionality {

    private static final Logger logger = LoggerFactory.getLogger(SearchFunctionality.class);
    
    /**
     * The current page of the search result list 
     */
    private int currentPage = 1;
    
    /**
     * 
     */
    private final int hitsPerPage;
    
    private final String baseUrl;

    
    /**
     * The query entered for a simple search 
     */
    private String simpleSearchQuery = "-";
    
    private String solrSortFields = "-";
    private String facetString = "-";
    private String collection = "-";
    
    private SearchBean searchBean;
    
    /**
     * @param searchPrefix
     */
    public SearchFunctionality(String searchPrefix, String baseUrl, int hitsPerPage) {
       this.facetString = searchPrefix;
       this.hitsPerPage = hitsPerPage;
       this.baseUrl = baseUrl;
    }
    
    
    public void resetSearch() throws PresentationException, IndexUnreachableException, DAOException {
        getSearchBean().resetSearchResults();
        getSearchBean().resetSearchParameters();
        getSearchBean().getFacets().resetCurrentFacetString();
        getSearchBean().resetSearchResults();
        setQueryString("");
        search();
    }
    
    public String searchSimple() {
        logger.trace("searchSimple");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
            return "";
        }
        setPageNo(1);
        getSearchBean().resetSearchResults();
        getSearchBean().resetSearchParameters();
        getSearchBean().getFacets().resetCurrentFacetString();
        getSearchBean().resetSearchResults();
        return "pretty:cmsOpenPageWithSearchSimple2";
    }
    
    public void search() throws PresentationException, IndexUnreachableException, DAOException {
        
        logger.trace("searchAction");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
            return;
        }
        getSearchBean().resetSearchResults();
        getSearchBean().setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
        getSearchBean().setHitsPerPage(getHitsPerPage());
//        getSearchBean().setExactSearchStringResetGui(getSimpleSearchQuery());
        getSearchBean().setSearchString(getSolrQuery());
        getSearchBean().setCurrentPage(getPageNo());
        getSearchBean().getFacets().setCurrentHierarchicalFacetString(getCollection());
        getSearchBean().getFacets().setCurrentFacetString(getFacetString());
        if(StringUtils.isNotBlank(getSolrSortFields())) {            
            getSearchBean().setSortString(getSolrSortFields());
        }
        getSearchBean().newSearch();
    }

    /**
     * @return the complete SOLR query string (query prefix + entered simple query)
     */
    public String getSolrQuery() {
        StringBuilder sb = new StringBuilder();
        if(StringUtils.isNotBlank(getSimpleSearchQuery())) {
            sb.append(getSimpleSearchQuery());
        } else {
            sb.append("*:*");
        }
//        if(StringUtils.isNotBlank(getSearchPrefix())) {
//            sb.append(" AND (").append(getSearchPrefix()).append(")");
//        }
        return sb.toString();
    }
    
    /**
     * The part of the search url before the page number
     * 
     * @return
     */
    public String getUrlPrefix() {
        StringBuilder sb = new StringBuilder();
        sb.append(getBaseUrl());
        sb.append("search/").append(getSimpleSearchQuery()).append("/");
        return sb.toString();
    }
    
    /**
     * The part of the search url after the page number
     * 
     * @return
     */
    public String getUrlSuffix() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSolrSortFields());
        sb.append("/").append(getFacetString());
        sb.append("/").append(getCollection());
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.itemfunctionality.Functionality#setPageNo(int)
     */
    @Override
    public void setPageNo(int pageNo) {
        this.currentPage = pageNo;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.itemfunctionality.Functionality#getPageNo()
     */
    @Override
    public int getPageNo() {
        return currentPage;

    }

    
    /**
     * @return the searchBean
     */
    public SearchBean getSearchBean() {
        if(this.searchBean == null) {
            this.searchBean = BeanUtils.getSearchBean();
        }
        return searchBean;
    }
    
    /**
     * @param simpleSearchQuery the simpleSearchQuery to set
     */
    public void setSimpleSearchQuery(String simpleSearchQuery) {
        if(StringUtils.isBlank(simpleSearchQuery)) {
            simpleSearchQuery = "-";
        }
        this.simpleSearchQuery = simpleSearchQuery;
    }
    
    /**
     * @return the simpleSearchQuery
     */
    public String getSimpleSearchQuery() {
        return simpleSearchQuery == null ? "" : simpleSearchQuery;
    }
    
    /**
     * @return the hitsPerPage
     */
    public int getHitsPerPage() {
        return hitsPerPage;
    }
    
    /**
     * @return the solrSortFields
     */
    public String getSolrSortFields() {
        return solrSortFields;
    }
    
    /**
     * @param solrSortFields the solrSortFields to set
     */
    public void setSolrSortFields(String solrSortFields) {
        this.solrSortFields = solrSortFields;
    }

    /**
     * @return the facetString
     */
    public String getFacetString() {
        return facetString;
    }

    /**
     * @param facetString the facetString to set
     */
    public void setFacetString(String facetString) {
        this.facetString = facetString;
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @param collection the collection to set
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }
    
    public String getQueryString() {
        if(StringUtils.isNotBlank(getSimpleSearchQuery().replace("-", ""))) {
            return getSimpleSearchQuery();
        } else {
            return "";
        }
    }
    
    public void setQueryString(String s) {
        if(s != null && StringUtils.isNotBlank(s.replace("-", ""))) {
            setSimpleSearchQuery(s);
        } else {
            setSimpleSearchQuery("");
        }
    }


    /**
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }


}
