/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.v1;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
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
    Application application;
    @Context 
    ServletConfig servletConfig;
    @Inject
    private AbstractApiUrlManager urls;
    
    private OpenAPI openApi;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OpenAPI getOpenApi() {
        this.openApi = initSwagger(servletConfig, application, urls.getApiPath());
        return this.openApi;
    }
    
    private OpenAPI initSwagger(ServletConfig servletConfig, ResourceConfig application, String apiUrl) {

        try {
            SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                    .prettyPrint(true)
                    .readAllResources(false)
                    .resourcePackages(Stream.of("io.goobi.viewer.api.rest").collect(Collectors.toSet()));
            
            OpenAPI openApi = new JaxrsOpenApiContextBuilder()
                    .servletConfig(servletConfig)
                    .application(application)
                    .openApiConfiguration(oasConfig)
                    .buildContext(true).read();
            
            Server server = new Server();
            server.setUrl(apiUrl);
            openApi.setServers(Collections.singletonList(server));
            
            openApi.setInfo(getInfo());
            
            return openApi;
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * @return
     */
    public Info getInfo() {
        Info info = new Info()
                .title("Goobi viewer API.")
                .description("This documentation describes the Goobi viewer API.")
                .version("v1")
                .contact(new Contact()
                        .email("info@intranda.com"))
                .license(new License()
                        .name("GPL2 or later")
                        .url("https://github.com/intranda/goobi-viewer-core/blob/master/LICENSE"));
        return info;
    }
    
}