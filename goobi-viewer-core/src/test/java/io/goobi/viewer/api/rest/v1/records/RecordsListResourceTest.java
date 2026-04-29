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
package io.goobi.viewer.api.rest.v1.records;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import jakarta.servlet.http.HttpServletRequest;

class RecordsListResourceTest extends AbstractTest {

    /**
     * @see RecordsListResource#createQuery(String, String, String, String)
     * @verifies escape solr special chars in subtheme value
     */
    @Test
    void createQuery_shouldEscapeSolrSpecialCharsInSubthemeValue() throws Exception {
        RecordsListResource resource = new RecordsListResource();
        // Inject a mock servlet request via reflection so getAllSuffixes() does not NPE.
        Field requestField = RecordsListResource.class.getDeclaredField("servletRequest");
        requestField.setAccessible(true);
        requestField.set(resource, Mockito.mock(HttpServletRequest.class));

        String discriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
        // Solr query injection attempt: parentheses, colon, asterisk, whitespace must all be escaped.
        String maliciousSubtheme = "*) OR (*:";

        String finalQuery = resource.createQuery(null, null, null, maliciousSubtheme);

        // Raw injection payload must not appear in the final query
        assertFalse(finalQuery.contains(" +" + discriminatorField + ":" + maliciousSubtheme),
                "Raw subtheme value must not be concatenated into the Solr query: " + finalQuery);
        // ClientUtils.escapeQueryChars escapes with a leading backslash, also for whitespace.
        assertTrue(finalQuery.contains(" +" + discriminatorField + ":\\*\\)\\ OR\\ \\(\\*\\:"),
                "Escaped subtheme value expected in the Solr query: " + finalQuery);
    }
}
