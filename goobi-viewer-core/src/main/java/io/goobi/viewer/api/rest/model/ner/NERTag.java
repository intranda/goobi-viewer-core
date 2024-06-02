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
 * <p>
 * NERTag class.
 * </p>
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
     * <p>
     * Constructor for NERTag.
     * </p>
     */
    public NERTag() {
        this.id = null;
        this.value = null;
        this.type = null;
        this.element = null;
    }

    /**
     * <p>
     * Constructor for NERTag.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param type a {@link io.goobi.viewer.api.rest.model.ner.NERTag.Type} object.
     * @param element a {@link io.goobi.viewer.api.rest.model.ner.ElementReference} object.
     */
    public NERTag(String id, String value, Type type, ElementReference element) {
        this.id = id;
        this.value = value;
        this.type = type;
        this.element = element;
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getValue() {
        return value;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.api.rest.model.ner.NERTag.Type} object.
     */
    public Type getType() {
        return type;
    }

    /**
     * <p>
     * Getter for the field <code>element</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.api.rest.model.ner.ElementReference} object.
     */
    public ElementReference getElement() {
        return element;
    }

    /**
     * <p>
     * Setter for the field <code>element</code>.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.api.rest.model.ner.ElementReference} object.
     */
    public void setElement(ElementReference element) {
        this.element = element;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getType() + ": \"" + getValue() + "\" (ID=" + getId() + ")";
    }

}
