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
package io.goobi.viewer.model.annotation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.model.annotation.comments.Comment;

/**
 * @author florian
 *
 */
public class AnnotationConverterTest {

    private static final String campaignAnnotation = "{\n"
            + "      \"id\": \"https://viewer.goobi.io/api/v1/annotations/annotation_2/\",\n"
            + "      \"type\": \"Annotation\",\n"
            + "      \"motivation\": \"describing\",\n"
            + "      \"body\": {\n"
            + "        \"type\": \"TextualBody\",\n"
            + "        \"format\": \"text/plain\",\n"
            + "        \"value\": \"Älterer Herr mit einem Zylinder auf dem Kopf und einem Stock in der Hand steht auf einer Straße und guckt selbige hinunter.\"\n"
            + "      },\n"
            + "      \"target\": {\n"
            + "        \"type\": \"SpecificResource\",\n"
            + "        \"selector\": {\n"
            + "          \"value\": \"xywh=1769,3423,271,584\",\n"
            + "          \"type\": \"FragmentSelector\"\n"
            + "        },\n"
            + "        \"source\": \"https://viewer.goobi.io/api/v1/records/mnha16210/pages/1/canvas/\"\n"
            + "      }\n"
            + "    }";

    private static final String commentAnnotation = "{\n"
            + "        \"id\": \"https://viewer.goobi.io/api/v1/annotations/comment_19/\",\n"
            + "        \"type\": \"Annotation\",\n"
            + "        \"motivation\": \"commenting\",\n"
            + "        \"body\": {\n"
            + "          \"type\": \"TextualBody\",\n"
            + "          \"format\": \"text/plain\",\n"
            + "          \"value\": \"Ein paar Wasserspeier\"\n"
            + "        },\n"
            + "        \"target\": {\n"
            + "          \"id\": \"https://viewer.goobi.io/api/v1/records/PPN615391702/pages/88/canvas/\",\n"
            + "          \"type\": \"Canvas\"\n"
            + "        },\n"
            + "        \"creator\": \"https://viewer.goobi.io/api/v1/users/5\",\n"
            + "        \"created\": \"2021-08-03T12:03Z\"\n"
            + "      }";

    private final AnnotationConverter converter;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnnotationConverterTest() {
        converter = new AnnotationConverter(new ApiUrls("https://viewer.goobi.io/api/v1/"));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    @Test
    public void testConvertAnnotation() throws JsonMappingException, JsonProcessingException {
        WebAnnotation webAnno = mapper.readValue(campaignAnnotation, WebAnnotation.class);
        assertEquals("https://viewer.goobi.io/api/v1/annotations/annotation_2/", webAnno.getId().toString());
    }

}
