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
package io.goobi.viewer.api.rest.model;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.fasterxml.jackson.annotation.JsonValue;

import io.goobi.viewer.api.rest.model.ner.DocumentReference;
import io.goobi.viewer.api.rest.model.ner.ElementReference;
import io.goobi.viewer.api.rest.model.ner.NERTag;

/**
 * <p>
 * GenericList class.
 * </p>
 */
@XmlRootElement(name = "list")
@XmlSeeAlso({ NERTag.class, NERTag.Type.class, DocumentReference.class, ElementReference.class })
public class GenericList<T> {

    private List<T> list;

    /**
     * <p>
     * Constructor for GenericList.
     * </p>
     */
    public GenericList() {
        this.list = Collections.emptyList();
    }

    /**
     * <p>
     * Constructor for GenericList.
     * </p>
     *
     * @param theList a {@link java.util.List} object.
     */
    public GenericList(List<T> theList) {
        this.list = Collections.unmodifiableList(theList);
    }

    /**
     * <p>
     * Getter for the field <code>list</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    @XmlAnyElement
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
