package io.goobi.viewer.model.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;

/**
 * An object containing a number  of translatable metadata values.
 * The values are mapped to a key which is a string corresponding to the SOLR field name the values are taken from
 * Each field name/key is mapped to a list of {@link IMetadataValue} objects which may contain a single string or translations in several languages
 * @author florian
 *
 */
public class MetadataContainer {

    private final String solrId;
    private final IMetadataValue label;
    
    private final Map<String, List<IMetadataValue>> metadata;
    
    public MetadataContainer(String solrId, IMetadataValue label, Map<String, List<IMetadataValue>> metadata) {
        this.solrId = solrId;
        this.label = label;
        this.metadata = metadata;
    }

    public MetadataContainer(String solrId, IMetadataValue label) {
        this(solrId, label, new HashMap<>());
    }
    
    public MetadataContainer(String solrId, String label) {
        this(solrId, new SimpleMetadataValue(label));
    }
    
    public MetadataContainer(MetadataContainer orig) {
        this.solrId = orig.solrId;
        this.label = orig.label.copy();
        this.metadata = orig.metadata.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().stream().map(IMetadataValue::copy).collect(Collectors.toList())));
    }
    
    public String getSolrId() {
        return solrId;
    }
    
    public IMetadataValue getLabel() {
        return label;
    }
    
    public String getLabel(Locale locale) {
        return label.getValueOrFallback(locale);
    }
    
    public Map<String, List<IMetadataValue>> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
    
    public List<IMetadataValue> get(String key) {
        return this.metadata.getOrDefault(key, Collections.emptyList());
    }
    
    public void put(String key, List<IMetadataValue> values) {
        this.metadata.put(key, values);
    }
    
    public void add(String key, IMetadataValue value) {
        this.metadata.computeIfAbsent(key, a -> new ArrayList<>()).add(value);
    }
    
    public void add(String key, String value) {
        this.add(key, new SimpleMetadataValue(value));
    }
    
    public void addAll(String key, Collection<IMetadataValue> values) {
        values.forEach(v -> this.add(key, v));
    }

    public void remove(String key) {
        this.metadata.remove(key);
    }
    
    
}
