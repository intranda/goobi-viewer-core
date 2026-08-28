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
package io.goobi.viewer.api.rest.filters;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;

/**
 * Verifies the override guard against a running container rather than a mock, because the
 * visibility of servlet-set headers inside a JAX-RS response filter depends on the container.
 */
class ApiCacheControlResponseFilterContainerTest extends JerseyTest {

    /**
     * Independent of test execution order: this class does not extend {@link AbstractTest}, so nothing else
     * resets the shared {@link DataManager} configuration between test classes.
     */
    @BeforeEach
    void injectTestConfiguration() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration(AbstractTest.TEST_CONFIG_PATH));
    }

    @Path("/guard")
    public static class GuardResource {

        @GET
        @Path("/preset")
        @Produces(MediaType.APPLICATION_JSON)
        public Response preset(@Context HttpServletResponse servletResponse) {
            servletResponse.addHeader("Cache-Control", "private, max-age=300");
            return Response.ok("{}").build();
        }

        @GET
        @Path("/plain")
        @Produces(MediaType.APPLICATION_JSON)
        public Response plain() {
            return Response.ok("{}").build();
        }
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.forServlet(new ServletContainer(
                new ResourceConfig(GuardResource.class, ApiCacheControlResponseFilter.class))).build();
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(jakarta.ws.rs.container.ContainerRequestContext,
     *      jakarta.ws.rs.container.ContainerResponseContext)
     * @verifies keep a header set on the raw servlet response inside a container
     */
    @Test
    void filter_shouldKeepAHeaderSetOnTheRawServletResponseInsideAContainer() {
        Response response = target("/guard/preset").request().get();

        Assertions.assertEquals("private, max-age=300", response.getHeaderString("Cache-Control"));
    }

    /**
     * @see ApiCacheControlResponseFilter#filter(jakarta.ws.rs.container.ContainerRequestContext,
     *      jakarta.ws.rs.container.ContainerResponseContext)
     * @verifies apply the data policy inside a container
     */
    @Test
    void filter_shouldApplyTheDataPolicyInsideAContainer() {
        Response response = target("/guard/plain").request().get();

        Assertions.assertEquals("no-store", response.getHeaderString("Cache-Control"));
    }
}
