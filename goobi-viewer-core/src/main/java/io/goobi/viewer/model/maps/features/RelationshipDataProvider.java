package io.goobi.viewer.model.maps.features;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.ComplexMetadataContainer;
import io.goobi.viewer.model.metadata.RelationshipMetadataContainer;
import io.goobi.viewer.solr.SolrSearchIndex;

public class RelationshipDataProvider extends MetadataDataProvider {

    private static final Logger logger = LogManager.getLogger(RelationshipDataProvider.class);

    public RelationshipDataProvider(SolrSearchIndex searchIndex, List<String> requiredFields) {
        super(searchIndex, requiredFields);
    }

    protected MetadataDocument getMetadataDocument(QueryResponse response, SolrDocument topDocument) {
        MetadataDocument doc = super.getMetadataDocument(response, topDocument);
        ComplexMetadataContainer mdGroups = doc.getMetadataGroups();
        try {
            RelationshipMetadataContainer relations = RelationshipMetadataContainer.loadRelationships(mdGroups, getFieldList(), this.searchIndex);
            return new MetadataDocument(doc.getPi(), doc.getIddoc(), doc.getMainDocMetadata(), relations, doc.getChildDocuments());
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Error loading related documents for {}. Reason: {}", doc.getPi(), e.toString());
            return doc;
        }
    }

}
