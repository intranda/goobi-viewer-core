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
package io.goobi.viewer.controller.model;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

public class FeatureSetConfiguration {

    private final String type;
    private final String name;
    private final String marker;
    private final String query;
    private final String labelConfig;
    private final List<LabeledValue> filters;

    public FeatureSetConfiguration(String type, String name, String marker, String query, String labelConfig, List<LabeledValue> filters) {
        super();
        this.type = type;
        this.name = name;
        this.marker = marker;
        this.query = query;
        this.labelConfig = labelConfig;
        this.filters = filters;
    }

    public FeatureSetConfiguration(HierarchicalConfiguration<ImmutableNode> config) {
        this(
                config.getString("[@type]"),
                config.getString("name"),
                config.getString("marker", ""),
                config.getString("query"),
                config.getString("labelConfig", ""),
                parseFilters(config.configurationsAt("filters.filter")));
    }

    private static List<LabeledValue> parseFilters(List<HierarchicalConfiguration<ImmutableNode>> filterConfigs) {
        return filterConfigs.stream().map(c -> {
            String field = c.getString("field");
            String label = c.getString("field[@label]", "");
            String styleClass = c.getString("field[@styleClass]", "");
            return new LabeledValue(field, label, styleClass);
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

    public String getLabelConfig() {
        return labelConfig;
    }

}
