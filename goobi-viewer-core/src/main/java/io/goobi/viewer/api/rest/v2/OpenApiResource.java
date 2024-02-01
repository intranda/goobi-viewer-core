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
package io.goobi.viewer.api.rest.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
import io.goobi.viewer.api.rest.v2.cms.CMSMediaImageResource3;
import io.goobi.viewer.api.rest.v2.collections.CollectionsResource;
import io.goobi.viewer.api.rest.v2.media.ExternalImageResource;
import io.goobi.viewer.api.rest.v2.records.RecordFilesResource;
import io.goobi.viewer.api.rest.v2.records.RecordPagesResource;
import io.goobi.viewer.api.rest.v2.records.RecordResource;
import io.goobi.viewer.api.rest.v2.records.RecordSectionsResource;
import io.goobi.viewer.api.rest.v2.records.media.RecordsFilesImageResource;
import io.goobi.viewer.api.rest.v2.records.media.RecordsImageResource;
import io.goobi.viewer.controller.DataManager;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * @author florian
 *
 */
@Path("/openapi.json")
public class OpenApiResource {

    @Context
    private Application application;
    @Context
    private ServletConfig servletConfig;

    private static OpenAPI openApi = null;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OpenAPI getOpenApi() {
        OpenApiResource.initOpenApi(getApiUrls());
        return OpenApiResource.openApi;
    }

    private static void initOpenApi(List<String> apiUrls) {
        if (OpenApiResource.openApi == null) {
            OpenApiResource.openApi = initSwagger(apiUrls);
        }
    }

    private static OpenAPI initSwagger(List<String> apiUrls) {

        OpenAPI oas = new OpenAPI();
        oas.info(getInfo());
        oas.servers(getServers(apiUrls));

        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .readAllResources(false);

        Reader reader = new Reader(oasConfig);
        return reader.read(Stream.of(
                CMSMediaImageResource3.class,
                CollectionsResource.class,
                ExternalImageResource.class,
                RecordsFilesImageResource.class,
                RecordsImageResource.class,
                RecordFilesResource.class,
                RecordPagesResource.class,
                RecordResource.class,
                RecordSectionsResource.class).collect(Collectors.toSet()));

    }

    private static List<String> getApiUrls() {

        return Arrays.asList(
                DataManager.getInstance().getRestApiManager().getDataApiManager(Version.v2).map(AbstractApiUrlManager::getApiUrl).orElse(null),
                DataManager.getInstance().getRestApiManager().getContentApiManager(Version.v2).map(AbstractApiUrlManager::getApiUrl).orElse(null))
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private static List<Server> getServers(List<String> apiUrls) {
        List<Server> servers = new ArrayList<>();
        for (String url : apiUrls) {
            Server server = new Server();
            server.setUrl(url);
            servers.add(server);
        }
        return servers;
    }

    /**
     * @return {@link Info}
     */
    public static Info getInfo() {
        return new Info()
                .title("Goobi viewer API.")
                .description("This documentation describes the Goobi viewer API.")
                .version("v2")
                .contact(new Contact()
                        .email("info@intranda.com"))
                .license(new License()
                        .name("GPL2 or later")
                        .url("https://github.com/intranda/goobi-viewer-core/blob/master/LICENSE"));
    }

}
