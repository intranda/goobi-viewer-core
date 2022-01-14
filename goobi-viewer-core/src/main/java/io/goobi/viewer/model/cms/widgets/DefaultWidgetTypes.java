package io.goobi.viewer.model.cms.widgets;

/**
 * Types of widgets that are always available for CMS pages and cannot be configured
 * 
 * @author florian
 *
 */
public enum DefaultWidgetTypes {

    /**
     * Browsing or "Stöbern" widget, containing all browse terms which are configured in the viewer-config
     */
    WIDGET_BROWSING,
    /**
     * Displays search facetting for a page with search functionality. Always displays the facet fields configured in viewer-config 
     * Also includes chronology-facetting (by year) and geospatial facetting (on a map) which are displayed as independent widgets in the GUI
     */
    WIDGET_FACETTING,
    /**
     * Displays a search input field and link to advanced search
     */
    WIDGET_SEARCH,
    /**
     * Display the total number of records available in the viewer
     */
    WIDGET_WORKCOUNT;
}
