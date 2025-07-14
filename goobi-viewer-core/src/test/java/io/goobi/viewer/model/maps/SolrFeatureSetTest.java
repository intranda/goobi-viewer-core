package io.goobi.viewer.model.maps;

import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

class SolrFeatureSetTest extends AbstractDatabaseAndSolrEnabledTest {

    @Test
    void testGetFeatures() throws PresentationException, IndexUnreachableException {
        SolrFeatureSet featureSet = new SolrFeatureSet();
        featureSet.setSearchScope(SolrSearchScope.RECORDS);
        featureSet.setSolrQuery("PI_TOPSTRUCT:AC03111335");
        featureSet.setMarkerTitleField("cms__geomaps__popup_content__option__metadata");
        Collection<GeoMapFeature> features = featureSet.createFeatures();
        Assertions.assertFalse(features.isEmpty());
    }

}
