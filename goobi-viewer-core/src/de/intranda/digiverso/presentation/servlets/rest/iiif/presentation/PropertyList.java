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
package de.intranda.digiverso.presentation.servlets.rest.iiif.presentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;

import de.intranda.digiverso.presentation.servlets.rest.services.Service;

/**
 * @author Florian Alpers
 *
 */
public class PropertyList<T> extends ArrayList<T>{

    /**
     * 
     */
    private static final long serialVersionUID = -4452705676844989719L;

    /**
     * @param collect
     */
    public PropertyList(List<T> collect) {
        super(collect);
    }

    /**
     * 
     */
    public PropertyList() {
        super();
    }

    @JsonValue
    public Object getValue() {
         if(this.size() == 1) {
            return this.get(0);
        } else {
            return new ArrayList<T>(this);
        }
    }

}
