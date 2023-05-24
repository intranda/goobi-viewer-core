package io.goobi.viewer.model.metadata;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

public class RelationshipMetadataContainer extends ComplexMetadataContainer {

    private static final String RELATED_RECORD_QUERY_FORMAT = "+DOCTYPE:DOCSTRCT +MD_PROCESSID:(%s)";
    public static final String DOCUMENT_IDENTIFIER = "MD_PROCESSID";
    public static final String RELATIONSHIP_ID_REFERENCE = "MD_IDENTIFIER";    
    private static final List<String> RELATED_RECORD_METADATA_FIELDS = List.of(SolrConstants.PI, DOCUMENT_IDENTIFIER, "MD_GENRE", "MD_VALUE", "NORM_COORDS_GEOJSON");

    private final Map<String, MetadataContainer> relatedDocumentMap;    
    
    public RelationshipMetadataContainer(List<SolrDocument> metadataDocs, Predicate<String> fieldNameFilter, Map<String, MetadataContainer> relatedDocumentMap) {
        super(metadataDocs, fieldNameFilter);
        this.relatedDocumentMap = relatedDocumentMap;
    }

    public RelationshipMetadataContainer(List<SolrDocument> metadataDocs, Map<String, MetadataContainer> relatedDocumentMap) {
        super(metadataDocs);
        this.relatedDocumentMap = relatedDocumentMap;
    }

    public RelationshipMetadataContainer(Map<String, List<ComplexMetadata>> metadataMap, Map<String, MetadataContainer> relatedDocumentMap) {
        super(metadataMap);
        this.relatedDocumentMap = relatedDocumentMap;
    }
    
    public MetadataContainer getRelatedRecord(ComplexMetadata relationship) {
        String id = relationship.getFirstValue(RELATIONSHIP_ID_REFERENCE, null);
        if(StringUtils.isNotBlank(id)) {
            return this.relatedDocumentMap.get(id);
        } else {
            return null;
        }
    }
 
    public static RelationshipMetadataContainer loadRelationshipMetadata(String pi, SolrSearchIndex searchIndex, List<String> recordFields) throws PresentationException, IndexUnreachableException {
        ComplexMetadataContainer container = ComplexMetadataContainer.loadMetadataDocuments(pi, searchIndex);
        List<ComplexMetadata> relationshipMetadata = container.metadataMap.values().stream().flatMap(List::stream)
                .filter(md -> md.hasValue(RELATIONSHIP_ID_REFERENCE))
                .collect(Collectors.toList());
        String recordIdentifiers = relationshipMetadata.stream().map(md -> md.getFirstValue(RELATIONSHIP_ID_REFERENCE, null))
        .collect(Collectors.joining(" "));
        String query = String.format(RELATED_RECORD_QUERY_FORMAT, recordIdentifiers);
        SolrDocumentList recordDocs = searchIndex.search(query, recordFields);
        Map<String, MetadataContainer> map = recordDocs.stream()
                .collect(Collectors.toMap(doc -> SolrTools.getSingleFieldStringValue(doc, DOCUMENT_IDENTIFIER), MetadataContainer::createMetadataEntity));
        return new RelationshipMetadataContainer(container.metadataMap, map);
    }

    public static ComplexMetadataContainer loadRelationshipMetadata(String pi, SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        return loadRelationshipMetadata(pi, searchIndex, RELATED_RECORD_METADATA_FIELDS);
    }
}