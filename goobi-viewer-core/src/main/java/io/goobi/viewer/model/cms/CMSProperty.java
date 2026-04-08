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
package io.goobi.viewer.model.cms;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a generic key-value property that can be attached to CMS pages for custom metadata.
 *
 * @author Florian Alpers
 */
@Entity
@Table(name = "cms_properties")
public class CMSProperty implements Serializable {

    private static final long serialVersionUID = -2227539151219620322L;

    public static final String KEY_ACCESS_CONDITION = "ACCESSCONDITION";
    /** Constant <code>KEY_EMPTY="EMPTY"</code>. */
    public static final String KEY_EMPTY = "EMPTY";
    /**
     * Boolean specifying that a page list should separate child pages by tag and prepend each group with a header.
     */
    //    public static final String KEY_DISPLAY_CHILD_TAGS_AS_HEADERS = "DISPLAY_CHILD_TAGS_AS_HEADERS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "property_key", nullable = true)
    private String key = KEY_EMPTY;

    @Column(name = "property_value", nullable = true, columnDefinition = "LONGTEXT")
    private String value;

    //    /** Reference to the owning {@link PersistentEntity}. */
    //    @ManyToOne
    //    @JoinColumn(name = "property_owner_id")
    //    private CMSPage owner;

    /**
     * Creates a new CMSProperty instance.
     */
    public CMSProperty() {
    }

    /**
     * Creates a new CMSProperty instance.
     *
     * @param key property key name
     */
    public CMSProperty(String key) {
        this.key = key;
    }

    /**
     * Key+value constructor.
     * 
     * @param key property key name
     * @param value property value
     */
    public CMSProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Creates a clone of the given property.
     *
     * @param original property to copy key and value from
     */
    public CMSProperty(CMSProperty original) {
        if (original.id != null) {
            this.id = original.id;
        }
        this.key = original.getKey();
        this.value = original.getValue();

    }

    /**
     * Getter for the field <code>id</code>.
     *
     * @return the database primary key of this property
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for the field <code>id</code>.
     *
     * @param id the database ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for the field <code>key</code>.
     *
     * @return the property key used to look up this CMS property
     */
    public String getKey() {
        return key;
    }

    /**
     * Setter for the field <code>key</code>.
     *
     * @param key the property key used to look up this CMS property
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Getter for the field <code>value</code>.
     *
     * @return the string value stored for this CMS property
     */
    public String getValue() {
        return value;
    }

    /**
     * Setter for the field <code>value</code>.
     *
     * @param value the string value stored for this CMS property
     */
    public void setValue(String value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return key + " - " + value;
    }

    /**
     * getBooleanValue.
     *
     * @return true if the string value of this property parses to {@code true}, false otherwise
     */
    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }

    /**
     * getLongValue.
     *
     * @return the value of this property parsed as a Long, or null if the value is blank or not a valid number
     */
    public Long getLongValue() {
        try {
            return Long.parseLong(value);
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * getDoubleValue.
     *
     * @return the value of this property parsed as a Double, or null if the value is blank or not a valid number
     */
    public Double getDoubleValue() {
        try {
            return Double.parseDouble(value);
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }
}
