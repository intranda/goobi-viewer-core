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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrSearchIndex;

public class ComplexMetadataContainer {

    private static final String QUERY_FORMAT = "+DOCTYPE:METADATA +PI_TOPSTRUCT:%s";
    private static final String DEFAULT_SORTING = "desc";

    protected final Map<String, List<ComplexMetadata>> metadataMap;

    private String sorting = DEFAULT_SORTING;

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
            long limit) {
        return streamMetadata(field, sortField, sortLanguage, filterField, filterValue, limit).collect(Collectors.toList());
    }

    public long getNumEntries(String field, String filterField, String filterMatcher) {
        return getMetadata(field).stream()
                .filter(m -> StringUtils.isBlank(filterField) || Pattern.matches(filterMatcher, m.getFirstValue(filterField, null)))
                .count();
    }

    protected Stream<ComplexMetadata> streamMetadata(String field, String sortField, Locale sortLanguage, String filterField, String filterMatcher,
            long listSizeLimit) {
        Stream<ComplexMetadata> stream = getMetadata(field).stream()
                .filter(m -> StringUtils.isBlank(filterField) || Pattern.matches(filterMatcher, m.getFirstValue(filterField, null)));
        List<ComplexMetadata> list = stream.collect(Collectors.toList());
        stream = list.stream();
        if (StringUtils.isNotBlank(sortField)) {
            stream = stream.sorted((m1, m2) -> m1.getFirstValue(sortField, sortLanguage).compareTo(m2.getFirstValue(sortField, sortLanguage))
                    * (isDescendingOrder() ? -1 : 1));
        }
        return stream.limit(listSizeLimit);
    }

    public List<ComplexMetadata> getMetadata(String field) {
        return metadataMap.getOrDefault(field, Collections.emptyList());
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
        if(StringUtils.isNoneBlank(filterField, filterValue)) {
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


}
