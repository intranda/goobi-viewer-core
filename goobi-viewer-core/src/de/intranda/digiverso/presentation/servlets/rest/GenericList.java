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
package de.intranda.digiverso.presentation.servlets.rest;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.fasterxml.jackson.annotation.JsonValue;

import de.intranda.digiverso.presentation.servlets.rest.ner.DocumentReference;
import de.intranda.digiverso.presentation.servlets.rest.ner.ElementReference;
import de.intranda.digiverso.presentation.servlets.rest.ner.NERTag;

@XmlRootElement(name = "list")
@XmlSeeAlso({ NERTag.class, NERTag.Type.class, DocumentReference.class, ElementReference.class })
public class GenericList<T> {

    private List<T> list;

    public GenericList() {
        this.list = Collections.emptyList();
    }

    public GenericList(List<T> theList) {
        this.list = Collections.unmodifiableList(theList);
    }

    @XmlAnyElement
    @JsonValue
    public List<T> getList() {
        return list;
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
