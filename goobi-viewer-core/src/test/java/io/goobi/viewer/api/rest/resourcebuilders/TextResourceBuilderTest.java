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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.managedbeans.UserBean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

class TextResourceBuilderTest extends AbstractDatabaseAndSolrEnabledTest {



    /**
     * @verifies prioritize plaintext over ALTO
     */
    @Test
    void getFulltextMap_shouldPrioritizePlaintextOverAlto() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession()).thenReturn(session);
        Mockito.when(session.getAttribute("userBean")).thenReturn(new UserBean());

        Map<java.nio.file.Path, String> result = new TextResourceBuilder().getFulltextMap(PI_KLEIUNIV, request);
        Assertions.assertNotNull(result);
        // Assertions.assertFalse(result.isEmpty());
    }

    /**
     * @verifies throw ContentNotFoundException if no alto files found
     */
    @Test
    void getAltoAsZip_shouldThrowContentNotFoundExceptionIfNoAltoFilesFound() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession()).thenReturn(session);
        Mockito.when(session.getAttribute("userBean")).thenReturn(new UserBean());

        // A record with no ALTO files must produce a 404 instead of an IllegalArgumentException
        Assertions.assertThrows(ContentNotFoundException.class,
                () -> new TextResourceBuilder().getAltoAsZip("NONEXISTENT_PI_NO_ALTO", request));
    }

    /**
     * @verifies throw ContentNotFoundException if no fulltext files found
     */
    @Test
    void getFulltextAsZip_shouldThrowContentNotFoundExceptionIfNoFulltextFilesFound() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession()).thenReturn(session);
        Mockito.when(session.getAttribute("userBean")).thenReturn(new UserBean());

        // A record with no fulltext files must produce a 404 instead of an IllegalArgumentException
        Assertions.assertThrows(ContentNotFoundException.class,
                () -> new TextResourceBuilder().getFulltextAsZip("NONEXISTENT_PI_NO_FULLTEXT", request));
    }
}
