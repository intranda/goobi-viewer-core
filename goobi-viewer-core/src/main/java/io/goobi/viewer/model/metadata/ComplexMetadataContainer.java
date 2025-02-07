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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrSearchIndex;

public class ComplexMetadataContainer {

    private static final String QUERY_FORMAT = "+DOCTYPE:METADATA +PI_TOPSTRUCT:%s";

    protected final Map<String, ComplexMetadataList> metadataMap;

    public ComplexMetadataContainer(Map<String, ComplexMetadataList> metadataMap) {
        this.metadataMap = metadataMap;
    }

    public ComplexMetadataContainer(List<SolrDocument> metadataDocs) {
        this(metadataDocs, s -> true);
    }

    public ComplexMetadataContainer(List<SolrDocument> metadataDocs, Predicate<String> fieldNameFilter) {
        this.metadataMap = ComplexMetadata.getMetadataFromDocuments(metadataDocs)
                .stream()
                .filter(doc -> fieldNameFilter.test(doc.getField()))
                .collect(Collectors.toMap(ComplexMetadata::getField, ComplexMetadataList::new, ComplexMetadataList::union));
    }

    public List<ComplexMetadata> getMetadata(String field, Locale sortLanguage, String filterField, String filterValue,
            long limit) {
        return streamMetadata(field, sortLanguage, filterField, filterValue, limit).collect(Collectors.toList());
    }

    public long getNumEntries(String field, String filterField, String filterMatcher) {
        return getList(field).getNumEntries(filterField, filterMatcher);
    }

    public Map<String, List<ComplexMetadata>> getGroupedMetadata(String field, Locale sortLanguage,
            List<Map<String, List<String>>> categories, long limit) {
        return getList(field).getGroupedMetadata(sortLanguage, categories, limit);
    }

    public Map<String, List<ComplexMetadata>> getGroupedMetadata(String field, Locale sortLanguage,
            Map<String, List<String>> categories, long limit) {
        return getList(field).getGroupedMetadata(sortLanguage, categories, limit);
    }

    protected Stream<ComplexMetadata> streamMetadata(String field, Locale sortLanguage, String filterField, String filterMatcher,
            long listSizeLimit) {
        return getList(field).streamMetadata(sortLanguage, filterField, filterMatcher, listSizeLimit);
    }

    public ComplexMetadataList getList(String field) {
        return getList(field, "");
    }

    public ComplexMetadataList getList(String field, String defaultSortField) {
        ComplexMetadataList list = metadataMap.getOrDefault(field, new ComplexMetadataList(Collections.emptyList()));
        if (StringUtils.isNotBlank(defaultSortField) && StringUtils.isBlank(list.getSortField())) {
            list.setSortField(defaultSortField);
        }
        return list;
    }

    public List<ComplexMetadata> getMetadata(String field) {
        return metadataMap.getOrDefault(field, new ComplexMetadataList(Collections.emptyList())).getMetadata();
    }

    public Collection<String> getFieldNames() {
        return metadataMap.keySet();
    }

    public List<String> getMetadataValues(String metadataField, String valueField, Locale locale) {
        return getMetadataValues(metadataField, "", "", valueField, locale);
    }

    public String getMetadataValue(String metadataField, String valueField, Locale locale) {
        return getMetadataValues(metadataField, "", "", valueField, locale).stream().findAny().orElse("");
    }

    public List<String> getMetadataValues(String metadataField, String filterField, String filterValue, String valueField, Locale locale) {
        Stream<ComplexMetadata> stream = this.getMetadata(metadataField).stream();
        if (StringUtils.isNoneBlank(filterField, filterValue)) {
            stream = stream.filter(cm -> cm.getValues(filterField, locale).contains(filterValue));
        }
        return stream.map(cm -> cm.getValues(valueField, locale)).flatMap(List::stream).collect(Collectors.toList());
    }

    public String getFirstMetadataValue(String metadataField, String filterField, String filterValue, String valueField, Locale locale) {
        return getMetadataValues(metadataField, filterField, filterValue, valueField, locale).stream().findFirst().orElse("");
    }

    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex)
            throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi));
        return new ComplexMetadataContainer(metadataDocs);
    }

    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex,
            Predicate<String> fieldNameFilter)
            throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi));
        return new ComplexMetadataContainer(metadataDocs, fieldNameFilter);
    }

    public static ComplexMetadataContainer loadMetadataDocuments(String pi, SolrSearchIndex searchIndex, List<String> fieldList)
            throws PresentationException, IndexUnreachableException {
        SolrDocumentList metadataDocs = searchIndex.search(String.format(QUERY_FORMAT, pi), fieldList);
        return new ComplexMetadataContainer(metadataDocs);
    }

}
