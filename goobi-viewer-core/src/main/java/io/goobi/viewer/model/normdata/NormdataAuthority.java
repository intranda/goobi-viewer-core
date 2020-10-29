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
package io.goobi.viewer.model.normdata;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author florian
 *
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum NormdataAuthority {

    GND("http://lobid.org/gnd/context.jsonld", "https://d-nb.info/gnd/");
    
    private final String context;
    private final String baseUri;
    
    private NormdataAuthority(String context, String uri) {
        this.context = context;
        this.baseUri = uri;
    }

    @JsonProperty
    public String getBaseUri() {
        return baseUri;
    }

    @JsonProperty
    public String getContext() {
        return context;
    }
}
