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
package io.goobi.viewer.api.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiSpecGeneratorTest {

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies generate non empty paths for v1
     */
    @Test
    void buildOpenApi_shouldGenerateNonEmptyPathsForV1() throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi("v1");
        assertFalse(openApi.getPaths().isEmpty(), "v1 paths must not be empty");
        assertTrue(openApi.getPaths().keySet().stream().anyMatch(p -> p.contains("/cms/media/files")),
                "v1 must contain the cms media files path");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies generate non empty paths for v2
     */
    @Test
    void buildOpenApi_shouldGenerateNonEmptyPathsForV2() throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi("v2");
        assertFalse(openApi.getPaths().isEmpty(), "v2 paths must not be empty");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies set info for v1
     */
    @Test
    void buildOpenApi_shouldSetInfoForV1() throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi("v1");
        assertNotNull(openApi.getInfo(), "v1 info must not be null");
        assertNotNull(openApi.getInfo().getTitle(), "v1 info title must be set");
        assertNotNull(openApi.getInfo().getVersion(), "v1 info version must be set");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies set info for v2
     */
    @Test
    void buildOpenApi_shouldSetInfoForV2() throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi("v2");
        assertNotNull(openApi.getInfo(), "v2 info must not be null");
        assertNotNull(openApi.getInfo().getTitle(), "v2 info title must be set");
        assertNotNull(openApi.getInfo().getVersion(), "v2 info version must be set");
    }

    /**
     * @see OpenApiSpecGenerator#requireNonEmptyPaths(OpenAPI, String)
     * @verifies throw when paths are empty
     */
    @Test
    void requireNonEmptyPaths_shouldThrowWhenPathsAreEmpty() {
        assertThrows(IllegalStateException.class,
                () -> OpenApiSpecGenerator.requireNonEmptyPaths(new OpenAPI(), "test"));
    }
}
