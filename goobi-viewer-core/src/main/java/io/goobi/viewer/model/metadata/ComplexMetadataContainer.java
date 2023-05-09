package io.goobi.viewer.model.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrSearchIndex;

public class ComplexMetadataContainer {

    private static final String QUERY_FORMAT = "+DOCTYPE:METADATA +PI_TOPSTRUCT:%s";
    
    private final Map<String, List<ComplexMetadata>> metadataMap;
    
    public ComplexMetadataContainer(Map<String, List<ComplexMetadata>> metadataMap) {
        this.metadataMap = metadataMap;
    }
    
    public ComplexMetadataContainer(List<SolrDocument> metadataDocs) {
        this(metadataDocs, s -> true);
    }
    
    public ComplexMetadataContainer(List<SolrDocument> metadataDocs, Predicate<String> fieldNameFilter) {
        this.metadataMap = ComplexMetadata.getMetadataFromDocuments(metadataDocs).stream().filter(doc -> fieldNameFilter.test(doc.getField())).collect(Collectors.toMap(ComplexMetadata::getField, List::of, ListUtils::union));
    }
    
    public List<ComplexMetadata> getMetadata(String field, String sortField, Locale sortLanguage, boolean reverseOrder, String filterField, String filterValue, Integer limit) {
        List<ComplexMetadata> list = getMetadata(field).stream()
                .filter(m -> StringUtils.isBlank(filterField) || m.getFirstValue(filterField, sortLanguage).equalsIgnoreCase(filterValue))
                .sorted( (m1,m2) -> m1.getFirstValue(sortField, sortLanguage).compareTo(m2.getFirstValue(sortField, sortLanguage)) * (reverseOrder?-1:1))
                .limit(limit)
                .collect(Collectors.toList());
        return list;
    }
    
    public List<ComplexMetadata> getMetadata(String field) {
        return metadataMap.get(field);
    }
    
    public Collection<String> getFieldNames() {
        return metadataMap.keySet();
    }
    
    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi));
        return new ComplexMetadataContainer(metadataDocs);
    }
    
    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex, Predicate<String> fieldNameFilter) throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi));
        return new ComplexMetadataContainer(metadataDocs, fieldNameFilter);
    }

    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex, List<String> fieldList) throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi), fieldList);
        return new ComplexMetadataContainer(metadataDocs);
    }
}
