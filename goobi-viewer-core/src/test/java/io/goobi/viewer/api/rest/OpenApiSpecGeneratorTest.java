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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
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
     * Operations whose success response legitimately has no body: the endpoint answers with a bare
     * status code, so there is no schema to declare.
     */
    private static final Set<String> SUCCESS_WITHOUT_BODY = Set.of(
            "v1 POST /users/{userId}/avatar");

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies declare a body schema for every success response for v1
     */
    @Test
    void buildOpenApi_shouldDeclareABodySchemaForEverySuccessResponseForV1() throws Exception {
        assertSuccessResponsesDeclareBodySchema("v1");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies declare a body schema for every success response for v2
     */
    @Test
    void buildOpenApi_shouldDeclareABodySchemaForEverySuccessResponseForV2() throws Exception {
        assertSuccessResponsesDeclareBodySchema("v2");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies describe every parameter for v1
     */
    @Test
    void buildOpenApi_shouldDescribeEveryParameterForV1() throws Exception {
        assertParametersDescribed("v1");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies describe every parameter for v2
     */
    @Test
    void buildOpenApi_shouldDescribeEveryParameterForV2() throws Exception {
        assertParametersDescribed("v2");
    }

    /**
     * Fails with the offending parameters when an operation parameter has no description. Parameters are
     * the only place Scalar can explain query and path values, and Spectral's default ruleset does not check them.
     *
     * @param version "v1" or "v2"
     */
    private static void assertParametersDescribed(String version) throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi(version);
        List<String> offenders = new ArrayList<>();
        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
            List<Parameter> parameters = new ArrayList<>();
            if (pathItem.getParameters() != null) {
                parameters.addAll(pathItem.getParameters());
            }
            if (operation.getParameters() != null) {
                parameters.addAll(operation.getParameters());
            }
            for (Parameter parameter : parameters) {
                if (StringUtils.isBlank(parameter.getDescription())) {
                    offenders.add(version + " " + method.name() + " " + path + " ?" + parameter.getName());
                }
            }
        }));
        assertTrue(offenders.isEmpty(), version + " parameters without description: " + offenders);
    }

    /**
     * Component schemas that no {@code $ref} in the spec points to. Names are removed as the corresponding schemas
     * become referenced; what remains are interfaces Swagger registers from return types although the operation
     * declares a concrete schema, plus a type whose serializer diverges from its Java shape.
     */
    private static final Set<String> KNOWN_UNREFERENCED_SCHEMAS = Set.of(
            "IAnnotationCollection",
            "IPresentationModelElement",
            "Translation");

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies reference every component schema for v1
     */
    @Test
    void buildOpenApi_shouldReferenceEveryComponentSchemaForV1() throws Exception {
        assertComponentsReferenced("v1");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies reference every component schema for v2
     */
    @Test
    void buildOpenApi_shouldReferenceEveryComponentSchemaForV2() throws Exception {
        assertComponentsReferenced("v2");
    }

    /**
     * Fails with the orphaned schema names when a component schema is never referenced by a {@code $ref} anywhere in the
     * spec — the condition Spectral reports as "oas3-unused-component". Serializing the model and scanning the JSON for
     * {@code $ref} properties mirrors what Spectral does (discriminator mappings are plain strings and count for neither)
     * and catches references from parameters, bodies, responses and nested schemas alike.
     *
     * @param version "v1" or "v2"
     */
    private static void assertComponentsReferenced(String version) throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi(version);
        String json = Json.mapper().writeValueAsString(openApi);
        Set<String> referenced = new TreeSet<>();
        Matcher matcher = Pattern.compile("\"\\$ref\"\\s*:\\s*\"#/components/schemas/([A-Za-z0-9_.]+)\"").matcher(json);
        while (matcher.find()) {
            referenced.add(matcher.group(1));
        }
        Set<String> orphans = openApi.getComponents()
                .getSchemas()
                .keySet()
                .stream()
                .filter(name -> !referenced.contains(name))
                .filter(name -> !KNOWN_UNREFERENCED_SCHEMAS.contains(name))
                .collect(Collectors.toCollection(TreeSet::new));
        assertTrue(orphans.isEmpty(), version + " component schemas nothing references: " + orphans);
    }

    /**
     * Minimum length of an operation description. Anything shorter is a stub that repeats the summary rather than
     * telling an API consumer something new.
     */
    private static final int MIN_DESCRIPTION_LENGTH = 40;

    /**
     * Operations whose summary already says everything an API consumer needs, so that any description would only
     * rephrase it. Each entry needs a comment naming the reason; the set is expected to stay empty.
     */
    private static final Set<String> SUMMARY_IS_SUFFICIENT = Set.of();

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies describe every operation for v1
     */
    @Test
    void buildOpenApi_shouldDescribeEveryOperationForV1() throws Exception {
        assertOperationsDescribed("v1");
    }

    /**
     * @see OpenApiSpecGenerator#buildOpenApi(String)
     * @verifies describe every operation for v2
     */
    @Test
    void buildOpenApi_shouldDescribeEveryOperationForV2() throws Exception {
        assertOperationsDescribed("v2");
    }

    /**
     * Fails with the offending operations when one has no description, when the description merely repeats the summary,
     * or when it is too short to say anything beyond it — the condition Spectral reports as "operation-description",
     * tightened so that a stub cannot satisfy it.
     *
     * @param version "v1" or "v2"
     */
    private static void assertOperationsDescribed(String version) throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi(version);
        List<String> offenders = new ArrayList<>();
        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
            String key = version + " " + method.name() + " " + path;
            if (SUMMARY_IS_SUFFICIENT.contains(key)) {
                return;
            }
            String description = operation.getDescription();
            if (StringUtils.isBlank(description) || description.trim().length() < MIN_DESCRIPTION_LENGTH) {
                offenders.add(key + " (missing or too short)");
            } else if (StringUtils.equalsIgnoreCase(description.trim(), StringUtils.trimToEmpty(operation.getSummary()))) {
                offenders.add(key + " (description repeats the summary)");
            }
        }));
        assertTrue(offenders.isEmpty(), version + " operations without a usable description: " + offenders);
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

    /**
     * Fails with the offending operations when one has no 2xx/3xx response at all, or when one of its
     * 2xx responses has no content schema. 3xx responses are redirects and carry no body.
     *
     * @param version "v1" or "v2"
     */
    private static void assertSuccessResponsesDeclareBodySchema(String version) throws Exception {
        OpenAPI openApi = OpenApiSpecGenerator.buildOpenApi(version);
        List<String> offenders = new ArrayList<>();
        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
            String key = version + " " + method.name() + " " + path;
            if (SUCCESS_WITHOUT_BODY.contains(key)) {
                return;
            }
            Map<String, ApiResponse> responses = operation.getResponses() == null ? Map.of() : operation.getResponses();
            boolean hasSuccess = responses.keySet().stream().anyMatch(code -> code.startsWith("2") || code.startsWith("3"));
            boolean bodiesHaveSchema = responses.entrySet()
                    .stream()
                    .filter(e -> e.getKey().startsWith("2"))
                    .allMatch(e -> hasSchema(e.getValue()));
            if (!hasSuccess || !bodiesHaveSchema) {
                offenders.add(key);
            }
        }));
        assertTrue(offenders.isEmpty(), version + " success responses without a body schema: " + offenders);
    }

    private static boolean hasSchema(ApiResponse response) {
        return response.getContent() != null && response.getContent().values().stream().allMatch(mediaType -> mediaType.getSchema() != null);
    }
}
