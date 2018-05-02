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
package de.intranda.digiverso.presentation.servlets.rest.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Provides a definition for a json property
 * 
 * @author Florian Alpers
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class Definition {

    private final Context context;
    private final String id;
    private final String type;

    public Definition(Context context, String id) {
        this.context = context;
        this.id = id;
        this.type = null;
    }
    
    public Definition(Context context, String id, String type) {
        this.context = context;
        this.id = id;
        this.type = type;
    }
    
    /**
     * @return the type
     */
    @JsonProperty("@type")
    public String getType() {
        return type;
    }
    
    @JsonProperty("@id")
    public String getId() {
        return context.getName() + ":" + id;
    }

}
