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

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrSearchIndex;

public class ComplexMetadataContainer {

    private static final String QUERY_FORMAT = "+DOCTYPE:METADATA +PI_TOPSTRUCT:%s";

    protected final Map<String, List<ComplexMetadata>> metadataMap;

    private String sorting = "desc";

    public void setSorting(String sorting) {
        this.sorting = sorting;
    }

    public String getSorting() {
        return sorting;
    }

    public boolean isDescendingOrder() {
        return "desc".equalsIgnoreCase(sorting);
    }

    public ComplexMetadataContainer(Map<String, List<ComplexMetadata>> metadataMap) {
        this.metadataMap = metadataMap;
    }

    public ComplexMetadataContainer(List<SolrDocument> metadataDocs) {
        this(metadataDocs, s -> true);
    }

    public ComplexMetadataContainer(List<SolrDocument> metadataDocs, Predicate<String> fieldNameFilter) {
        this.metadataMap = ComplexMetadata.getMetadataFromDocuments(metadataDocs)
                .stream()
                .filter(doc -> fieldNameFilter.test(doc.getField()))
                .collect(Collectors.toMap(ComplexMetadata::getField, List::of, ListUtils::union));
    }

    public List<ComplexMetadata> getMetadata(String field, String sortField, Locale sortLanguage, String filterField, String filterValue,
            Integer limit) {
        return getMetadata(field).stream()
                .filter(m -> StringUtils.isBlank(filterField) || m.getFirstValue(filterField, sortLanguage).equalsIgnoreCase(filterValue))
                .sorted((m1, m2) -> m1.getFirstValue(sortField, sortLanguage).compareTo(m2.getFirstValue(sortField, sortLanguage))
                        * (isDescendingOrder() ? -1 : 1))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<ComplexMetadata> getMetadata(String field) {
        return metadataMap.getOrDefault(field, Collections.emptyList());
    }

    public Collection<String> getFieldNames() {
        return metadataMap.keySet();
    }

    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex)
            throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi));
        return new ComplexMetadataContainer(metadataDocs);
    }

    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex, Predicate<String> fieldNameFilter)
            throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi));
        return new ComplexMetadataContainer(metadataDocs, fieldNameFilter);
    }

    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex, List<String> fieldList)
            throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi), fieldList);
        return new ComplexMetadataContainer(metadataDocs);
    }
    
    public List<String> getAllValues(String field, String filterField, Locale locale) {
        return this.getMetadata(field).stream().filter(md -> StringUtils.isBlank(filterField) ? true : filterField.equals(md.getFirstValue((Locale)null))).map(md -> md.getValues(locale)).flatMap(List::stream).distinct().collect(Collectors.toList());
    }
}
