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

/**
 * An entity holding an object which can be selected or unselected
 *
 * @author florian
 * @param <T>
 */
public class Selectable<T> implements Comparable<Selectable<T>>, Serializable {

    private static final long serialVersionUID = -7364321290125791403L;

    private final T value;
    private boolean selected;

    /**
     * <p>
     * Constructor for Selectable.
     * </p>
     *
     * @param value a T object.
     * @param selected a boolean.
     */
    public Selectable(T value, boolean selected) {
        this.value = value;
        this.selected = selected;
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return the value
     */
    public T getValue() {
        return value;
    }

    /**
     * <p>
     * isSelected.
     * </p>
     *
     * @return the selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * <p>
     * Setter for the field <code>selected</code>.
     * </p>
     *
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int compareTo(Selectable<T> other) {
        if (this.getValue() instanceof Selectable && other.getValue() instanceof Selectable) {
            return ((Comparable) this.getValue()).compareTo(other.getValue());
        }

        return 0;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return (((Selectable) obj).getValue()).equals(this.getValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.value == null ? 0 : this.value.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return (isSelected() ? "SELECTED" : "") + getValue().toString();
    }

}
