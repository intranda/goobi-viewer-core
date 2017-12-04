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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
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

    /**
     * 
     */
    private final String baseUrl;
    private final String pageFacetString;


//    private SearchBean searchBean;

    /**
     * @param searchPrefix
     */
    public SearchFunctionality(String pageFacetString, String baseUrl) {
        this.pageFacetString = pageFacetString;
        this.baseUrl = baseUrl;
    }

    public String resetSearch() throws PresentationException, IndexUnreachableException, DAOException {
        getSearchBean().resetSearchAction();
        return "pretty:cmsOpenPageWithSearch5";
    }

    public String searchSimple() throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("searchSimple");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
            return "";
        }
        getSearchBean().searchSimple();
        return "pretty:cmsOpenPageWithSearch5";
    }
    
    public String searchAdvanced() throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("searchSimple");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
            return "";
        }
        getSearchBean().searchAdvanced();
        return "pretty:cmsOpenPageWithSearch5";
    }

    public void search() throws PresentationException, IndexUnreachableException, DAOException {

        logger.trace("searchAction");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
            return;
        }
        getSearchBean().getFacets().setCurrentFacetString(getCompleteFacetString(getSearchBean().getFacets().getCurrentFacetString()));
        getSearchBean().search();
        
    }

    /**
     * @return
     */
    private String getCompleteFacetString(String baseFacetString) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(getPageFacetString())) {
            sb.append(getPageFacetString());
            if (StringUtils.isNotBlank(baseFacetString) && !"-".equals(baseFacetString)) {
                sb.append(";;").append(baseFacetString);
            }
        } else if (StringUtils.isNotBlank(baseFacetString) && !"-".equals(baseFacetString)) {
            sb.append(baseFacetString);
        }
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
        sb.append("search/").append(getQueryString()).append("/");
        return sb.toString();
    }

    public String getUrlSuffix() {
        return getUrlSuffix(getSolrSortFields());
    }

    /**
     * The part of the search url after the page number
     * 
     * @return
     */
    public String getUrlSuffix(String solrSortFields) {
        StringBuilder sb = new StringBuilder();
        sb.append(solrSortFields);
        sb.append("/").append(getFacetString());
        sb.append("/").append(getCollection());
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.itemfunctionality.Functionality#setPageNo(int)
     */
    @Override
    public void setPageNo(int pageNo) {
        getSearchBean().setCurrentPage(pageNo);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.itemfunctionality.Functionality#getPageNo()
     */
    @Override
    public int getPageNo() {
        return getSearchBean().getCurrentPage();
    }

    /**
     * @return the searchBean
     */
    public SearchBean getSearchBean() {
        return BeanUtils.getSearchBean();
//        if (this.searchBean == null) {
//            this.searchBean = BeanUtils.getSearchBean();
//        }
//        return searchBean;
    }

    /**
     * @return the hitsPerPage
     */
    public int getHitsPerPage() {
        return getSearchBean().getHitsPerPage();
    }

    /**
     * @return the solrSortFields
     */
    public String getSolrSortFields() {
        return getSearchBean().getSortString();
    }

    /**
     * @param solrSortFields the solrSortFields to set
     */
    public void setSolrSortFields(String solrSortFields) {
        getSearchBean().setSortString(solrSortFields);
    }

    /**
     * @return the facetString
     */
    public String getFacetString() {
        return getSearchBean().getFacets().getCurrentFacetString();
    }

    /**
     * @param facetString the facetString to set
     */
    public void setFacetString(String facetString) {
        getSearchBean().getFacets().setCurrentFacetString(facetString);
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return getSearchBean().getFacets().getCurrentHierarchicalFacetString();
    }

    /**
     * @param collection the collection to set
     */
    public void setCollection(String collection) {
        getSearchBean().getFacets().setCurrentHierarchicalFacetString(collection);
    }

    public String getQueryString() {
        return getSearchBean().getExactSearchString();
    }

    public void setQueryString(String s) {
        getSearchBean().setExactSearchString(s);
    }

    /**
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @return the pageFacetString
     */
    public String getPageFacetString() {
        return pageFacetString;
    }

    public String getSortUrl(String sortString, boolean descending) {
        sortString = (descending ? "!" : "") + sortString;
        return getUrlPrefix() + getPageNo() + "/" + getUrlSuffix(sortString);
    }
    
    public String removeFacet(String facet) throws UnsupportedEncodingException {
        final String currentFacetString = getSearchBean().getFacets().getCurrentFacetString();
        String facetString = Stream.of(currentFacetString.split(URLEncoder.encode(";;", "utf-8")))
        .filter(s -> !s.equalsIgnoreCase(facet))
        .collect(Collectors.joining(";;"));
        if(StringUtils.isBlank(facetString)) {
            facetString = "-";
        }
        return facetString;
    }
}
