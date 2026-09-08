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
package io.goobi.viewer.api.rest.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
import io.goobi.viewer.controller.DataManager;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * @author Florian Alpers
 */
@Path("/openapi.json")
public class OpenApiResource {

    @Context
    private Application application;
    @Context
    private ServletConfig servletConfig;

    private OpenAPI openApi;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OpenAPI getOpenApi() {
        this.openApi = initSwagger(servletConfig, application, getApiUrls());
        return this.openApi;
    }

    private OpenAPI initSwagger(ServletConfig servletConfig, ResourceConfig application, List<String> apiUrls) {

        try {
            SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                    .prettyPrint(true)
                    .readAllResources(false)
                    .resourcePackages(Stream.of("io.goobi.viewer.api.rest.v1").collect(Collectors.toSet()));

            OpenAPI oApi = new JaxrsOpenApiContextBuilder()
                    .servletConfig(servletConfig)
                    .application(application)
                    .openApiConfiguration(oasConfig)
                    .buildContext(true)
                    .read();

            List<Server> servers = new ArrayList<>();
            for (String url : apiUrls) {
                Server server = new Server();
                server.setUrl(url);
                servers.add(server);
            }
            oApi.setServers(servers);

            oApi.setInfo(getInfo());
            oApi.setTags(getTags());

            return oApi;
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static List<String> getApiUrls() {

        return Arrays.asList(
                DataManager.getInstance().getRestApiManager().getDataApiManager(Version.v1).map(AbstractApiUrlManager::getApiUrl).orElse(null),
                DataManager.getInstance().getRestApiManager().getContentApiManager(Version.v1).map(AbstractApiUrlManager::getApiUrl).orElse(null))
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Made static so the build-time OpenAPI spec generator can reuse the exact same
     * {@link Info} object as the runtime resource, instead of duplicating a slimmed-down
     * copy that could drift out of sync (no instance state is used here).
     *
     * @return {@link Info}
     */
    public static Info getInfo() {
        return new Info()
                .title("Goobi viewer API.")
                .description("This documentation describes the Goobi viewer API.")
                .version("v1")
                .contact(new Contact()
                        .email("info@intranda.com"))
                .license(new License()
                        .name("GPL2 or later")
                        .url("https://github.com/intranda/goobi-viewer-core/blob/master/LICENSE"));
    }

    /**
     * Returns the global tag list of the v1 API.
     *
     * <p>Operations reference these tags by name only. Declaring them here is what gives each group in the API
     * documentation a description, and this list's order is the order in which the groups are presented. A tag
     * used by an operation but missing here still forms a group, just an undescribed one at the end.
     *
     * <p>Static for the same reason as {@link #getInfo()}: the build-time spec generator reuses it, so the
     * validated spec stays identical to what this resource serves.
     *
     * @return tags of the v1 API, in presentation order
     */
    public static List<Tag> getTags() {
        return List.of(
                tag("records", "Records addressed by persistent identifier: metadata, pages, files, images and downloads."),
                tag("iiif", "IIIF Presentation 2.1.1, Image, Search and Change Discovery resources."),
                tag("collections", "Information about and downloads of BagIt archives for a whole collection."),
                tag("search", "OpenSearch description, quick filter facets and search result exports."),
                tag("index", "Direct Solr index access: field list, geospatial search, heatmaps, queries and statistics."),
                tag("annotations", "Web Annotations on records and pages, including comments and ALTO text annotations."),
                tag("bookmarks", "Bookmark lists of the current user and public lists addressed by their share key."),
                tag("rss", "RSS feeds of recent records and of bookmark lists, as XML or JSON."),
                tag("json", "Record metadata rendered through statically configured JSON templates."),
                tag("media", "CMS media items, optionally filtered by category."),
                tag("authority", "Resolver for normdata authority resources addressed by their escaped URL."),
                tag("localization", "Translations for message keys in the configured languages."),
                tag("statistics", "Usage statistics per day or time frame, and moving wall information."),
                tag("auth", "Session login and logout."),
                tag("login", "OpenID Connect callbacks and header based login."),
                tag("users", "Information about the current user and upload of avatar images."),
                tag("clients", "Registration and administration of trusted client applications."),
                tag("tasks", "Queueing of long running tasks in a limited thread pool, and their status."),
                tag("cache", "Status of the internal image, thumbnail and PDF cache, and endpoints to clear it."),
                tag("monitoring", "Availability report for the data providing services the viewer depends on."));
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }

}
