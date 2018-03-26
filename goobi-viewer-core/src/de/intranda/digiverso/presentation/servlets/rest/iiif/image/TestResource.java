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
package de.intranda.digiverso.presentation.servlets.rest.iiif.image;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

/**
 * @author Florian Alpers
 *
 */
@Path("/imageTT")
public class TestResource {
    
    @GET
    @Path("/{a}/{b}/default")
    public String forwardToContentServer(@Context ContainerRequestContext request, @PathParam("a") String a, @PathParam("b") String b) {
            System.out.println("Received " + a + "/" + b);
            return "Received " + a + "/" + b;
    }

    @GET
    @Path("/{b}/default")
    public String forwardToContentServer1(@Context ContainerRequestContext request, @PathParam("b") String b) {
            System.out.println("Received only" + b);
            return "Received only " + b;
    }
    
}
