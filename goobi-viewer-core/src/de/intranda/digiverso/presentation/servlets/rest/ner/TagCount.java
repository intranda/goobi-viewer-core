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
package de.intranda.digiverso.presentation.servlets.rest.ner;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.intranda.digiverso.presentation.servlets.rest.ner.NERTag.Type;

@XmlRootElement(name = "tag")
@XmlType(propOrder = { "value", "type", "counter", "references" })
@JsonPropertyOrder({ "value", "type", "counter", "references" })
@JsonInclude(Include.NON_NULL)
public class TagCount implements Comparable<TagCount> {

    private String value;
    private NERTag.Type type;
    private List<ElementReference> references = new ArrayList<>();

    /**
     * @param id
     * @param value2
     * @param type2
     * @param element
     */
    public TagCount(String value, Type type, ElementReference element) {
        this.value = value;
        this.type = type;
        if (!references.contains(element)) {
            references.add(element);
        }
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the counter
     */
    @JsonProperty("counter")
    @XmlElement(name = "counter")
    public Integer getCounter() {
        return getReferences().size();
    }

    /**
     * @return the type
     */
    public NERTag.Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(NERTag.Type type) {
        this.type = type;
    }

    /**
     * @return the references
     */
    public List<ElementReference> getReferences() {
        return references;
    }

    /**
     * @param references the references to set
     */
    public void setReferences(List<ElementReference> references) {
        this.references = references;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(TagCount o) {
        return getCounter().compareTo(o.getCounter());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return getValue().equals(((TagCount) obj).getValue()) && getType().equals(((TagCount) obj).getType());
        }
        return false;
    }

    /**
     * @param references2
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
