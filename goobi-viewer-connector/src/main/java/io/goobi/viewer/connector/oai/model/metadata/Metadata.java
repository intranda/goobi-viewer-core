/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.connector.oai.model.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Metadata field configuration.
 */
public class Metadata implements Serializable {

    private static final long serialVersionUID = 4346850402844125222L;

    /** Label from messages.properties. */
    private final String label;
    /** Value from messages.properties (with placeholders) */
    private final String masterValue;
    private final int type;
    private final int number;
    private final List<MetadataValue> values = new ArrayList<>();
    private final List<MetadataParameter> params = new ArrayList<>();
    private final boolean group;
    private final boolean multivalued;

    /**
     * <p>
     * Constructor for Metadata.
     * </p>
     *
     * @param label a {@link java.lang.String} object.
     * @param masterValue a {@link java.lang.String} object.
     * @param type a int.
     * @param params a {@link java.util.List} object.
     * @param group a boolean.
     * @param number a int.
     * @param multivalued a boolean.
     */
    public Metadata(String label, String masterValue, int type, List<MetadataParameter> params, boolean group, int number, boolean multivalued) {
        if (StringUtils.isEmpty(label)) {
            throw new IllegalArgumentException("label may not be emtpy");
        }
        this.label = label;
        this.masterValue = masterValue;
        this.type = type;
        this.params.addAll(params);
        this.group = group;
        this.number = number;
        this.multivalued = multivalued;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((masterValue == null) ? 0 : masterValue.hashCode());
        result = prime * result + type;
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
        Metadata other = (Metadata) obj;
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        if (masterValue == null) {
            if (other.masterValue != null) {
                return false;
            }
        } else if (!masterValue.equals(other.masterValue)) {
            return false;
        }

        return type == other.type;
    }

    /**
     * <p>
     * isHasLabel.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasLabel() {
        return StringUtils.isNotBlank(label);
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * <p>
     * Getter for the field <code>masterValue</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMasterValue() {
        if (StringUtils.isEmpty(masterValue)) {
            return "{0}";
        }

        return masterValue;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * <p>
     * Getter for the field <code>values</code>.
     * </p>
     *
     * @return the values
     */
    public List<MetadataValue> getValues() {
        return values;
    }

    /**
     * <p>
     * Getter for the field <code>params</code>.
     * </p>
     *
     * @return the params
     */
    public List<MetadataParameter> getParams() {
        return params;
    }

    /**
     * <p>
     * hasParam.
     * </p>
     *
     * @param paramName a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasParam(String paramName) {
        for (MetadataParameter param : params) {
            if (param.getKey().equals(paramName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether any parameter values are set. 'empty' seems to be a reserved word in JSF, so use 'blank'.
     *
     * @return true if all paramValues are empty or blank; false otherwise.
     * @should return true if all paramValues are empty
     * @should return false if at least one paramValue is not empty
     */
    public boolean isBlank() {
        for (MetadataValue value : values) {
            if (value.getParamValues().isEmpty()) {
                return true;
            }
            for (List<String> paramValues : value.getParamValues()) {
                for (String paramValue : paramValues) {
                    if (StringUtils.isNotBlank(paramValue)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Getter for the field <code>number</code>.
     * </p>
     *
     * @return a int.
     */
    public int getNumber() {
        return number;
    }

    /**
     * <p>
     * isGroup.
     * </p>
     *
     * @return the group
     */
    public boolean isGroup() {
        return group;
    }

    /**
     * <p>
     * isMultivalued.
     * </p>
     *
     * @return the multivalued
     */
    public boolean isMultivalued() {
        return multivalued;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Label: " + label + " MasterValue: " + masterValue + " paramValues: " + values.get(0).getParamValues() + " ### ";
    }
}
