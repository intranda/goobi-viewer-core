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
package io.goobi.viewer.api.rest.v1.clients;

import static io.goobi.viewer.api.rest.v1.ApiUrls.CLIENTS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.CLIENTS_REGISTER;
import static io.goobi.viewer.api.rest.v1.ApiUrls.CLIENTS_REQUEST;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.clients.ClientApplication;
import io.goobi.viewer.model.clients.ClientApplication.AccessStatus;
import io.swagger.v3.oas.annotations.Operation;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(CLIENTS)
@CORSBinding
public class ClientApplicationsResource {

    private static final Logger logger = LoggerFactory.getLogger(ClientApplicationsResource.class);
    
    public static final String CLIENT_IDENTIFIER_HEADER = "goobi-viewer-client-identifier";

    public static final String CLIENT_SESSION_ATTRIBUTE = "registered-client";
    
    private final IDAO dao;
    
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public ClientApplicationsResource() throws DAOException {
        this(DataManager.getInstance().getDao());
    }
    
    public ClientApplicationsResource(IDAO dao) {
        this.dao = dao;
    }
    
    @POST
    @javax.ws.rs.Path(CLIENTS_REGISTER)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Request registration as a trusted client application", tags = { "clients" })
    public String register() throws ContentLibException, DAOException {
        String clientIdentifier = getClientIdentifier(servletRequest);
        Optional<ClientApplication> existingClient = getClientByClientIdentifier(clientIdentifier);
        if(existingClient.isPresent()) {
            throw new IllegalRequestException("Client with this machine identifier is already registered");
        } else {
            ClientApplication client = new ClientApplication(clientIdentifier);
            client.setAccessStatus(AccessStatus.REQUESTED);
            String ip = NetTools.getIpAddress(servletRequest);
            if(StringUtils.isNotBlank(ip)) {
                client.setClientIp(ip);
            }
            if(dao.saveClientApplication(client)) {                
                return createRegistrationResponse(client);
            } else {
                throw new DAOException("Failed to persist client");
            }
        }
    }
    
    @GET
    @javax.ws.rs.Path(CLIENTS_REQUEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Request", tags = { "clients" })
    public String request() throws ContentLibException, DAOException {
        String clientIdentifier = getClientIdentifier(servletRequest);
        if(StringUtils.isBlank(clientIdentifier)) {
            throw new IllegalRequestException("Missing client idenifier in header");
        }
        HttpSession session = servletRequest.getSession();
        if(session == null) {
            throw new IllegalStateException("No http session available for request");
        }
        Optional<ClientApplication> client = getClientByClientIdentifier(clientIdentifier);
        if(client.isPresent()) {
            boolean allowed = registerClientInSession(client.get(), session);
            return createRequestResponse(client.get(), allowed);
        } else {
            throw new IllegalRequestException("No client registered with given identifier. Please register the client first");
        }
    }

    /**
     * @param client
     * @param allowed
     * @return
     */
    private String createRequestResponse(ClientApplication client, boolean allowed) {
        JSONObject obj = new JSONObject();
        obj.put("access", allowed);
        obj.put("status", client.getAccessStatus());
        return obj.toString();
    }

    /**
     * @param client
     * @param session
     */
    private boolean registerClientInSession(ClientApplication client, HttpSession session) {
        if(AccessStatus.GRANTED.equals(client.getAccessStatus())) {            
            session.setAttribute(CLIENT_SESSION_ATTRIBUTE, client);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param client
     * @return
     */
    private String createRegistrationResponse(ClientApplication client) {
        JSONObject obj = new JSONObject();
        obj.put("status", client.getAccessStatus());
        return obj.toString();
    }

    /**
     * @param clientIdentifier
     * @return
     * @throws DAOException 
     */
    private Optional<ClientApplication> getClientByClientIdentifier(String clientIdentifier) throws DAOException {
        return dao.getAllClientApplications().stream()
                .filter(c -> c.matchesClientIdentifier(clientIdentifier))
                .findAny();
    }

    /**
     * @param servletRequest2
     * @return
     */
    private String getClientIdentifier(HttpServletRequest request) {
       return request.getHeader(CLIENT_IDENTIFIER_HEADER);
    }
    
}
