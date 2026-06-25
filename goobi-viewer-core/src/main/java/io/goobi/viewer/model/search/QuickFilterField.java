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
package io.goobi.viewer.model.search;

import java.io.Serializable;
import java.util.Objects;

public class QuickFilterField implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        DATE_RANGE("dateRange"),
        FACET_DROPDOWN("facetDropdown");

        private final String configValue;

        Type(String configValue) {
            this.configValue = configValue;
        }

        public String getConfigValue() {
            return configValue;
        }

        public static Type fromString(String value) {
            for (Type t : values()) {
                if (t.configValue.equals(value)) {
                    return t;
                }
            }
            return null;
        }
    }

    private final Type type;
    private final String label;
    private final String solrField;

    public QuickFilterField(Type type, String label, String solrField) {
        this.type = type;
        this.label = label;
        this.solrField = solrField;
    }

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getSolrField() {
        return solrField;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, label, solrField);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        QuickFilterField other = (QuickFilterField) obj;
        return type == other.type && Objects.equals(label, other.label) && Objects.equals(solrField, other.solrField);
    }
}
