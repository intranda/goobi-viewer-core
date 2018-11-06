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

import java.util.Collections;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.intranda.digiverso.presentation.controller.BCrypt;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.security.authentication.model.VuAuthenticationResponse.Group;
import de.intranda.digiverso.presentation.model.security.user.User;

/**
 * Test resource for receiving vu authorization responses as POST requests
 * 
 * @author Florian Alpers
 *
 */
//@Path("/authentication/vufind")
public class VuAuthenticationResource {

//    @POST
//    @Path("/user/auth")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
    public VuAuthenticationResponse handleAuthorizationResponse(VuAuthenticationRequest request) throws DAOException {
        
        VuAuthenticationResponse.User vuUser = new VuAuthenticationResponse.User();
        vuUser.setExists(true);
        vuUser.setIsValid(true);
        Group group = new Group();
        group.setDesc("testGroup");
        vuUser.setGroup(group);
        VuAuthenticationResponse.Expired expired = new VuAuthenticationResponse.Expired();
        expired.setIsExpired(false);
        VuAuthenticationResponse response = new VuAuthenticationResponse();
        response.setUser(vuUser);
        response.setExpired(expired);
        VuAuthenticationResponse.Blocks blocks = new VuAuthenticationResponse.Blocks();
        blocks.setIsBlocked(false);
        response.setBlocks(blocks);
        
        User user = DataManager.getInstance().getDao().getUserByNickname(request.getUsername());
        if(user == null) {
            user = DataManager.getInstance().getDao().getUserByEmail(request.getUsername());
        }
        if(user == null || !user.isActive()) {
            vuUser.setExists(false);
            vuUser.setIsValid(false);
        } else if(user.isSuspended()) {
            vuUser.setIsValid(false);
        } else if(!new BCrypt().checkpw(request.getPassword(), user.getPasswordHash())) {
            blocks.setIsBlocked(false);
            VuAuthenticationResponse.Reason reason = new VuAuthenticationResponse.Reason();
            reason.setCode("07");
            reason.setNote("Passwort ungültig");
            blocks.setReasons(Collections.singletonList(reason));
        }

        return response;
    }
    
//    @POST
//    @Path("/send")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
    public VuAuthenticationResponse handleAuthorizationResponse(VuAuthenticationResponse response) {
        return response;
    }
    
//    @GET
//    @Path("/get")
//    @Produces(MediaType.APPLICATION_JSON)
    public VuAuthenticationRequest getRequest() {
        return new VuAuthenticationRequest("test", "testtesttest");
    }
}
