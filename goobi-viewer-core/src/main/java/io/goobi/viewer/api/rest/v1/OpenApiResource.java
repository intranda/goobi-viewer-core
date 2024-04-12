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

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

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
     * @return {@link Info}
     */
    public Info getInfo() {
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

}
