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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * NERTag class.
 */
@JsonInclude(Include.NON_NULL)
public class NERTag {

    public enum Type {
        PERSON("person"),
        LOCATION("location", "place"),
        CORPORATION("corporation", "corporate", "institution", "organization"),
        EVENT("event"),
        MISC("miscellaneous");

        private List<String> labels;

        private Type(String... labels) {
            this.labels = Arrays.asList(labels);
        }

        public static Type getByLabel(String label) {
            if (StringUtils.isBlank(label)) {
                return null;
            }
            for (Type type : EnumSet.allOf(Type.class)) {
                if (type.labels.contains(label.trim().toLowerCase())) {
                    return type;
                }
            }
            return null;
        }

        public boolean matches(String label) {
            return this.labels.contains(label.trim().toLowerCase());
        }
    }

    private final String id;
    private String value;
    private Type type;
    private ElementReference element;

    /**
     * Creates a new NERTag instance.
     */
    public NERTag() {
        this.id = null;
        this.value = null;
        this.type = null;
        this.element = null;
    }

    /**
     * Creates a new NERTag instance.
     *
     * @param id unique identifier of the named entity tag
     * @param value text value of the named entity tag
     * @param type NER category of the tag (person, location, etc.)
     * @param element element reference where the tag appears
     */
    public NERTag(String id, String value, Type type, ElementReference element) {
        this.id = id;
        this.value = value;
        this.type = type;
        this.element = element;
    }

    /**
     * Getter for the field <code>id</code>.
     *
     * @return the identifier of this NER tag
     */
    public String getId() {
        return id;
    }

    /**
     * Getter for the field <code>value</code>.
     *
     * @return the text value of this NER tag
     */
    public String getValue() {
        return value;
    }

    /**
     * Getter for the field <code>type</code>.
     *
     * @return the NER tag type (e.g. person, location, organization)
     */
    public Type getType() {
        return type;
    }

    /**
     * Getter for the field <code>element</code>.
     *
     * @return the element reference indicating where this tag appears on the page
     */
    public ElementReference getElement() {
        return element;
    }

    /**
     * Setter for the field <code>element</code>.
     *
     * @param element element reference where the tag appears
     */
    public void setElement(ElementReference element) {
        this.element = element;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(NERTag.class)) {
            NERTag other = (NERTag) obj;
            if (Objects.equals(this.getId(), other.getId()) && Objects.equals(this.getType(), other.getType())
                    && Objects.equals(this.getValue(), other.getValue())) {
                return true;
            }
        }
        return false;

    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getType() + ": \"" + getValue() + "\" (ID=" + getId() + ")";
    }

}
