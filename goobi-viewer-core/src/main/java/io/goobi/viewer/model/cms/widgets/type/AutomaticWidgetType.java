package io.goobi.viewer.model.cms.widgets.type;

/**
 * All types of sidebar widgets which are generated automatically if certain conditions are met, usually if certain CMS content exists
 * Currently (22-01) only CMS-Geomaps provide automatic widgets
 * 
 * @author florian
 *
 */
public enum AutomaticWidgetType implements WidgetContentType {
    
    /**
     * Widget displaying a geomap created in CMS
     */
    WIDGET_CMSGEOMAP("widgetGeoMap", "geoMap.xhtml");
    
    private final String label;
    private final String filename;
    
    private AutomaticWidgetType(String label, String filename) {
        this.label = label;
        this.filename = filename;
    }
    
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }
   

}
