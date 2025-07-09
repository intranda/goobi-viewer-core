package io.goobi.viewer.model.maps;

import java.util.ArrayList;
import java.util.List;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.maps.features.DocStructDataProvider;
import io.goobi.viewer.model.maps.features.IFeatureDataProvider;
import io.goobi.viewer.model.maps.features.MetadataDataProvider;
import io.goobi.viewer.model.maps.features.MetadataDocument;
import io.goobi.viewer.model.maps.features.RecordDataProvider;
import io.goobi.viewer.solr.SolrSearchIndex;

public class GlobalFeatureDataProvider implements IFeatureDataProvider {

    private final DocStructDataProvider docStructProvider;
    private final RecordDataProvider recordDataProvider;
    private final MetadataDataProvider metadataDataProvider;

    public GlobalFeatureDataProvider(SolrSearchIndex searchIndex, List<String> requiredFields) {
        this.docStructProvider = new DocStructDataProvider(searchIndex, requiredFields);
        this.recordDataProvider = new RecordDataProvider(searchIndex, requiredFields, false);
        this.metadataDataProvider = new MetadataDataProvider(searchIndex, requiredFields);
    }

    @Override
    public List<MetadataDocument> getResults(String query, int maxResults) throws PresentationException, IndexUnreachableException {
        List<MetadataDocument> recordResults = this.recordDataProvider.getResults(query, maxResults);
        List<MetadataDocument> docStructResults = this.docStructProvider.getResults(query, maxResults);
        List<MetadataDocument> metadataResults = this.metadataDataProvider.getResults(query, maxResults);
        List<MetadataDocument> results = new ArrayList<>();
        results.addAll(recordResults);
        results.addAll(docStructResults);
        results.addAll(metadataResults);
        return results;
    }

}
