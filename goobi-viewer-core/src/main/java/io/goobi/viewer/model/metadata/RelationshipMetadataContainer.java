package io.goobi.viewer.model.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

public class RelationshipMetadataContainer extends ComplexMetadataContainer {

    private static final String FIELD_IN_RELATED_DOCUMENT_PREFIX = "related.";
    private static final String RELATED_RECORD_QUERY_FORMAT = "+DOCTYPE:DOCSTRCT +MD_PROCESSID:(%s)";
    public static final String DOCUMENT_IDENTIFIER = "MD_PROCESSID";
    public static final String RELATIONSHIP_ID_REFERENCE = "MD_IDENTIFIER";    
    private static final List<String> RELATED_RECORD_METADATA_FIELDS = List.of(SolrConstants.PI, DOCUMENT_IDENTIFIER, SolrConstants.TITLE, SolrConstants.DOCSTRCT, "MD_*", "NORM_COORDS_GEOJSON");

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
//                .filter(md -> md.hasValue(RELATIONSHIP_ID_REFERENCE))
                .collect(Collectors.toList());
        String recordIdentifiers = relationshipMetadata.stream().map(md -> md.getFirstValue(RELATIONSHIP_ID_REFERENCE, null))
        .collect(Collectors.joining(" "));
        if(StringUtils.isBlank(recordIdentifiers)) {
            return new RelationshipMetadataContainer(container.metadataMap, Collections.emptyMap());
        } else {            
            String query = String.format(RELATED_RECORD_QUERY_FORMAT, recordIdentifiers);
            SolrDocumentList recordDocs = searchIndex.search(query, recordFields);
            Map<String, MetadataContainer> map = recordDocs.stream()
                    .collect(Collectors.toMap(doc -> SolrTools.getSingleFieldStringValue(doc, DOCUMENT_IDENTIFIER), MetadataContainer::createMetadataEntity));
            return new RelationshipMetadataContainer(container.metadataMap, map);
        }
    }

    public static ComplexMetadataContainer loadRelationshipMetadata(String pi, SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        return loadRelationshipMetadata(pi, searchIndex, RELATED_RECORD_METADATA_FIELDS);
    }
    
    @Override
    public long getNumEntries(String field, String filterField, String filterValue) {
        return getNumEntries(field, filterField, filterValue, false);
    }

    
    public long getNumEntries(String field, String filterField, String filterMatcher, boolean hideUninkedEntries) {
        boolean searchInRelatedRecords = filterField.startsWith(FIELD_IN_RELATED_DOCUMENT_PREFIX);
        if(searchInRelatedRecords) {
            String relatedFilterField = filterField.replace(FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            return super.streamMetadata(field, null, null, null, null, Integer.MAX_VALUE)
            .filter(m -> Optional.ofNullable(getRelatedRecord(m)).map(r ->  Pattern.matches(filterMatcher, r.getFirstValue(relatedFilterField))).orElse(false))   
            .count();
        } else {
            return getMetadata(field).stream()
                    .filter(m -> StringUtils.isBlank(filterField) || Pattern.matches(filterMatcher, m.getFirstValue(filterField, null)))
             .filter(m -> hideUninkedEntries ? getRelatedRecord(m) != null : true)
             .count();

        }
    }
    
    @Override
    public List<ComplexMetadata> getMetadata(String field, String sortField, Locale sortLanguage, String filterField, String filterMatcher, long limit) {
        return getMetadata(field, sortField, sortLanguage, filterField, filterMatcher, false, limit);
    }
        
        
    public List<ComplexMetadata> getMetadata(String field, String sortField, Locale sortLanguage, String filterField, String filterMatcher, boolean hideUnlinkedRecords, long limit) {

        boolean searchInRelatedRecords = filterField.startsWith(FIELD_IN_RELATED_DOCUMENT_PREFIX);
        if(searchInRelatedRecords) {
            String relatedFilterField = filterField.replace(FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            Stream<ComplexMetadata> stream = super.streamMetadata(field, sortField, sortLanguage, "", filterMatcher, Integer.MAX_VALUE)
            .filter(m -> Optional.ofNullable(getRelatedRecord(m)).map(record -> Pattern.matches(filterMatcher, record.getFirstValue(relatedFilterField))).orElse(false));   
            
            if (StringUtils.isNotBlank(sortField)) {
                stream = stream.sorted((m1, m2) -> {
                    String v1 = Optional.ofNullable(getRelatedRecord(m1)).map(c -> c.getFirstValue(sortField, sortLanguage)).orElse(m1.getFirstValue(sortField, sortLanguage));
                    String v2 = Optional.ofNullable(getRelatedRecord(m2)).map(c -> c.getFirstValue(sortField, sortLanguage)).orElse(m2.getFirstValue(sortField, sortLanguage));
                    return v1.compareTo(v2) * (isDescendingOrder() ? -1 : 1);
                });
            }
            if(hideUnlinkedRecords) {
                stream = stream.filter(m -> getRelatedRecord(m) != null);
            }
            
            return stream.limit(limit)
            .collect(Collectors.toList());
        } else if(sortField != null && sortField.startsWith(FIELD_IN_RELATED_DOCUMENT_PREFIX)) {
            String relatedSortField = sortField.replace(FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            Stream<ComplexMetadata> stream = super.streamMetadata(field, "", sortLanguage, filterField, filterMatcher, Integer.MAX_VALUE);
            stream = stream.sorted((m1, m2) -> {
                String v1 = Optional.ofNullable(getRelatedRecord(m1)).map(c -> c.getFirstValue(relatedSortField, sortLanguage)).orElse(m1.getFirstValue(relatedSortField, sortLanguage));
                String v2 = Optional.ofNullable(getRelatedRecord(m2)).map(c -> c.getFirstValue(relatedSortField, sortLanguage)).orElse(m2.getFirstValue(relatedSortField, sortLanguage));
                return v1.compareTo(v2) * (isDescendingOrder() ? -1 : 1);
            });
            if(hideUnlinkedRecords) {
                stream = stream.filter(m -> getRelatedRecord(m) != null);
            }
            return stream.limit(limit)
            .collect(Collectors.toList());
        } else {
            Stream<ComplexMetadata> stream = super.streamMetadata(field, sortField, sortLanguage, filterField, filterMatcher, Integer.MAX_VALUE);
            if(hideUnlinkedRecords) {
                stream = stream.filter(m -> getRelatedRecord(m) != null);
            }
            return stream.limit(limit)
            .collect(Collectors.toList());
        }
    }
    
    /**
     * Get all metadata documents with a certain {@link ComplexMetadata#field}
     * @param fieldName The metadata field for which to return metadata documents. If empty, all metadata documents will be returned
     * @return  A stream of Metadata documents
     */
    private Stream<ComplexMetadata> getAllMetadataByField(String fieldName) {
        return this.metadataMap.values().stream().flatMap(Collection::stream).filter(md -> StringUtils.isBlank(fieldName) ? true : fieldName.equals(md.getField()));
    }
    
    @Override
    public List<String> getMetadataValues(String metadataField, String filterField, String filterValue, String valueField, Locale locale) {
        Stream<ComplexMetadata> stream = this.getMetadata(metadataField).stream();
        if(StringUtils.isNoneBlank(filterField, filterValue)) {
            stream = stream.filter(cm -> cm.getValues(filterField, locale).contains(filterValue));
        }
        if(valueField != null &&  valueField.startsWith(FIELD_IN_RELATED_DOCUMENT_PREFIX)) {
            String relatedField = valueField.replace(FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            return stream.map(this::getRelatedRecord).filter(Objects::nonNull).map(related -> related.getValues(relatedField, locale))
            .flatMap(List::stream).collect(Collectors.toList());
        } else {            
            return stream.map(cm -> cm.getValues(valueField, locale)).flatMap(List::stream).collect(Collectors.toList());
        }
    }
}
