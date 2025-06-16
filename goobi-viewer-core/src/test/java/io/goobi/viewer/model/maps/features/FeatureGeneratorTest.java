package io.goobi.viewer.model.maps.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.maps.GeoMapFeature;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;

class FeatureGeneratorTest {

    private final String COORDINATE_FIELD = "MD_COORDINATES";

    @Test
    void test_findFeatureInDocument() {
        Metadata metadataConfig =
                new Metadata("Label", "{MD_TITLE} in {MD_LOCATION}", List.of(
                        new MetadataParameter(MetadataParameterType.FIELD, "MD_TITLE"),
                        new MetadataParameter(MetadataParameterType.FIELD, "MD_LOCATION")));

        LabelCreator titleGenerator = new LabelCreator(Map.of("_DEFAULT", metadataConfig));
        FeatureGenerator generator = new FeatureGenerator(List.of(COORDINATE_FIELD), titleGenerator, titleGenerator);

        SolrDocument mainDoc = new SolrDocument(
                Map.of("PI", "1234", "MD_TITLE", "Document title", "MD_LOCATION", "Göttingen", "MD_COORDINATES", "51.533172 9.935790"));

        MetadataDocument mdDoc = MetadataDocument.fromSolrDocs(mainDoc, Collections.emptyList(), Collections.emptyList());

        List<GeoMapFeature> features = new ArrayList<>(generator.getFeatures(mdDoc));
        Assertions.assertEquals(features.size(), 1);
        Assertions.assertEquals("Document title in Göttingen", features.get(0).getTitle().getValue().orElse(""));
        Assertions.assertEquals(features.get(0).getEntities().size(), 1);
        Assertions.assertEquals("Document title in Göttingen", features.get(0).getEntities().get(0).getLabel().getValue().orElse(""));
    }

    @Test
    void test_findFeaturesInMetadataDocs() {
        SolrDocument mainDoc = new SolrDocument(
                Map.of("PI", "1234", "MD_TITLE", "Document title"));

        SolrDocument locationDoc1 = new SolrDocument(
                Map.of("LABEL", "MD_LOCATION", "MD_VALUE", "Göttingen", "MD_COORDINATES", "51.533172 9.935790"));
        SolrDocument locationDoc2 = new SolrDocument(
                Map.of("LABEL", "MD_LOCATION", "MD_VALUE", "Kassel", "MD_COORDINATES", "51.311296 9.484915"));

        LabelCreator titleGenerator = new LabelCreator(Map.of("_DEFAULT", Metadata.forField("MD_TITLE")));
        LabelCreator entityTitleGenerator = new LabelCreator(Map.of("_DEFAULT", Metadata.forField("MD_VALUE")));
        FeatureGenerator generator = new FeatureGenerator(List.of(COORDINATE_FIELD), titleGenerator, entityTitleGenerator);

        MetadataDocument mdDoc = MetadataDocument.fromSolrDocs(mainDoc, Collections.emptyList(), List.of(locationDoc1, locationDoc2));

        List<GeoMapFeature> features = new ArrayList<>(generator.getFeatures(mdDoc));
        Assertions.assertEquals(2, features.size());
        Assertions.assertEquals("Document title", features.get(0).getTitle().getValue().orElse(""));
        Assertions.assertEquals("Document title", features.get(1).getTitle().getValue().orElse(""));

    }

}
