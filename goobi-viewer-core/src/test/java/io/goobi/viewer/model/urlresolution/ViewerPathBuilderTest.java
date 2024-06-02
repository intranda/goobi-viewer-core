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
package io.goobi.viewer.model.urlresolution;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;

/**
 * @author Florian Alpers
 *
 */
class ViewerPathBuilderTest {

    /**
     * <p>setUp.</p>
     *
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
    }

    /**
     * <p>tearDown.</p>
     *
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    void testStartsWith() {
        String url1 = "a";
        String url2 = "a/b";
        String url3 = "a/b/c";
        String url4 = "f";
        String url5 = "b/a";
        String url6 = "a/bc";

        URI uri = URI.create("a/b/cdef");

        Assertions.assertTrue(ViewerPathBuilder.startsWith(uri, url1));
        Assertions.assertTrue(ViewerPathBuilder.startsWith(uri, url2));
        Assertions.assertFalse(ViewerPathBuilder.startsWith(uri, url3));
        Assertions.assertFalse(ViewerPathBuilder.startsWith(uri, url4));
        Assertions.assertFalse(ViewerPathBuilder.startsWith(uri, url5));
        Assertions.assertFalse(ViewerPathBuilder.startsWith(uri, url6));

    }

    @Test
    void testCreateCorrectPathWIthLeadingExcamationMark() throws DAOException {
        String url = "http://localhost:8082/viewer/!fulltext/AC03343066/13/";
        String serverUrl = "http://localhost:8082/viewer";
        String applicationName ="/viewer";
        Optional<ViewerPath> path = ViewerPathBuilder.createPath(serverUrl, applicationName, url, "");
        assertTrue(path.isPresent());
        assertEquals("fulltext/AC03343066/13/", path.get().getCombinedPath().toString());
    }

}
