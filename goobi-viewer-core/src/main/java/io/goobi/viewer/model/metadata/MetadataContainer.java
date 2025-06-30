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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * An object containing a number of translatable metadata values. The values are mapped to a key which is a string corresponding to the SOLR field
 * name the values are taken from Each field name/key is mapped to a list of {@link IMetadataValue} objects which may contain a single string or
 * translations in several languages Used in {@link GeoCoordinateConverter} to add translatable metadata entites to geomap features.
 * 
 * @author florian
 *
 */
public class MetadataContainer {

    private final String solrId;
    private IMetadataValue label;

    private final Map<String, List<IMetadataValue>> metadata;

    public MetadataContainer(Map<String, List<IMetadataValue>> metadata) {
        this("", new SimpleMetadataValue(""), metadata);
    }

    /**
     * 
     * @param solrId
     * @param label
     * @param metadata
     */
    public MetadataContainer(String solrId, IMetadataValue label, Map<String, List<IMetadataValue>> metadata) {
        this.solrId = solrId;
        this.label = label;
        this.metadata = metadata;
    }

    /**
     * 
     * @param solrId
     * @param label
     */
    public MetadataContainer(String solrId, IMetadataValue label) {
        this(solrId, label, new HashMap<>());
    }

    /**
     * 
     * @param solrId
     * @param label
     */
    public MetadataContainer(String solrId, String label) {
        this(solrId, new SimpleMetadataValue(label));
    }

    /**
     * Cloning constructor.
     * 
     * @param orig
     */
    public MetadataContainer(MetadataContainer orig) {
        this.solrId = orig.solrId;
        this.label = orig.label.copy();
        this.metadata = orig.metadata.entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().stream().map(IMetadataValue::copy).collect(Collectors.toList())));
    }

    public String getSolrId() {
        return solrId;
    }

    public IMetadataValue getLabel() {
        return label;
    }

    public void setLabel(IMetadataValue label) {
        this.label = label;
    }

    public String getLabel(Locale locale) {
        return label.getValueOrFallback(locale);
    }

    public Map<String, List<IMetadataValue>> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Get all metadata for the given key.
     * 
     * @param key the field name for which to get the metadata value
     * @return List<IMetadataValue>
     */
    public List<IMetadataValue> get(String key) {
        return this.metadata.getOrDefault(key, Collections.emptyList());
    }

    /**
     * get the first metadata value for the given key. If no such value exists, an empty {@link IMetadataValue} is returned.
     * 
     * @param key the field name for which to get the metadata value
     * @return {@link IMetadataValue}
     */
    public IMetadataValue getFirst(String key) {
        return this.metadata.getOrDefault(key, Collections.emptyList()).stream().findFirst().orElse(new SimpleMetadataValue(""));
    }

    /**
     * Get all values of the default language (or any value of no default langauge value exists) for the given field
     * 
     * @param key the field name for which to get the metadata value
     * @return List<String>
     */
    public List<String> getValues(String key) {
        return this.get(key).stream().map(value -> value.getValueOrFallback(null)).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
    }

    /**
     * Get the first found value in the default language for the given key.
     * 
     * @param key the field name for which to get the metadata value
     * @return First value for the given key; empty string if not found
     */
    public String getFirstValue(String key) {
        return this.get(key).stream().findFirst().flatMap(IMetadataValue::getValue).orElse("");
    }

    public Integer getFirstIntValue(String key) {
        return this.get(key)
                .stream()
                .findFirst()
                .flatMap(IMetadataValue::getValue)
                .filter(StringTools::isInteger)
                .map(Integer::parseInt)
                .orElse(null);
    }

    public Boolean getFirstBooleanValue(String key) {
        return this.get(key)
                .stream()
                .findFirst()
                .flatMap(IMetadataValue::getValue)
                .map(v -> SolrTools.getAsBoolean(v))
                .orElse(null);
    }

    /**
     * Get all values of the default language (or any value of no default langauge value exists) for the given field.
     * 
     * @param key the field name for which to get the metadata value
     * @param locale the language for which to find a value. If there is no translation in that langauge, use the default language and failing that
     *            any language entry
     * @return List of values for the given key and locale
     */
    public List<String> getValues(String key, Locale locale) {
        return this.get(key).stream().map(value -> value.getValueOrFallback(locale)).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
    }

    /**
     * Get the first found value for the given key and locale.
     * 
     * @param key the field name for which to get the metadata value
     * @param locale the language for which to find a value. If there is no translation in that langauge, use the default language and failing that
     *            any language entry
     * @return First value for the given key and locale; empty string if not found
     */
    public String getFirstValue(String key, Locale locale) {
        return this.get(key).stream().filter(value -> !value.isEmpty()).findFirst().map(value -> value.getValueOrFallback(locale)).orElse("");
    }

    /**
     * 
     * @param key
     * @param values
     */
    public void put(String key, List<IMetadataValue> values) {
        this.metadata.put(key, values);
    }

    /**
     * 
     * @param key
     * @param value
     */
    public void add(String key, IMetadataValue value) {
        this.metadata.computeIfAbsent(key, a -> new ArrayList<>()).add(value);
    }

    /**
     * 
     * @param key
     * @param value
     */
    public void add(String key, String value) {
        this.add(key, new SimpleMetadataValue(value));
    }

    /**
     * 
     * @param key
     * @param values
     */
    public void addAll(String key, Collection<IMetadataValue> values) {
        this.addAll(key, values, false);
    }

    /**
     * 
     * @param key
     * @param values
     * @param overwrite
     */
    public void addAll(String key, Collection<IMetadataValue> values, boolean overwrite) {
        values.forEach(v -> {
            if (overwrite) {
                this.put(key, List.of(v));
            } else {
                this.add(key, v);
            }
        });
    }

    /**
     * 
     * @param key
     */
    public void remove(String key) {
        this.metadata.remove(key);
    }

    /**
     * Returns a {@link MetadataContainer} which includes all metadata fields matching the given fieldNameFilter from the given {@link SolrDocument}
     * doc as well as the {@link SolrConstants#MD_VALUE values} of those child documents which {@link SolrConstants#LABEL label} matches the
     * fieldnameFilter.
     * 
     * @param doc The main DOCSTRUCT document
     * @param children METADATA type documents belonging to the main doc
     * @param mainDocFieldNameFilter A function which should return true for all metadata field names to be included in the main doc
     * @param childDocFieldNameFilter A function which should return true for all metadata field names to be included in child docs
     * @return a {@link MetadataContainer}
     */
    public static MetadataContainer createMetadataEntity(SolrDocument doc, List<SolrDocument> children, Predicate<String> mainDocFieldNameFilter,
            Predicate<String> childDocFieldNameFilter) {
        Map<String, List<IMetadataValue>> translatedMetadata = SolrTools.getTranslatedMetadata(doc, mainDocFieldNameFilter::test);
        MetadataContainer entity = new MetadataContainer(
                SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC), "");

        Set<String> childLabels = children.stream()
                .map(c -> SolrTools.getSingleFieldStringValue(c, SolrConstants.LABEL))
                .map(SolrTools::getBaseFieldName)
                .collect(Collectors.toSet());
        translatedMetadata.entrySet()
                .stream()
                .filter(e -> !childLabels.contains(SolrTools.getBaseFieldName(e.getKey())))
                .forEach(e -> entity.put(e.getKey(), e.getValue()));

        List<ComplexMetadata> childDocs = ComplexMetadata.getMetadataFromDocuments(children);
        List<Entry<String, List<IMetadataValue>>> allChildDocValues = childDocs.stream()
                .map(mdDoc -> mdDoc.getMetadata().entrySet())
                .flatMap(Set::stream)
                .filter(e -> childDocFieldNameFilter.test(e.getKey()))
                .toList();
        allChildDocValues.forEach(e -> entity.addAll(e.getKey(), e.getValue(), true));
        return entity;
    }

    public static MetadataContainer createMetadataEntity(StructElement doc) {
        Map<String, List<IMetadataValue>> translatedMetadata = doc.getMetadataFields()
                .keySet()
                .stream()
                .collect(
                        Collectors.toMap(field -> field.replaceAll("_UNTOKENIZED$", ""), field -> List.of(doc.getMultiLanguageMetadataValue(field)),
                                ListUtils::union));
        MetadataContainer entity =
                new MetadataContainer(doc.getMetadataValue(SolrConstants.IDDOC), doc.getMultiLanguageDisplayLabel(), translatedMetadata);
        return entity;
    }

    /**
     * Returns a {@link MetadataContainer} which includes all metadata fields matching the given fieldNameFilter from the given {@link SolrDocument}
     * doc.
     * 
     * @param doc The main DOCSTRUCT document
     * @param fieldNameFilter A function which should return true for all metadata field names to be included in the return value
     * @return a {@link MetadataContainer}
     */
    public static MetadataContainer createMetadataEntity(SolrDocument doc, Predicate<String> fieldNameFilter) {
        return createMetadataEntity(doc, Collections.emptyList(), fieldNameFilter, s -> true);
    }

    /**
     * Returns a {@link MetadataContainer} which includes all metadata fields matching the given fieldNameFilter from the given {@link SolrDocument}
     * doc.
     * 
     * @param doc The main DOCSTRUCT document
     * @return a {@link MetadataContainer}
     */
    public static MetadataContainer createMetadataEntity(SolrDocument doc) {
        return createMetadataEntity(doc, Collections.emptyList(), s -> true, s -> true);
    }

    public boolean containsField(String field) {
        return this.metadata.containsKey(field);
    }

    @Override
    public String toString() {
        return metadata.entrySet().stream().map(entry -> {
            return entry.getKey() + "\t-\t"
                    + entry.getValue().stream().map(v -> v.getValueOrFallback(Locale.ENGLISH)).collect(Collectors.joining(", "));
        }).sorted().collect(Collectors.joining("\n"));
    }

}
