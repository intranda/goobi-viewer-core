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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.search.SearchFacets;
import de.intranda.digiverso.presentation.model.search.SearchFilter;
import de.intranda.digiverso.presentation.model.search.SearchInterface;
import de.intranda.digiverso.presentation.model.urlresolution.ViewHistory;
import de.intranda.digiverso.presentation.model.urlresolution.ViewerPath;
import de.intranda.digiverso.presentation.model.urlresolution.ViewerPathBuilder;

/**
 * @author Florian Alpers
 *
 */
public class SearchFunctionality implements Functionality, SearchInterface {

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

    public String resetSearch() {
        getSearchBean().resetSearchAction();
        redirectToSearchUrl(false);
        return "";
    }

    /**
     * @throws DAOException
     * 
     */
    public void redirectToSearchUrl(boolean keepUrlParameter) {
        try {
            ViewerPathBuilder.createPath(BeanUtils.getRequest(), this.baseUrl).ifPresent(path -> {
                if (path != null) {
                    if(keepUrlParameter) {                        
                        path.setParameterPath(getParameterPath());
                    }
                    final FacesContext context = FacesContext.getCurrentInstance();
                    String redirectUrl = path.getApplicationName() + path.getCombinedPrettyfiedUrl();
                    try {
                        context.getExternalContext().redirect(redirectUrl);
                    } catch (IOException e) {
                        logger.error("Failed to redirect to url", e);
                    }
                }
            });
        } catch (DAOException e) {
            logger.error("Error retrieving search url", e);
        }
    }

    public String searchSimple() {
        logger.trace("searchSimple");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
        } else {
            getSearchBean().searchSimple(true, false);
            redirectToSearchUrl(true);
        }
        return "";
    }

    public String searchAdvanced() {
        logger.trace("searchAdvanced");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
        } else {
            getSearchBean().searchAdvanced();
            redirectToSearchUrl(true);
        }
        return "";
    }

    public void searchFacetted() {
        logger.trace("searchSimple");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
        } else {
            getSearchBean().searchSimple();
            redirectToSearchUrl(true);
        }
    }

    public void search() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("searchAction");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
            return;
        }
            String facetString = getSearchBean().getFacets().getCurrentFacetString();
            getSearchBean().getFacets().setCurrentFacetString(getCompleteFacetString(getSearchBean().getFacets().getCurrentFacetString()));
            getSearchBean().search();
            getSearchBean().getFacets().setCurrentFacetString(facetString);
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
        sb.append(getQueryString()).append("/");
        return sb.toString();
    }

    public String getUrlSuffix() {
        return getUrlSuffix(getSortString());
    }

    /**
     * The part of the search url after the page number
     * 
     * @return
     */
    public String getUrlSuffix(String solrSortFields) {
        StringBuilder sb = new StringBuilder();
        sb.append(solrSortFields).append("/");
        sb.append(getFacetString()).append("/");
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

    public int getCurrentPage() {
        return getPageNo();
    }
    
    /**
     * @return the searchBean
     */
    public SearchBean getSearchBean() {
        return BeanUtils.getSearchBean();
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
    public String getSortString() {
        return getSearchBean().getSortString();
    }

    /**
     * @param solrSortFields the solrSortFields to set
     */
    public void setSortString(String solrSortFields) {
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

    //    /**
    //     * @return the collection
    //     */
    //    public String getCollection() {
    //        return getSearchBean().getFacets().getCurrentFacetString();
    //    }

    //    /**
    //     * @param collection the collection to set
    //     */
    //    public void setCollection(String collection) {
    //        getSearchBean().getFacets().setCurrentFacetString(collection);
    //    }

    public String getQueryString() {
        return getSearchBean().getExactSearchString();
    }
    
    public String getExactSearchString() {
        return getQueryString();
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

    private URI getParameterPath() {
        URI path = URI.create("");
        //        path = ViewerPathBuilder.resolve(path, getCollection());
        path = ViewerPathBuilder.resolve(path, getQueryString());
        path = ViewerPathBuilder.resolve(path, Integer.toString(getPageNo()));
        path = ViewerPathBuilder.resolve(path, getSortString());
        path = ViewerPathBuilder.resolve(path, getFacetString());
        return path;
    }

    /**
     * @return the pageFacetString
     */
    public String getPageFacetString() {
        return pageFacetString;
    }

    public String getNewSearchUrl() {
        return getSortUrl("-", false);
    }

    public String getSortUrl(String sortString, boolean descending) {
        sortString = (descending ? "!" : "") + sortString;
        return getUrlPrefix() + getPageNo() + "/" + getUrlSuffix(sortString);
    }

    public String getFacettedUrl(String facetString) {
        Path path = Paths.get(getBaseUrl());
        //        path = path.resolve(getCollection());
        path = path.resolve(getQueryString());
        path = path.resolve(Integer.toString(getPageNo()));
        path = path.resolve(getSortString());
        path = path.resolve(facetString);
        return path.toString();
    }

    public String removeFacet(String facet) {
        final String currentFacetString = getSearchBean().getFacets().getCurrentFacetString();
        String separator = ";;";
        try {
            separator = URLEncoder.encode(separator, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.toString(), e);
        }
        String facetString = Stream.of(currentFacetString.split(separator)).filter(s -> !s.equalsIgnoreCase(facet)).collect(Collectors.joining(";;"));
        if (StringUtils.isBlank(facetString)) {
            facetString = "-";
        }
        return facetString;
    }

    public String getCurrentPagePath() {
        ViewerPath path = ViewHistory.getCurrentView(BeanUtils.getRequest()).get();
        if (path != null) {
            return path.getApplicationName() + path.getPrettifiedPagePath();
        }
        return "";
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#isSearchInDcFlag()
     */
    @Override
    public boolean isSearchInDcFlag() {
        return getSearchBean().isSearchInDcFlag();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#getFacets()
     */
    @Override
    public SearchFacets getFacets() {
        return getSearchBean().getFacets();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#autocomplete(java.lang.String)
     */
    @Override
    public List<String> autocomplete(String suggestion) throws IndexUnreachableException {
        return getSearchBean().autocomplete(suggestion);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#getSearchString()
     */
    @Override
    public String getSearchString() {
        return getSearchBean().getSearchString();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#getSearchFilters()
     */
    @Override
    public List<SearchFilter> getSearchFilters() {
        return getSearchBean().getSearchFilters();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#getCurrentSearchFilterString()
     */
    @Override
    public String getCurrentSearchFilterString() {
        return getSearchBean().getCurrentSearchFilterString();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#setCurrentSearchFilterString(java.lang.String)
     */
    @Override
    public void setCurrentSearchFilterString(String filter) {
        getSearchBean().setCurrentSearchFilterString(filter);
    }
    

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#getActiveSearchType()
     */
    @Override
    public int getActiveSearchType() {
        return getSearchBean().getActiveSearchType();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#setActiveSearchType(int)
     */
    @Override
    public void setActiveSearchType(int type) {
        getSearchBean().setActiveSearchType(type);
        
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#setSearchString(java.lang.String)
     */
    @Override
    public void setSearchString(String searchString) {
        getSearchBean().setSearchString(searchString);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#isSearchPerformed()
     */
    @Override
    public boolean isSearchPerformed() {
        return getSearchBean().isSearchPerformed();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#getHitsCount()
     */
    @Override
    public long getHitsCount() {
        return getSearchBean().getHitsCount();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#getCurrentSearchUrlRoot()
     */
    @Override
    public String getCurrentSearchUrlRoot() {
        if(getBaseUrl().endsWith("/")) {
            return getBaseUrl().substring(0, getBaseUrl().length()-1);
        } else {
            return getBaseUrl();
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#getLastPage()
     */
    @Override
    public int getLastPage() {
        return getSearchBean().getLastPage();
    }
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.SearchInterface#isExplicitSearchPerformed()
     */
    @Override
    public boolean isExplicitSearchPerformed() {
        return StringUtils.isNotBlank(getExactSearchString().replace("-", ""));
    }

}
