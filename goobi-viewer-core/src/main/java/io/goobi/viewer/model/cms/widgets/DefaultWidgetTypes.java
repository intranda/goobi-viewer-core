package io.goobi.viewer.model.cms.widgets;

/**
 * Types of widgets that are always available for CMS pages and cannot be configured
 * 
 * @author florian
 *
 */
public enum DefaultWidgetTypes implements WidgetContentType {

    /**
     * Browsing or "Stöbern" widget, containing all browse terms which are configured in the viewer-config
     */
    WIDGET_BROWSING("browseTitle", "cms_widget__browse__description", "widget_browsing.xhtml"),
    /**
     * Displays search facetting for a page with search functionality. Always displays the facet fields configured in viewer-config 
     * Also includes chronology-facetting (by year) and geospatial facetting (on a map) which are displayed as independent widgets in the GUI
     */
    WIDGET_FACETTING("faceting", "cms_widget__faceting__description", "widget_searchFacets.xhtml"),
    /**
     * Displays a search input field and link to advanced search
     */
    WIDGET_SEARCH("navigationSearch", "cms_widget__search__description", "widget_searchField.xhtml"),
    /**
     * Display the total number of records available in the viewer
     */
    WIDGET_WORKCOUNT("totalNumberOfVolumes", "cms_widget__total_number_of_volumes__description", "widget_workCount.xhtml");
    
    private final String label;
    private final String description;
    private final String filename;
    
    private DefaultWidgetTypes(String label, String description, String filename) {
        this.label = label;
        this.description = description;
        this.filename = filename;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getFilename() {
        return filename;
    }
}
