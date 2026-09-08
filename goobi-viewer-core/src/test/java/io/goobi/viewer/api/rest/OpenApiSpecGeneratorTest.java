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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.tags.Tag;

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

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies declare every operation tag globally for v1
     */
    @Test
    void buildOpenApi_shouldDeclareEveryOperationTagGloballyForV1() throws Exception {
        assertUndeclaredTags("v1");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies declare every operation tag globally for v2
     */
    @Test
    void buildOpenApi_shouldDeclareEveryOperationTagGloballyForV2() throws Exception {
        assertUndeclaredTags("v2");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies set a description for every declared tag
     */
    @Test
    void buildOpenApi_shouldSetADescriptionForEveryDeclaredTag() throws Exception {
        for (String version : List.of("v1", "v2")) {
            for (Tag tag : OpenApiSpecGenerator.buildOpenApi(version).getTags()) {
                assertTrue(StringUtils.isNotBlank(tag.getDescription()),
                        version + " tag '" + tag.getName() + "' must have a description");
            }
        }
    }

    /**
     * Fails with the offending tag names when an operation uses a tag that the spec does not declare
     * globally, the condition Spectral reports as "operation-tag-defined".
     *
     * @param version "v1" or "v2"
     */
    private static void assertUndeclaredTags(String version) throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi(version);
        Set<String> declared = openApi.getTags().stream().map(Tag::getName).collect(Collectors.toSet());
        Set<String> undeclared = openApi.getPaths()
                .values()
                .stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .map(Operation::getTags)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(tag -> !declared.contains(tag))
                .collect(Collectors.toCollection(TreeSet::new));
        assertTrue(undeclared.isEmpty(),
                version + " operations use tags that OpenApiResource.getTags() does not declare: " + undeclared);
    }
}
