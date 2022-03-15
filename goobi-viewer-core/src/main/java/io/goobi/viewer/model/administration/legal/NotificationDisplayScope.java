package io.goobi.viewer.model.administration.legal;

import java.io.Serializable;

/**
 * Describes the locations in the viewer where a notification should be displayed.
 * This can either be all pages or all record pages which may be further restricted by a SOLR query condition
 * 
 * @author florian
 *
 */
public class NotificationDisplayScope implements Serializable {

    private static final long serialVersionUID = 8408939885661597164L;

    public static enum Pages {
        ALL,
        RECORD
    }
    
    private Pages pageScope;
    private String filterQuery;
    
    /**
     * Creates a scope that appplies to all viewer pages
     */
    public NotificationDisplayScope() {
        this.pageScope = Pages.ALL;
        this.filterQuery = "";
    }
    
    /**
     * Creates a scope that applies to all record pages of records which meet the given solr filer
     * @param query
     */
    public NotificationDisplayScope(String filter) {
        this.pageScope = Pages.RECORD;
        this.filterQuery = filter;
    }

    /**
     * @return the pageScope
     */
    public Pages getPageScope() {
        return pageScope;
    }

    /**
     * @param pageScope the pageScope to set
     */
    public void setPageScope(Pages pageScope) {
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
    
    
    
}
