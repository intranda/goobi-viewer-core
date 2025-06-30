package io.goobi.viewer.model.maps;

import de.intranda.metadata.multilanguage.IMetadataValue;

public class GeoMapFeatureItem {

    private final IMetadataValue label;
    private final String link;

    public GeoMapFeatureItem(IMetadataValue label, String link) {
        super();
        this.label = label;
        this.link = link;
    }

    public IMetadataValue getLabel() {
        return label;
    }

    public String getLink() {
        return link;
    }

}
