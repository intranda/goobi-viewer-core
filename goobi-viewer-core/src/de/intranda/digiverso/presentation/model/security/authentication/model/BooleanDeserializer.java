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
package de.intranda.digiverso.presentation.model.security.authentication.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * @author Florian Alpers
 *
 */
public class BooleanDeserializer extends JsonDeserializer<Boolean> {

    private static final String TRUE = "Y";
    private static final String FALSE = "N";


    /**
     * Returns {@link Boolean#TRUE} if and only if the next value read by the {@link JsonParser parser} is the String "Y" or "y". 
     * Returns {@link Boolean#FALSE} if and only if the next value read by the {@link JsonParser parser} is the String "N" or "n".
     * Otherwise return null. Usually this happens if the value is encoded with "U" for unknown 
     */
    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        String value = parser.readValueAs(String.class);
        if(TRUE.equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        } else if(FALSE.equalsIgnoreCase(value)){
            return Boolean.FALSE;
        } else {
            return null;
        }
    }
}
