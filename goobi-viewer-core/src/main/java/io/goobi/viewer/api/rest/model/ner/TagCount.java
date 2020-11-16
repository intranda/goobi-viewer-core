/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
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
package io.goobi.viewer.api.rest.model.ner;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.goobi.viewer.api.rest.model.ner.NERTag.Type;

/**
 * <p>
 * TagCount class.
 * </p>
 */
@JsonPropertyOrder({ "value", "type", "counter", "references" })
@JsonInclude(Include.NON_NULL)
public class TagCount implements Comparable<TagCount> {

    private String value;
    private NERTag.Type type;
    private List<ElementReference> references = new ArrayList<>();

    /**
     * <p>
     * Constructor for TagCount.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.servlets.rest.ner.ElementReference} object.
     * @param value a {@link java.lang.String} object.
     * @param type a {@link io.goobi.viewer.servlets.rest.ner.NERTag.Type} object.
     */
    public TagCount(String value, Type type, ElementReference element) {
        this.value = value;
        this.type = type;
        if (!references.contains(element)) {
            references.add(element);
        }
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * <p>
     * Setter for the field <code>value</code>.
     * </p>
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * <p>
     * getCounter.
     * </p>
     *
     * @return the counter
     */
    @JsonProperty("counter")
    public Integer getCounter() {
        return getReferences().size();
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public NERTag.Type getType() {
        return type;
    }

    /**
     * <p>
     * Setter for the field <code>type</code>.
     * </p>
     *
     * @param type the type to set
     */
    public void setType(NERTag.Type type) {
        this.type = type;
    }

    /**
     * <p>
     * Getter for the field <code>references</code>.
     * </p>
     *
     * @return the references
     */
    public List<ElementReference> getReferences() {
        return references;
    }

    /**
     * <p>
     * Setter for the field <code>references</code>.
     * </p>
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
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return getValue().equals(((TagCount) obj).getValue()) && getType().equals(((TagCount) obj).getType());
        }
        return false;
    }

    /**
     * <p>
     * addReferences.
     * </p>
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
