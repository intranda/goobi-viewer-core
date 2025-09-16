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
package io.goobi.viewer.faces.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;

/**
 * Tools to create SelectItem collections to use in jsf components
 * 
 * @author florian
 *
 */
public final class SelectItemBuilder {

    private SelectItemBuilder() {
    }

    /**
     * Sort the given list of items into buckets for each starting letter
     * 
     * @param <T>
     * @param items List of items that should be sorted
     * @param sortValueSupplier Function mapping an item to the sting according to which it should be sorted
     * @return Map with starting letters of sort values as keys
     */
    public static <T> SortedMap<String, List<T>> getAsAlphabeticallySortedMap(List<T> items, Function<T, String> sortValueSupplier) {
        Map<String, List<T>> unsortedMap = items.stream()
                .collect(Collectors.toMap(
                        i -> Optional.ofNullable(sortValueSupplier.apply(i))
                                .map(s -> StringUtils.isEmpty(s) ? s : s.substring(0, 1))
                                .map(String::toUpperCase)
                                .orElse(i.toString()), //key is the first character of the sortValue 
                        Arrays::asList, //values are lists of <T>
                        (l1, l2) -> new ArrayList<>(CollectionUtils.union(l1, l2)))); //combine lists by building union
        return new TreeMap<>(unsortedMap);
    }

    /**
     * Sort the given list of items into buckets for each values returned by the sortValueSupplier
     * 
     * @param <T>
     * @param items List of items that should be sorted
     * @param sortValueSupplier Function mapping an item to the string according to which it should be sorted
     * @param keyGenerator Function that creates labels for the return values of the sortValueSupplier. These labels are used as the actual keys of
     *            the map
     * @return Map with starting letters of sort values as keys
     */
    public static <T> SortedMap<String, List<T>> getAsSortedMap(List<T> items, Function<T, List<String>> sortValueSupplier,
            Function<String, String> keyGenerator) {

        SortedMap<String, List<T>> map = new TreeMap<>();
        for (T item : items) {
            List<String> sortValues = sortValueSupplier.apply(item);
            for (String sortValue : sortValues) {
                String key = keyGenerator.apply(sortValue);
                List<T> bucketItems = map.computeIfAbsent(key, k -> new ArrayList<>());
                bucketItems.add(item);
            }
        }
        return map;
    }

    /**
     * Create a List of {@link SelectItem selectItems} from the given map, grouped into OptGroups for each map key
     * 
     * @param <T>
     * @param map Map of items to include in selectItems
     * @param valueSupplier
     * @param labelSupplier
     * @param descriptionSupplier
     * @param disabledPredicate
     * @return List<SelectItem>
     */
    public static <T> List<SelectItem> getAsGroupedSelectItems(Map<String, List<T>> map, Function<T, Object> valueSupplier,
            Function<T, String> labelSupplier, Function<T, String> descriptionSupplier, Predicate<T> disabledPredicate) {
        List<SelectItem> items = new ArrayList<>(map.size());
        for (Entry<String, List<T>> entry : map.entrySet()) {
            String groupName = entry.getKey();
            List<T> groupValues = entry.getValue();
            SelectItemGroup group = new SelectItemGroup(groupName);
            SelectItem[] groupItems = groupValues.stream()
                    .map(value -> createSelectItem(valueSupplier, labelSupplier, descriptionSupplier, value, disabledPredicate))
                    .collect(Collectors.toList())
                    .toArray(new SelectItem[0]);
            group.setSelectItems(groupItems);
            items.add(group);
        }

        return items;
    }

    /**
     * 
     * @param <T>
     * @param valueSupplier
     * @param labelSupplier
     * @param descriptionSupplier
     * @param value
     * @param disabledPredicate
     * @return SelectItem
     */
    public static <T> SelectItem createSelectItem(Function<T, Object> valueSupplier, Function<T, String> labelSupplier,
            Function<T, String> descriptionSupplier, T value, Predicate<T> disabledPredicate) {
        SelectItem item = new SelectItem(valueSupplier.apply(value), labelSupplier.apply(value), descriptionSupplier.apply(value));
        item.setDisabled(disabledPredicate.test(value));
        return item;
    }
}
