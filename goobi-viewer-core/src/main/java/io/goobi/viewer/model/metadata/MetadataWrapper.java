package io.goobi.viewer.model.metadata;

import io.goobi.viewer.model.viewer.StringPair;

public class MetadataWrapper {

    private Metadata metadata;
    private StringPair valuePair;

    /**
     * @return the metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     * @return this
     */
    public MetadataWrapper setMetadata(Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * @return the valuePair
     */
    public StringPair getValuePair() {
        return valuePair;
    }

    /**
     * @param valuePair the valuePair to set
     * @return this
     */
    public MetadataWrapper setValuePair(StringPair valuePair) {
        this.valuePair = valuePair;
        return this;
    }
}
