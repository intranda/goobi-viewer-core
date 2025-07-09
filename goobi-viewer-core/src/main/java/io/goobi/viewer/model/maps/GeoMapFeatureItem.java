package io.goobi.viewer.model.maps;

import java.util.List;
import java.util.Map;

import de.intranda.metadata.multilanguage.IMetadataValue;

public class GeoMapFeatureItem {

    private final IMetadataValue label;
    private final String link;
    private final Map<String, List<IMetadataValue>> additionalFields;

    public GeoMapFeatureItem(IMetadataValue label, String link, Map<String, List<IMetadataValue>> additionalFields) {
        super();
        this.label = label;
        this.link = link;
        this.additionalFields = additionalFields;
    }

    public IMetadataValue getLabel() {
        return label;
    }

    public String getLink() {
        return link;
    }

    public Map<String, List<IMetadataValue>> getAdditionalFields() {
        return additionalFields;
    }

}
