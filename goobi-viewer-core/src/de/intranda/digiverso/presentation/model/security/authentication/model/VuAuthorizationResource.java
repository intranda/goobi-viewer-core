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
package de.intranda.digiverso.presentation.model.security.authentication.model;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Test resource for receiving vu authorization responses as POST requests
 * 
 * @author Florian Alpers
 *
 */
@Path("/authentication/vufind")
public class VuAuthorizationResource {

    @POST
    @Path("/user/auth")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public VuAuthenticationResponse handleAuthorizationResponse(VuAuthenticationRequest request) {
        System.out.println("received request " + request);
        VuAuthenticationResponse response = new VuAuthenticationResponse();
        VuAuthenticationResponse.User user = new VuAuthenticationResponse.User();
        user.setExists(true);
        user.setIsValid(true);
        user.setGroup("my group");
        VuAuthenticationResponse.Expired expired = new VuAuthenticationResponse.Expired();
        expired.setIsExpired(false);
        response.setUser(user);
        response.setExpired(expired);
        return response;
    }
    
    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public VuAuthenticationResponse handleAuthorizationResponse(VuAuthenticationResponse response) {
        System.out.println("received response " + response);
        return response;
    }
    
    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public VuAuthenticationRequest getRequest() {
        return new VuAuthenticationRequest("test", "testtesttest");
    }
}
