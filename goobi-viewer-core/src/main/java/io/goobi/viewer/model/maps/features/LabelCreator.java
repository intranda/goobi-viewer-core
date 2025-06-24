package io.goobi.viewer.model.maps.features;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataBuilder;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.solr.SolrConstants;

public class LabelCreator {

    private final Map<String, Metadata> metadataTemplates;

    public LabelCreator(Map<String, Metadata> metadataTemplates) {
        this.metadataTemplates = metadataTemplates;
    }

    public Metadata getMetadata(MetadataContainer doc, String template) {
        return this.metadataTemplates.getOrDefault(
                Optional.ofNullable(doc).map(mc -> mc.getFirstValue(SolrConstants.DOCSTRCT)).orElse(StringConstants.DEFAULT_NAME),
                this.metadataTemplates.getOrDefault(StringConstants.DEFAULT_NAME, new Metadata()));
    }

    public IMetadataValue getValue(MetadataContainer doc, String template) {
        return new MetadataBuilder(doc).build(this.getMetadata(doc, template));
    }

    public IMetadataValue getValue(Map<String, List<IMetadataValue>> metadata, String template) {
        Metadata mdConfig = this.metadataTemplates.get(template);
        if (mdConfig != null) {
            return new MetadataBuilder(metadata).build(mdConfig);
        } else {
            return new SimpleMetadataValue("");
        }
    }

}
