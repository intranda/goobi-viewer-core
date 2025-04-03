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
package io.goobi.viewer.api.rest.v1.localization;

import static io.goobi.viewer.api.rest.v1.ApiUrls.LOCALIZATION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.LOCALIZATION_TRANSLATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;

/**
 * @author florian
 *
 */
class TranslationResourceTest extends AbstractRestApiTest {
    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.localization.TranslationResource#getTranslations(java.lang.String)}.
     * 
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Test
    void testGetTranslations() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(LOCALIZATION, LOCALIZATION_TRANSLATIONS).build())
                .queryParam("keys", "cancel,ok")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            JSONObject translations = new JSONObject(entity);
            assertEquals("Cancel", translations.getJSONObject("cancel").getJSONArray("en").get(0));
            assertEquals("ok", translations.getJSONObject("ok").getJSONArray("en").get(0));

        }
    }

}
