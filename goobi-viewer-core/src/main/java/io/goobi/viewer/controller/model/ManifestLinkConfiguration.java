package io.goobi.viewer.controller.model;

import io.goobi.viewer.model.metadata.MetadataParameter;

public class ManifestLinkConfiguration {

    private final String label;
    private final String format;
    private final MetadataParameter param;
    
    /**
     * @param label
     * @param format
     * @param param
     */
    public ManifestLinkConfiguration(String label, String format, MetadataParameter param) {
        super();
        this.label = label;
        this.format = format;
        this.param = param;
    }
    
    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }
    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }
    /**
     * @return the param
     */
    public MetadataParameter getParam() {
        return param;
    }

    
    
}
