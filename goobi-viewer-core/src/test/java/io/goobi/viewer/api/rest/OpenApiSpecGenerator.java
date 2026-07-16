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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import io.goobi.viewer.api.rest.v2.OpenApiResource;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Build-time generator that produces the runtime OpenAPI spec offline (without a servlet
 * container) so it can be validated in CI. Mirrors the resource selection of OpenApiResource
 * for each API version: v1 scans by package, v2 uses the explicit class set.
 */
public final class OpenApiSpecGenerator {

    private OpenApiSpecGenerator() {
        // static-only utility
    }

    /**
     * Builds the OpenAPI model for the given API version.
     *
     * @param version "v1" or "v2"
     * @return generated OpenAPI model
     * @throws OpenApiConfigurationException on scanner/config errors
     * @should generate non empty paths for v1
     * @should generate non empty paths for v2
     * @should set info for v1
     * @should set info for v2
     */
    public static OpenAPI buildOpenApi(String version) throws OpenApiConfigurationException {
        switch (version) {
            case "v1":
                // Same config as v1/OpenApiResource.initSwagger, minus the servlet context:
                // the ClassGraph-based package scan works without servletConfig/application.
                OpenAPI v1Api = new JaxrsOpenApiContextBuilder<>()
                        .openApiConfiguration(new SwaggerConfiguration()
                                .resourcePackages(Set.of("io.goobi.viewer.api.rest.v1"))
                                .readAllResources(false))
                        .buildContext(true)
                        .read();
                // v1/OpenApiResource.getInfo() is now public static (see that class), so reuse it
                // verbatim to stay runtime-faithful instead of duplicating a slimmed-down copy that
                // could drift out of sync. Referenced by fully-qualified name here (not imported)
                // because the short name "OpenApiResource" is already taken by the v2 import above.
                v1Api.setInfo(io.goobi.viewer.api.rest.v1.OpenApiResource.getInfo());
                return v1Api;
            case "v2":
                // v2 publishes an explicit class set (no package scan) - reuse it verbatim.
                OpenAPI v2Api = new Reader(new SwaggerConfiguration().readAllResources(false))
                        .read(OpenApiResource.getResourceClasses());
                // v2/OpenApiResource.getInfo() is public static, so reuse it verbatim to stay
                // runtime-faithful; otherwise the generated spec lacks the required "info" object.
                v2Api.setInfo(OpenApiResource.getInfo());
                return v2Api;
            default:
                throw new IllegalArgumentException("Unknown API version: " + version);
        }
    }

    /**
     * Fails fast if the generated spec has no paths, which would indicate a failed resource scan
     * and otherwise slip through validation as a structurally-valid but empty (false-green) spec.
     *
     * @param openApi generated model
     * @param label version label for the error message
     * @should throw when paths are empty
     */
    public static void requireNonEmptyPaths(OpenAPI openApi, String label) {
        if (openApi == null || openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
            throw new IllegalStateException(
                    "Generated OpenAPI spec for " + label + " has no paths; resource scan likely failed");
        }
    }

    /**
     * Build-time entry point that generates OpenAPI specifications for all API versions offline
     * (without a servlet container) so they can be validated in CI. For each version (v1, v2),
     * builds the spec, fails fast if paths are empty (via {@link #requireNonEmptyPaths}),
     * and writes the result as JSON to {@code &lt;outputDir&gt;/openapi-v{1,2}.json} for downstream
     * validation (e.g., by Spectral).
     *
     * @param args optional single argument: the output directory. Must be passed as an absolute
     *             path (e.g. Maven's {@code ${project.build.directory}/openapi}) because exec:java
     *             runs in-process and a relative path would resolve against the JVM working
     *             directory (the reactor root in a multi-module build), not the module. Falls back
     *             to {@code target/openapi} when absent.
     * @throws OpenApiConfigurationException on scanner/configuration errors
     * @throws IOException on write errors
     */
    public static void main(String[] args) throws OpenApiConfigurationException, IOException {
        Path outDir = args.length > 0 && !args[0].isBlank() ? Paths.get(args[0]) : Paths.get("target", "openapi");
        Files.createDirectories(outDir);
        for (String version : new String[] { "v1", "v2" }) {
            OpenAPI openApi = buildOpenApi(version);
            requireNonEmptyPaths(openApi, version);
            Files.write(outDir.resolve("openapi-" + version + ".json"),
                    Json.pretty(openApi).getBytes(StandardCharsets.UTF_8));
        }
    }
}
