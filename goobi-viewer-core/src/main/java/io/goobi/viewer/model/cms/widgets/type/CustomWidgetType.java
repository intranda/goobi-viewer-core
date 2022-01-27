package io.goobi.viewer.model.cms.widgets.type;

/**
 * Types of sidebar widgets that contain individual configuration and must be created by a user
 * 
 * @author florian
 *
 */
public enum CustomWidgetType implements WidgetContentType {
    
    /**
     * Displays an RSS feed. Number and sorting of feed item may be configured, as well as a search query to filter the feed items 
     */
    WIDGET_RSSFEED("cms__add_widget__select_rss_title", "cms__add_widget__select_rss_desc", "widget_rssFeed.xhtml"),
    /**
     * Display facets for a search field of type 'FACET_'. A filter query for facet results may be configured, as well as the order of facets
     */
    WIDGET_FIELDFACETS("cms__add_widget__select_facets_title", "cms__add_widget__select_facets_desc", "widget_fieldFacets.xhtml"),
    /**
     * Displays links to CMS pages. The linked pages can be selected when creating the widget
     */
    WIDGET_CMSPAGES("cms__add_widget__select_pages_title", "cms__add_widget__select_pages_desc", "widget_cmsPageLinks.xhtml"),
    /**
     * Display an html text
     */
    WIDGET_HTML("cms__add_widget__select_html_title", "cms__add_widget__select_html_desc", "widget_custom.xhtml");
    
    private final String label;
    private final String description;
    private final String filename;
    
    private CustomWidgetType(String label, String description, String filename) {
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
    
    @Override
    public String getFilename() {
        return this.filename;
    }
    
    @Override
    public String getName() {
        return name();
    }
}
