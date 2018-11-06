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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author Florian Alpers
 *
 */
public class BooleanSerializer extends JsonSerializer<Boolean> {

    private static final String TRUE = "Y";
    private static final String FALSE = "N";
    private static final String UNKNOWN = "U";

 
    /**
     * @param value
     * @param generator
     * @param provider
     * @throws IOException
     * @throws JsonProcessingException
     */
    @Override
    public void serialize(Boolean value, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
        if (Boolean.TRUE.equals(value)) {
            generator.writeString(TRUE);
        } else if (Boolean.FALSE.equals(value)) {
            generator.writeString(FALSE);
        } else {
            generator.writeString(UNKNOWN);
        }
    }

}
