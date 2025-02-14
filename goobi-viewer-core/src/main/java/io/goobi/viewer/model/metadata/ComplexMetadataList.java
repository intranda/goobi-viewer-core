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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.sorting.ObjectComparatorBuilder;

public class ComplexMetadataList {

    public static final String SORT_DIRECTION_ASCENDING = "asc";
    public static final String SORT_DIRECTION_DESCENDING = "desc";

    private final List<ComplexMetadata> metadata;
    private String sortField = "";
    private String sortOrder = "";

    public ComplexMetadataList(ComplexMetadata md) {
        this(List.of(md));
    }

    public ComplexMetadataList(List<ComplexMetadata> metadata) {
        this(metadata, "");
    }

    public ComplexMetadataList(List<ComplexMetadata> metadata, String sortField) {
        this(metadata, sortField, "");
    }

    public ComplexMetadataList(List<ComplexMetadata> metadata, String sortField, String sortOrder) {
        super();
        this.metadata = metadata;
        this.sortField = sortField;
        this.sortOrder = sortOrder;
    }

    public static ComplexMetadataList union(ComplexMetadataList a, ComplexMetadataList b) {
        return new ComplexMetadataList(ListUtils.union(a.getMetadata(), b.getMetadata()));
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<ComplexMetadata> getMetadata() {
        return metadata;
    }

    public void setSorting(String sortField, String sortOrder) {
        setSortField(sortField);
        setSortOrder(sortOrder);
    }

    public boolean isDescendingOrder() {
        return sortOrder != null && ("desc".equalsIgnoreCase(sortOrder) || sortOrder.toLowerCase().endsWith("_desc"));
    }

    public List<ComplexMetadata> getMetadata(Locale sortLanguage, String filterField, String filterValue,
            long limit) {
        return streamMetadata(sortLanguage, filterField, filterValue, limit).collect(Collectors.toList());
    }

    public long getNumEntries(String filterField, String filterMatcher) {
        return getMetadata().stream()
                .filter(m -> StringUtils.isBlank(filterField) || Pattern.matches(filterMatcher, m.getFirstValue(filterField, null)))
                .count();
    }

    public Map<String, List<ComplexMetadata>> getGroupedMetadata(Locale sortLanguage,
            List<Map<String, List<String>>> categories, long limit) {
        Map<String, List<String>> categoryMap = new LinkedHashMap<>();
        for (Map<String, List<String>> map : categories) {
            categoryMap.putAll(map);
        }
        return getGroupedMetadata(sortLanguage, categoryMap, limit);
    }

    public Map<String, List<ComplexMetadata>> getGroupedMetadata(Locale sortLanguage,
            Map<String, List<String>> categories, long limit) {

        List<ComplexMetadata> allMetadata = getMetadata(sortLanguage, "", ".*", limit);

        Map<String, List<ComplexMetadata>> map = new LinkedHashMap<String, List<ComplexMetadata>>();
        map.put("", allMetadata);

        for (Entry<String, List<String>> entry : categories.entrySet()) {
            if (entry.getValue() != null && entry.getValue().size() == 2) {
                String category = entry.getKey();
                String filterField = entry.getValue().get(0);
                String filterMatcher = entry.getValue().get(0);
                List<ComplexMetadata> mds = getMetadata(sortLanguage, filterField, filterMatcher, limit);
                mds.forEach(md -> allMetadata.remove(md));
                map.put(category, mds);
            }
        }
        return map;
    }

    public Stream<ComplexMetadata> streamMetadata(Locale sortLanguage, String filterField, String filterMatcher,
            long listSizeLimit) {
        Stream<ComplexMetadata> stream = getMetadata().stream()
                .filter(m -> StringUtils.isBlank(filterField) || Pattern.matches(filterMatcher, m.getFirstValue(filterField, null)));
        List<ComplexMetadata> list = stream.collect(Collectors.toList());
        stream = list.stream();
        if (StringUtils.isNotBlank(sortField)) {
            stream = stream.sorted(ObjectComparatorBuilder.build(sortOrder, sortLanguage, m -> m.getFirstValue(sortField, sortLanguage)));
        }
        return stream.limit(listSizeLimit);
    }

    public List<ComplexMetadata> getMetadata(Locale sortLanguage, String filterField, String filterMatcher,
            boolean hideUnlinkedRecords, long limit, Function<ComplexMetadata, MetadataContainer> relatedEntityGetter) {

        boolean searchInRelatedRecords = filterField.startsWith(RelationshipMetadataContainer.FIELD_IN_RELATED_DOCUMENT_PREFIX);
        if (searchInRelatedRecords) {
            String relatedFilterField = filterField.replace(RelationshipMetadataContainer.FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            Stream<ComplexMetadata> stream = streamMetadata(sortLanguage, "", filterMatcher, Integer.MAX_VALUE)
                    .filter(m -> Optional.ofNullable(relatedEntityGetter.apply(m))
                            .map(rec -> Pattern.matches(filterMatcher, rec.getFirstValue(relatedFilterField)))
                            .orElse(false));

            if (StringUtils.isNotBlank(getSortField())) {

                stream = stream.sorted(ObjectComparatorBuilder.build(getSortOrder(), sortLanguage,
                        v -> Optional.ofNullable(relatedEntityGetter.apply(v))
                                .map(c -> c.getFirstValue(getSortField(), sortLanguage))
                                .filter(StringUtils::isNotBlank)
                                .orElse(v.getFirstValue(getSortField(), sortLanguage))));
            }
            if (hideUnlinkedRecords) {
                stream = stream.filter(m -> relatedEntityGetter.apply(m) != null);
            }

            return stream.limit(limit)
                    .collect(Collectors.toList());
        } else if (getSortField() != null && getSortField().startsWith(RelationshipMetadataContainer.FIELD_IN_RELATED_DOCUMENT_PREFIX)) {
            String relatedSortField = getSortField().replace(RelationshipMetadataContainer.FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            Stream<ComplexMetadata> stream = streamMetadata(sortLanguage, filterField, filterMatcher, Integer.MAX_VALUE);

            stream = stream.sorted(ObjectComparatorBuilder.build(getSortOrder(), sortLanguage,
                    v -> Optional.ofNullable(relatedEntityGetter.apply(v))
                            .map(c -> c.getFirstValue(relatedSortField, sortLanguage))
                            .filter(StringUtils::isNotBlank)
                            .orElse(v.getFirstValue(relatedSortField, sortLanguage))));
            if (hideUnlinkedRecords) {
                stream = stream.filter(m -> relatedEntityGetter.apply(m) != null);
            }
            return stream.limit(limit)
                    .collect(Collectors.toList());
        } else {
            Stream<ComplexMetadata> stream = streamMetadata(sortLanguage, filterField, filterMatcher, Integer.MAX_VALUE);
            if (hideUnlinkedRecords) {
                stream = stream.filter(m -> relatedEntityGetter.apply(m) != null);
            }
            return stream.limit(limit)
                    .collect(Collectors.toList());
        }
    }

    public Map<String, List<ComplexMetadata>> getGroupedMetadata(Locale sortLanguage,
            Map<String, List<String>> categories, boolean hideUnlinkedRecords, long limit,
            Function<ComplexMetadata, MetadataContainer> relatedEntityGetter) {

        List<ComplexMetadata> allMetadata = getMetadata(sortLanguage, "", ".*", hideUnlinkedRecords, Integer.MAX_VALUE, relatedEntityGetter);

        Map<String, List<ComplexMetadata>> map = new LinkedHashMap<String, List<ComplexMetadata>>();

        for (Entry<String, List<String>> entry : categories.entrySet()) {
            if (StringUtils.isNotBlank(entry.getKey()) && entry.getValue() != null && entry.getValue().size() == 2) {
                String category = entry.getKey();
                String filterField = entry.getValue().get(0);
                String filterMatcher = entry.getValue().get(1);
                List<ComplexMetadata> mds = getMetadata(sortLanguage, filterField, filterMatcher, hideUnlinkedRecords, limit, relatedEntityGetter);
                mds.forEach(md -> allMetadata.remove(md));
                map.put(category, mds);
            }
        }

        map.put("", allMetadata.stream().limit(limit).toList());

        return map;
    }

    public List<String> getMetadataValues(String valueField, Locale locale) {
        return getMetadataValues("", "", valueField, locale);
    }

    public String getMetadataValue(String valueField, Locale locale) {
        return getMetadataValues("", "", valueField, locale).stream().findAny().orElse("");
    }

    public List<String> getMetadataValues(String filterField, String filterValue, String valueField, Locale locale) {
        return getMetadataValues(filterField, filterValue, valueField, locale, x -> null);
    }

    public List<String> getMetadataValues(String filterField, String filterValue, String valueField, Locale locale,
            Function<ComplexMetadata, MetadataContainer> relatedEntityGetter) {
        Stream<ComplexMetadata> stream = getMetadata().stream();
        if (StringUtils.isNoneBlank(filterField, filterValue)) {
            stream = stream.filter(cm -> cm.getValues(filterField, locale).contains(filterValue));
        }
        if (valueField != null && valueField.startsWith(RelationshipMetadataContainer.FIELD_IN_RELATED_DOCUMENT_PREFIX)) {
            String relatedField = valueField.replace(RelationshipMetadataContainer.FIELD_IN_RELATED_DOCUMENT_PREFIX, "");
            return stream.map(relatedEntityGetter::apply)
                    .filter(Objects::nonNull)
                    .map(related -> related.getValues(relatedField, locale))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        return stream.map(cm -> cm.getValues(valueField, locale)).flatMap(List::stream).collect(Collectors.toList());
    }

}
