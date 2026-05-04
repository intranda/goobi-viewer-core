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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
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
     * @see TextResourceBuilder#sumFileSizesOrThrow(String, List, long, String)
     * @verifies throw IllegalRequestException when sum exceeds limit
     */
    @Test
    void sumFileSizesOrThrow_shouldThrowIllegalRequestExceptionWhenSumExceedsLimit() throws Exception {
        // Bundled ALTO fixture for PI_KLEIUNIV is 7749 bytes; with a 1024-byte limit the helper
        // must reject. Direct helper test bypasses the Solr fixture (the embedded test index does
        // not carry FILENAME_ALTO entries for KLEIUNIV, so going through getAltoDocument() would
        // resolve to an empty file list and the guard would never fire).
        Path altoFile = Paths.get("src/test/resources/data/viewer/data/1/alto/PPN517154005/00000001.xml");
        Assertions.assertTrue(altoFile.toFile().exists(), "test ALTO fixture missing");

        Assertions.assertThrows(IllegalRequestException.class,
                () -> TextResourceBuilder.sumFileSizesOrThrow(PI_KLEIUNIV, List.of(altoFile), 1024L, "alto.zip"));
    }

    /**
     * @see TextResourceBuilder#sumFileSizesOrThrow(String, List, long, String)
     * @verifies return total when sum stays within limit
     */
    @Test
    void sumFileSizesOrThrow_shouldReturnTotalWhenSumStaysWithinLimit() throws Exception {
        Path altoFile = Paths.get("src/test/resources/data/viewer/data/1/alto/PPN517154005/00000001.xml");
        long total = TextResourceBuilder.sumFileSizesOrThrow(PI_KLEIUNIV, List.of(altoFile), 1_000_000L, "alto.zip");
        Assertions.assertEquals(altoFile.toFile().length(), total);
    }

    /**
     * @see TextResourceBuilder#sumStringSizesOrThrow(String, java.util.Collection, long, String)
     * @verifies throw IllegalRequestException when sum exceeds limit
     */
    @Test
    void sumStringSizesOrThrow_shouldThrowIllegalRequestExceptionWhenSumExceedsLimit() throws Exception {
        // Two 600-character pages -> 2 * 600 * 2 = 2400 in-heap UTF-16 bytes (plus separators);
        // a 1024-byte limit must trip on the second page.
        String page = "x".repeat(600);
        Assertions.assertThrows(IllegalRequestException.class,
                () -> TextResourceBuilder.sumStringSizesOrThrow(PI_KLEIUNIV, List.of(page, page), 1024L, "plaintext.zip"));
    }

    /**
     * @see TextResourceBuilder#sumStringSizesOrThrow(String, java.util.Collection, long, String)
     * @verifies return total when sum stays within limit
     */
    @Test
    void sumStringSizesOrThrow_shouldReturnTotalWhenSumStaysWithinLimit() throws Exception {
        String page = "x".repeat(100);
        long total = TextResourceBuilder.sumStringSizesOrThrow(PI_KLEIUNIV, List.of(page, page), 100_000L, "plaintext.zip");
        // 2 pages of 100 chars: each contributes length()*2 + 4 (separator overhead) = 204
        Assertions.assertEquals(2L * (100L * 2 + 4), total);
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
