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
package io.goobi.viewer.model.cms.itemfunctionality;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchFilter;
import io.goobi.viewer.model.search.SearchInterface;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;

/**
 * <p>
 * SearchFunctionality class.
 * </p>
 *
 * @author Florian Alpers
 */
public class SearchFunctionality implements Functionality, SearchInterface {

    private static final Logger logger = LogManager.getLogger(SearchFunctionality.class);

    /**
     * The current page of the search result list
     */

    /**
     *
     */
    private final String baseUrl;
    private final String pageFacetString;

    /**
     * <p>
     * Constructor for SearchFunctionality.
     * </p>
     *
     * @param pageFacetString a {@link java.lang.String} object.
     * @param baseUrl a {@link java.lang.String} object.
     */
    public SearchFunctionality(String pageFacetString, String baseUrl) {
        this.pageFacetString = pageFacetString;
        this.baseUrl = baseUrl;
    }

    /** {@inheritDoc} */
    @Override
    public String resetSearch() {
        getSearchBean().resetSearchAction();
        redirectToSearchUrl(false);
        return "";
    }

    /**
     * <p>
     * redirectToSearchUrl.
     * </p>
     *
     * @param keepUrlParameter a boolean.
     */
    public void redirectToSearchUrl(boolean keepUrlParameter) {
        try {
            ViewerPathBuilder.createPath(BeanUtils.getRequest(), this.baseUrl).ifPresent(path -> {
                if (path != null) {
                    if (keepUrlParameter) {
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

    /** {@inheritDoc} */
    @Override
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

    /** {@inheritDoc} */
    @Override
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

    /**
     * <p>
     * searchFacetted.
     * </p>
     */
    public void searchFacetted() {
        logger.trace("searchSimple");
        if (getSearchBean() == null) {
            logger.error("Cannot search: SearchBean is null");
        } else {
            getSearchBean().searchSimple(true, true);
            redirectToSearchUrl(true);
        }
    }

    /**
     * <p>
     * search.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void search(String subtheme) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("searchAction");
        SearchBean searchBean = getSearchBean();
        if (searchBean == null) {
            logger.error("Cannot search: SearchBean is null");
            return;
        }
        searchBean.search(getCompleteFilterString(subtheme));
    }

    /**
     * @return
     */
    private String getCompleteFilterString(String subtheme) {

        String filterString = getPageFacetString();
        String subthemeFilter = getSubthemeFilter(subtheme);

        if (StringUtils.isNoneBlank(subthemeFilter, filterString)) {
            return "+($1) +($2)".replace("$1", filterString).replace("$2", subthemeFilter);
        } else if (StringUtils.isNotBlank(filterString)) {
            return filterString;
        } else if (StringUtils.isNotBlank(subthemeFilter)) {
            return subthemeFilter;
        } else {
            return "";
        }
    }

    /**
     * @param subtheme
     * @return
     */
    private static String getSubthemeFilter(String subtheme) {
        if (StringUtils.isNotBlank(subtheme)) {
            String subthemeDiscriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
            return subthemeDiscriminatorField + ":" + subtheme;
        }

        return "";
    }

    /**
     * The part of the search url before the page number
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUrlPrefix() {
        StringBuilder sb = new StringBuilder();
        sb.append(getBaseUrl());
        sb.append(getQueryString()).append("/");
        return sb.toString();
    }

    /**
     * <p>
     * getUrlSuffix.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUrlSuffix() {
        return getUrlSuffix(getSortString());
    }

    /**
     * The part of the search url after the page number
     *
     * @param solrSortFields a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getUrlSuffix(String solrSortFields) {
        StringBuilder sb = new StringBuilder();
        sb.append(solrSortFields).append("/");
        sb.append(getFacetString()).append("/");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.itemfunctionality.Functionality#setPageNo(int)
     */
    /** {@inheritDoc} */
    @Override
    public void setPageNo(int pageNo) {

        Optional.ofNullable(getSearchBean()).ifPresent(bean -> bean.setCurrentPage(pageNo));
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.itemfunctionality.Functionality#getPageNo()
     */
    /** {@inheritDoc} */
    @Override
    public int getPageNo() {
        return Optional.ofNullable(getSearchBean()).map(SearchBean::getCurrentPage).orElse(1);
    }

    /** {@inheritDoc} */
    @Override
    public int getCurrentPage() {
        return getPageNo();
    }

    /**
     * <p>
     * getSearchBean.
     * </p>
     *
     * @return the searchBean
     */
    public SearchBean getSearchBean() {
        return BeanUtils.getSearchBean();
    }

    /**
     * <p>
     * getHitsPerPage.
     * </p>
     *
     * @return the hitsPerPage
     */
    public int getHitsPerPage() {
        return getSearchBean().getHitsPerPage();
    }

    /** {@inheritDoc} */
    @Override
    public String getSortString() {
        return getSearchBean().getSortString();
    }

    /** {@inheritDoc} */
    @Override
    public void setSortString(String solrSortFields) {
        getSearchBean().setSortString(solrSortFields);
    }

    /**
     * <p>
     * getFacetString.
     * </p>
     *
     * @return the facetString
     */
    public String getFacetString() {
        return getSearchBean().getFacets().getActiveFacetString();
    }

    /**
     * <p>
     * setFacetString.
     * </p>
     *
     * @param facetString the facetString to set
     */
    public void setFacetString(String facetString) {
        getSearchBean().getFacets().setActiveFacetString(facetString);
    }

    /**
     * <p>
     * getQueryString.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getQueryString() {
        return getSearchBean().getExactSearchString();
    }

    /** {@inheritDoc} */
    @Override
    public String getExactSearchString() {
        return getQueryString();
    }

    /**
     * <p>
     * setQueryString.
     * </p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setQueryString(String s) {
        getSearchBean().setExactSearchString(s);
    }

    /**
     * <p>
     * Getter for the field <code>baseUrl</code>.
     * </p>
     *
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    private URI getParameterPath() {
        URI path = URI.create("");
        //        path = ViewerPathBuilder.resolve(path, getCollection());
        // URL-encoder query, if necessary (otherwise, exceptions might occur)
        String queryString = getQueryString();
        path = ViewerPathBuilder.resolve(path, queryString);
        path = ViewerPathBuilder.resolve(path, Integer.toString(getPageNo()));
        path = ViewerPathBuilder.resolve(path, getSortString());
        path = ViewerPathBuilder.resolve(path, getFacetString());
        return path;
    }

    /**
     * <p>
     * Getter for the field <code>pageFacetString</code>.
     * </p>
     *
     * @return the pageFacetString
     */
    public String getPageFacetString() {
        return pageFacetString;
    }

    /**
     * <p>
     * getNewSearchUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getNewSearchUrl() {
        return getSortUrl("-", false);
    }

    public String changeSorting() throws IOException {
        String sortString = getSearchBean().getSortString();
        String url = getSortUrl(sortString, false);
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect(url);
        return "";
    }

    /**
     * <p>
     * getSortUrl.
     * </p>
     *
     * @param sortString a {@link java.lang.String} object.
     * @param descending a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String getSortUrl(String sortString, boolean descending) {
        sortString = (descending ? "!" : "") + sortString;
        return getUrlPrefix() + getPageNo() + "/" + getUrlSuffix(sortString);
    }

    /**
     * <p>
     * getFacettedUrl.
     * </p>
     *
     * @param facetString a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getFacettedUrl(String facetString) {
        Path path = Paths.get(getBaseUrl());
        //        path = path.resolve(getCollection());
        path = path.resolve(getQueryString());
        path = path.resolve(Integer.toString(getPageNo()));
        path = path.resolve(getSortString());
        path = path.resolve(facetString);
        return path.toString();
    }

    /**
     * <p>
     * removeFacet.
     * </p>
     *
     * @param facet a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String removeFacet(String facet) {
        final String currentFacetString = getSearchBean().getFacets().getActiveFacetString();
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

    /**
     * <p>
     * getCurrentPagePath.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCurrentPagePath() {
        Optional<ViewerPath> viewerPath = ViewHistory.getCurrentView(BeanUtils.getRequest());
        if (!viewerPath.isPresent()) {
            return "";
        }

        ViewerPath path = viewerPath.get();
        if (path != null) {
            return path.getApplicationName() + path.getPrettifiedPagePath();
        }

        return "";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#isSearchInDcFlag()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isSearchInDcFlag() {
        return getSearchBean().isSearchInDcFlag();
    }

    @Override
    public boolean isSearchInFacetFieldFlag(String fieldName) {
        return getSearchBean().isSearchInFacetFieldFlag(fieldName);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getFacets()
     */
    /** {@inheritDoc} */
    @Override
    public SearchFacets getFacets() {
        return getSearchBean().getFacets();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#autocomplete(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public List<String> autocomplete(String suggestion) throws IndexUnreachableException {
        return getSearchBean().autocomplete(suggestion);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getSearchString()
     */
    /** {@inheritDoc} */
    @Override
    public String getSearchString() {
        return getSearchBean().getSearchString();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getSearchFilters()
     */
    /** {@inheritDoc} */
    @Override
    public List<SearchFilter> getSearchFilters() {
        return getSearchBean().getSearchFilters();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getCurrentSearchFilterString()
     */
    /** {@inheritDoc} */
    @Override
    public String getCurrentSearchFilterString() {
        return getSearchBean().getCurrentSearchFilterString();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#setCurrentSearchFilterString(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public void setCurrentSearchFilterString(String filter) {
        getSearchBean().setCurrentSearchFilterString(filter);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getActiveSearchType()
     */
    /** {@inheritDoc} */
    @Override
    public int getActiveSearchType() {
        return getSearchBean().getActiveSearchType();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#setActiveSearchType(int)
     */
    /** {@inheritDoc} */
    @Override
    public void setActiveSearchType(int type) {
        getSearchBean().setActiveSearchType(type);

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#setSearchString(java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public void setSearchString(String searchString) {
        getSearchBean().setSearchString(searchString);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#isSearchPerformed()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isSearchPerformed() {
        return getSearchBean().isSearchPerformed();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getHitsCount()
     */
    /** {@inheritDoc} */
    @Override
    public long getHitsCount() {
        return getSearchBean().getHitsCount();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getCurrentSearchUrlRoot()
     */
    /** {@inheritDoc} */
    @Override
    public String getCurrentSearchUrlRoot() {
        if (getBaseUrl().endsWith("/")) {
            return getBaseUrl().substring(0, getBaseUrl().length() - 1);
        }

        return getBaseUrl();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getLastPage()
     */
    /** {@inheritDoc} */
    @Override
    public int getLastPage() {
        return getSearchBean().getLastPage();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#isExplicitSearchPerformed()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isExplicitSearchPerformed() {
        return StringUtils.isNotBlank(getExactSearchString().replace("-", ""));
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#hasGeoLocationHits()
     */
    @Override
    public boolean hasGeoLocationHits() {
        return getSearchBean().hasGeoLocationHits();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#getHitsMap()
     */
    @Override
    public GeoMap getHitsMap() {
        return getSearchBean().getHitsMap();
    }

}
