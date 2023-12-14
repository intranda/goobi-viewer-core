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
package io.goobi.viewer.model.administration.legal;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.exceptions.JSONException;
import org.json.JSONObject;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Describes the locations in the viewer where a notification should be displayed. This can either be all pages or all record pages which may be
 * further restricted by a SOLR query condition
 *
 * @author florian
 *
 */
public class DisplayScope implements Serializable {

    private static final long serialVersionUID = 8408939885661597164L;

    /**
     * The type of viewer-pages that are in scope
     *
     * @author florian
     *
     */
    public enum PageScope {
        /**
         * all viewer pages, except those in the admin backend
         */
        ALL,
        /**
         * only pages belonging to a record/document
         */
        RECORD
    }

    private PageScope pageScope;
    private String filterQuery;

    /**
     * Creates a scope that appplies to all viewer pages
     */
    public DisplayScope() {
        this.pageScope = PageScope.ALL;
        this.filterQuery = "";
    }

    /**
     * Creates a scope from a given PageScope and filterQuery. The filterQuery is only meaningful if the scope is PageScope.RECORD
     *
     * @param scope
     * @param filter
     */
    public DisplayScope(PageScope scope, String filter) {
        this.pageScope = scope;
        this.filterQuery = filter;
    }

    public DisplayScope(String jsonString) throws IllegalArgumentException {
        try {
            JSONObject json = new JSONObject(jsonString);
            this.pageScope = PageScope.valueOf(json.getString("pageScope").toUpperCase());
            this.filterQuery = json.getString("filterQuery");
        } catch (NullPointerException | JSONException e) {
            throw new IllegalArgumentException("Cannot construct DisplayScope from string " + jsonString, e);
        }

    }

    /**
     * @return the pageScope
     */
    public PageScope getPageScope() {
        return pageScope;
    }

    /**
     * @param pageScope the pageScope to set
     */
    public void setPageScope(PageScope pageScope) {
        this.pageScope = pageScope;
    }

    /**
     * @return the filterQuery
     */
    public String getFilterQuery() {
        return filterQuery;
    }

    /**
     * @param filterQuery the filterQuery to set
     */
    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public boolean appliesToPage(PageType pageType, String pi, SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        if (this.pageScope.equals(PageScope.ALL)) {
            return !pageType.isAdminBackendPage();
        } else if (pageType != null && pageType.isDocumentPage()) {
            return matchesFilter(this.getQueryForSearch(), pi, searchIndex);
        } else {
            return false;
        }
    }

    /**
     *
     * @param query
     * @param pi
     * @param searchIndex
     * @return true if given pi is found using query; false otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static boolean matchesFilter(String query, String pi, SolrSearchIndex searchIndex)
            throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(pi) || "-".matches(pi)) {
            return false;
        } else if (StringUtils.isBlank(query)) {
            return true;
        } else {
            String singleRecordQuery = "+({1}) +{2}".replace("{1}", query).replace("{2}", "PI:" + pi);
            return searchIndex.getHitCount(singleRecordQuery) > 0;
        }
    }

    /**
     * Get the query to use in a SOLR search to deterimine whether a record should show the disclaimer
     *
     * @return a solr search query for the disclaimer
     */
    public String getQueryForSearch() {
        if (StringUtils.isBlank(this.filterQuery)) {
            return "";
        }

        return "+(" + this.filterQuery + ") +(ISWORK:* ISANCHOR:*)";
    }

    public String getAsJson() {
        JSONObject json = new JSONObject();
        json.put("pageScope", this.getPageScope());
        json.put("filterQuery", this.getFilterQuery());
        return json.toString();
    }

}
