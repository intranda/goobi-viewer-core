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

/**
 * SearchFilter class.
 */
public class SearchFilter implements Serializable {

    private static final long serialVersionUID = 7282765948716424825L;

    private String label;
    private String field;
    private boolean defaultFilter = false;

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SearchFilter other = (SearchFilter) obj;
        if (field == null) {
            if (other.field != null) {
                return false;
            }
        } else if (!field.equals(other.field)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        return true;
    }

    /**
     * Creates a new SearchFilter instance.
     *
     * @param label display label shown in the UI
     * @param field Solr field name this filter applies to
     * @param defaultFilter whether this filter is the default selection
     * @should set attributes correctly
     */
    public SearchFilter(String label, String field, boolean defaultFilter) {
        this.label = label;
        this.field = field;
        this.defaultFilter = defaultFilter;
    }

    /**
     * Getter for the field <code>label</code>.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Setter for the field <code>label</code>.
     *
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Getter for the field <code>field</code>.
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * Setter for the field <code>field</code>.
     *
     * @param field the field to set
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * @return the defaultFilter
     */
    public boolean isDefaultFilter() {
        return defaultFilter;
    }

    /**
     * @param defaultFilter the defaultFilter to set
     */
    public void setDefaultFilter(boolean defaultFilter) {
        this.defaultFilter = defaultFilter;
    }
}
