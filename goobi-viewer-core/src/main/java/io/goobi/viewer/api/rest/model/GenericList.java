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
package io.goobi.viewer.api.rest.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * GenericList class.
 *
 * @param <T>
 */
public class GenericList<T> {

    private List<T> list;

    /**
     * Creates a new GenericList instance.
     */
    public GenericList() {
        this.list = Collections.emptyList();
    }

    /**
     * Creates a new GenericList instance.
     *
     * @param theList list of elements to wrap as an unmodifiable list
     */
    public GenericList(List<T> theList) {
        this.list = Collections.unmodifiableList(theList);
    }

    /**
     * Getter for the field <code>list</code>.
     *
     * @return a {@link java.util.List} object.
     */
    @JsonValue
    public List<T> getList() {
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return list.toString();
    }
}
