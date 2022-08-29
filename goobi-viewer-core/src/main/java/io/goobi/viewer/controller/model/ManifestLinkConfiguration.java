package io.goobi.viewer.controller.model;

import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;

public class ManifestLinkConfiguration {

    private final String label;
    private final String format;
    private final Metadata metadata;
    
    /**
     * @param label
     * @param format
     * @param param
     */
    public ManifestLinkConfiguration(String label, String format, Metadata metadata) {
        super();
        this.label = label;
        this.format = format;
        this.metadata = metadata;
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
    public Metadata getMetadata() {
        return metadata;
    }

    
    
}
