package io.goobi.viewer.model.cms.widgets;

/**
 * Types of sidebar widgets that contain individual configuration and must be created by a user
 * 
 * @author florian
 *
 */
public enum CustomWidgetTypes {
    
    /**
     * Displays an RSS feed. Number and sorting of feed item may be configured, as well as a search query to filter the feed items 
     */
    WIDGET_RSSFEED,
    /**
     * Display facets for a search field of type 'FACET_'. A filter query for facet results may be configured, as well as the order of facets
     */
    WIDGET_FIELDFACETS,
    /**
     * Displays links to CMS pages. The linked pages can be selected when creating the widget
     */
    WIDGET_CMSPAGES,
    /**
     * Display an html text
     */
    WIDGET_HTML;

}
