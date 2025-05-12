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
 * <p>
 * CMSProperty class.
 * </p>
 *
 * @author Florian Alpers
 */
@Entity
@Table(name = "cms_properties")
public class CMSProperty implements Serializable {

    private static final long serialVersionUID = -2227539151219620322L;

    public static final String KEY_ACCESS_CONDITION = "ACCESSCONDITION";
    /** Constant <code>KEY_EMPTY="EMPTY"</code> */
    public static final String KEY_EMPTY = "EMPTY";
    /**
     * Boolean specifying that a page list should separate child pages by tag and prepend each group with a header
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
     * <p>
     * Constructor for CMSProperty.
     * </p>
     */
    public CMSProperty() {
    }

    /**
     * <p>
     * Constructor for CMSProperty.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     */
    public CMSProperty(String key) {
        this.key = key;
    }

    /**
     * Key+value constructor.
     * 
     * @param key
     * @param value
     */
    public CMSProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Creates a clone of the given property
     *
     * @param original a {@link io.goobi.viewer.model.cms.CMSProperty} object.
     */
    public CMSProperty(CMSProperty original) {
        if (original.id != null) {
            this.id = original.id;
        }
        this.key = original.getKey();
        this.value = original.getValue();

    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>key</code>.
     * </p>
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * <p>
     * Setter for the field <code>key</code>.
     * </p>
     *
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return key + " - " + value;
    }

    /**
     * <p>
     * getBooleanValue.
     * </p>
     *
     * @return a boolean.
     */
    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }

    /**
     * <p>
     * getLongValue.
     * </p>
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getLongValue() {
        try {
            return Long.parseLong(value);
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * <p>
     * getDoubleValue.
     * </p>
     *
     * @return a {@link java.lang.Double} object.
     */
    public Double getDoubleValue() {
        try {
            return Double.parseDouble(value);
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }
}
