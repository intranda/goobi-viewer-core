package io.goobi.viewer.controller.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

public class FeatureSetConfiguration {
    
    private final String type;
    private final String name;
    private final String marker;
    private final String query;
    private final List<LabeledValue> filters;
    
    
    
    public FeatureSetConfiguration(String type, String name, String marker, String query, List<LabeledValue> filters) {
        super();
        this.type = type;
        this.name = name;
        this.marker = marker;
        this.query = query;
        this.filters = filters;
    }
    
    public FeatureSetConfiguration(HierarchicalConfiguration<ImmutableNode> config) {
        this(
                config.getString("[@type]"),
                config.getString("name"),
                config.getString("marker", ""),
                config.getString("query"),
                parseFilters(config.configurationsAt("filters.filter"))
                );
    }
    
    private static List<LabeledValue> parseFilters(List<HierarchicalConfiguration<ImmutableNode>> filterConfigs) {
        return filterConfigs.stream().map(c -> {
            String field = c.getString("field");
            String label = c.getString("field[@label]", "");
            return new LabeledValue(field, label);
        })
        .collect(Collectors.toList());
    }

    public String getType() {
        return type;
    }
    public String getName() {
        return name;
    }
    public String getMarker() {
        return marker;
    }
    public String getQuery() {
        return query;
    }
    public List<LabeledValue> getFilters() {
        return filters;
    }

}
