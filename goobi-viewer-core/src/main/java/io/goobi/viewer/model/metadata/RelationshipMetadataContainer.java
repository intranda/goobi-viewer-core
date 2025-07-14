/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
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

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

public class RelationshipMetadataContainer extends ComplexMetadataContainer {

    public static final String FIELD_IN_RELATED_DOCUMENT_PREFIX = "related.";
    private static final String RELATED_RECORD_QUERY_FORMAT = "+DOCTYPE:DOCSTRCT +MD_PROCESSID:(%s)";
    public static final String DOCUMENT_IDENTIFIER = "MD_PROCESSID";
    public static final String RELATIONSHIP_ID_REFERENCE = "MD_IDENTIFIER";
    private static final List<String> RELATED_RECORD_METADATA_FIELDS =
            List.of(SolrConstants.PI, DOCUMENT_IDENTIFIER, SolrConstants.TITLE, SolrConstants.DOCSTRCT, "MD_*", "NORM_COORDS_GEOJSON");

    private final Map<String, MetadataContainer> relatedDocumentMap;

    public RelationshipMetadataContainer(List<SolrDocument> metadataDocs, Predicate<String> fieldNameFilter,
            Map<String, MetadataContainer> relatedDocumentMap) {
        super(metadataDocs, fieldNameFilter);
        this.relatedDocumentMap = relatedDocumentMap;
    }

    public RelationshipMetadataContainer(List<SolrDocument> metadataDocs, Map<String, MetadataContainer> relatedDocumentMap) {
        super(metadataDocs);
        this.relatedDocumentMap = relatedDocumentMap;
    }

    public RelationshipMetadataContainer(Map<String, ComplexMetadataList> metadataMap, Map<String, MetadataContainer> relatedDocumentMap) {
        super(metadataMap);
        this.relatedDocumentMap = relatedDocumentMap;
    }

    public MetadataContainer getRelatedRecord(ComplexMetadata relationship) {
        String id = relationship.getFirstValue(RELATIONSHIP_ID_REFERENCE, null);
        if (StringUtils.isNotBlank(id)) {
            return this.relatedDocumentMap.get(id);
        }

        return null;
    }

    public static RelationshipMetadataContainer loadRelationshipMetadata(String pi, SolrSearchIndex searchIndex,
            List<String> recordFields)
            throws PresentationException, IndexUnreachableException {
        ComplexMetadataContainer container = ComplexMetadataContainer.loadMetadataDocuments(pi, searchIndex);
        return loadRelationships(container, recordFields, searchIndex);
    }

    public static RelationshipMetadataContainer loadRelationships(ComplexMetadataContainer container)
            throws PresentationException, IndexUnreachableException {
        return loadRelationships(container, DataManager.getInstance().getSearchIndex());
    }

    public static RelationshipMetadataContainer loadRelationships(ComplexMetadataContainer container, SolrSearchIndex searchIndex)
            throws PresentationException, IndexUnreachableException {
        return loadRelationships(container, RELATED_RECORD_METADATA_FIELDS, searchIndex);
    }

    public static RelationshipMetadataContainer loadRelationships(ComplexMetadataContainer container, List<String> recordFields,
            SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        List<ComplexMetadata> relationshipMetadata = container.metadataMap.values()
                .stream()
                .map(ComplexMetadataList::getMetadata)
                .flatMap(List::stream)
                //                .filter(md -> md.hasValue(RELATIONSHIP_ID_REFERENCE))
                .collect(Collectors.toList());
        String recordIdentifiers = relationshipMetadata.stream()
                .map(md -> md.getFirstValue(RELATIONSHIP_ID_REFERENCE, null))
                .collect(Collectors.joining(" "));
        if (StringUtils.isBlank(recordIdentifiers)) {
            return new RelationshipMetadataContainer(container.metadataMap, Collections.emptyMap());
        }

        String query = String.format(RELATED_RECORD_QUERY_FORMAT, recordIdentifiers);
        SolrDocumentList recordDocs = searchIndex.search(query, recordFields);
        Map<String, MetadataContainer> map = recordDocs.stream()
                .collect(Collectors.toMap(doc -> SolrTools.getSingleFieldStringValue(doc, DOCUMENT_IDENTIFIER),
                        MetadataContainer::createMetadataEntity));
        return new RelationshipMetadataContainer(container.metadataMap, map);
    }

    public static ComplexMetadataContainer loadRelationshipMetadata(String pi, SolrSearchIndex searchIndex)
            throws PresentationException, IndexUnreachableException {
        return loadRelationshipMetadata(pi, searchIndex, RELATED_RECORD_METADATA_FIELDS);
    }

    @Override
    public long getNumEntries(String field, String filterField, String filterValue) {
        return getNumEntries(field, filterField, filterValue, false);
    }

    public long getNumEntries(String field, String filterField, String filterMatcher, boolean hideUninkedEntries) {
        boolean searchInRelatedRecords = filterField.startsWith(FIELD_IN_RELATED_DOCUMENT_PREFIX);
        if (searchInRelatedRecords) {
            String relatedFilterField = filterField.replace(FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            return super.streamMetadata(field, null, null, null, Integer.MAX_VALUE)
                    .filter(m -> Optional.ofNullable(getRelatedRecord(m))
                            .map(r -> Pattern.matches(filterMatcher, r.getFirstValue(relatedFilterField)))
                            .orElse(false))
                    .count();
        }

        return getMetadata(field).stream()
                .filter(m -> StringUtils.isBlank(filterField) || Pattern.matches(filterMatcher, m.getFirstValue(filterField, null)))
                .filter(m -> !hideUninkedEntries || getRelatedRecord(m) != null)
                .count();
    }

    @Override
    public List<ComplexMetadata> getMetadata(String field, Locale sortLanguage, String filterField, String filterMatcher,
            long limit) {
        return getMetadata(field, sortLanguage, filterField, filterMatcher, false, limit);
    }

    @Override
    public Map<String, List<ComplexMetadata>> getGroupedMetadata(String field, Locale sortLanguage,
            List<Map<String, List<String>>> categories, long limit) {
        return getGroupedMetadata(field, sortLanguage, categories, false, limit);
    }

    @Override
    public Map<String, List<ComplexMetadata>> getGroupedMetadata(String field, Locale sortLanguage,
            Map<String, List<String>> categories, long limit) {
        return getGroupedMetadata(field, sortLanguage, categories, false, limit);
    }

    public Map<String, List<ComplexMetadata>> getGroupedMetadata(String field, Locale sortLanguage,
            List<Map<String, List<String>>> categories, boolean hideUnlinkedRecords, long limit) {
        Map<String, List<String>> categoryMap = new LinkedHashMap<>();
        for (Map<String, List<String>> map : categories) {
            categoryMap.putAll(map);
        }
        return getGroupedMetadata(field, sortLanguage, categoryMap, hideUnlinkedRecords, limit);
    }

    public Map<String, List<ComplexMetadata>> getGroupedMetadata(String field, Locale sortLanguage,
            Map<String, List<String>> categories, boolean hideUnlinkedRecords, long limit) {

        return getList(field).getGroupedMetadata(sortLanguage, categories, hideUnlinkedRecords, limit, this::getRelatedRecord);
    }

    public List<ComplexMetadata> getMetadata(String field, Locale sortLanguage, String filterField, String filterMatcher,
            boolean hideUnlinkedRecords, long limit) {
        return getList(field).getMetadata(sortLanguage, filterField, filterMatcher, hideUnlinkedRecords, limit, this::getRelatedRecord);
    }

    @Override
    public List<String> getMetadataValues(String metadataField, String filterField, String filterValue, String valueField, Locale locale) {
        Stream<ComplexMetadata> stream = this.getMetadata(metadataField).stream();
        if (StringUtils.isNoneBlank(filterField, filterValue)) {
            stream = stream.filter(cm -> cm.getValues(filterField, locale).contains(filterValue));
        }
        if (valueField != null && valueField.startsWith(FIELD_IN_RELATED_DOCUMENT_PREFIX)) {
            String relatedField = valueField.replace(FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            return stream.map(this::getRelatedRecord)
                    .filter(Objects::nonNull)
                    .map(related -> related.getValues(relatedField, locale))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        return stream.map(cm -> cm.getValues(valueField, locale)).flatMap(List::stream).collect(Collectors.toList());
    }
}
