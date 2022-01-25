package io.goobi.viewer.model.cms.widgets.type;

public enum WidgetGenerationType {
    DEFAULT(""),
    AUTOMATIC("cms_widgets__type_automatic"),
    CUSTOM("cms_widgets__type_custom");
    
    private final String label;
    
    private WidgetGenerationType(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}