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
     * @param element initial element reference where this tag occurs
     * @param value text value of the named entity tag
     * @param type NER category of the tag (person, location, etc.)
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

     */
    public String getValue() {
        return value;
    }

    /**
     * Setter for the field <code>value</code>.
     *

     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * getCounter.
     *

     */
    @JsonProperty("counter")
    public Integer getCounter() {
        return getReferences().size();
    }

    /**
     * Getter for the field <code>type</code>.
     *

     */
    public NERTag.Type getType() {
        return type;
    }

    /**
     * Setter for the field <code>type</code>.
     *
     * @param type NER category to assign to this tag
     */
    public void setType(NERTag.Type type) {
        this.type = type;
    }

    
    public String getIdentifier() {
        return identifier;
    }

    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Getter for the field <code>references</code>.
     *

     */
    public List<ElementReference> getReferences() {
        return references;
    }

    /**
     * Setter for the field <code>references</code>.
     *
     * @param references list of element references to assign
     */
    public void setReferences(List<ElementReference> references) {
        this.references = references;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(TagCount o) {
        return getCounter().compareTo(o.getCounter());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

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
     * @param references element references to add if not already present
     */
    public void addReferences(List<ElementReference> references) {
        for (ElementReference ref : references) {
            addReference(ref);
        }

    }

    /**
     * @param ref element reference to add
     */
    private void addReference(ElementReference ref) {
        if (!references.contains(ref)) {
            references.add(ref);
        }

    }
}
