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
package io.goobi.viewer.api.rest.model.ner;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.goobi.viewer.api.rest.model.ner.NERTag.Type;

/**
 * TagCount class.
 */
@JsonPropertyOrder({ "value", "type", "counter", "references" })
@JsonInclude(Include.NON_NULL)
public class TagCount implements Comparable<TagCount> {

    private String value;
    private NERTag.Type type;
    private String identifier;
    private List<ElementReference> references = new ArrayList<>();

    /**
     * Creates a new TagCount instance.
     *
     * @param element a {@link io.goobi.viewer.api.rest.model.ner.ElementReference} object.
     * @param value a {@link java.lang.String} object.
     * @param type a {@link io.goobi.viewer.api.rest.model.ner.NERTag.Type} object.
     */
    public TagCount(String value, Type type, ElementReference element) {
        this.value = value;
        this.type = type;
        if (!references.contains(element)) {
            references.add(element);
        }
    }

    /**
     * Getter for the field <code>value</code>.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Setter for the field <code>value</code>.
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * getCounter.
     *
     * @return the counter
     */
    @JsonProperty("counter")
    public Integer getCounter() {
        return getReferences().size();
    }

    /**
     * Getter for the field <code>type</code>.
     *
     * @return the type
     */
    public NERTag.Type getType() {
        return type;
    }

    /**
     * Setter for the field <code>type</code>.
     *
     * @param type the type to set
     */
    public void setType(NERTag.Type type) {
        this.type = type;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Getter for the field <code>references</code>.
     *
     * @return the references
     */
    public List<ElementReference> getReferences() {
        return references;
    }

    /**
     * Setter for the field <code>references</code>.
     *
     * @param references the references to set
     */
    public void setReferences(List<ElementReference> references) {
        this.references = references;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public int compareTo(TagCount o) {
        return getCounter().compareTo(o.getCounter());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (getValue() != null && getType() != null && obj != null && obj.getClass().equals(this.getClass())) {
            TagCount other = (TagCount) obj;
            if (this.getIdentifier() == null && other.getIdentifier() != null) {
                return false;
            }
            if (this.getIdentifier() != null && other.getIdentifier() == null) {
                return false;
            }
            return getValue().equals(other.getValue()) && getType().equals(other.getType())
                    && ((getIdentifier() == null && other.getIdentifier() == null) || getIdentifier().equals(other.getIdentifier()));
        }
        return false;
    }

    /**
     * addReferences.
     *
     * @param references a {@link java.util.List} object.
     */
    public void addReferences(List<ElementReference> references) {
        for (ElementReference ref : references) {
            addReference(ref);
        }

    }

    /**
     * @param ref
     */
    private void addReference(ElementReference ref) {
        if (!references.contains(ref)) {
            references.add(ref);
        }

    }
}
