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
import static io.goobi.viewer.api.rest.v1.ApiUrls.CLIENTS_CLIENT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.CLIENTS_REGISTER;
import static io.goobi.viewer.api.rest.v1.ApiUrls.CLIENTS_REQUEST;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(CLIENTS)
@CORSBinding
@ViewerRestServiceBinding
public class ClientApplicationsResource {

    private static final Logger logger = LogManager.getLogger(ClientApplicationsResource.class);

    private final IDAO dao;
    private final ClientApplicationManager clientManager;

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public ClientApplicationsResource() throws DAOException {
        this(DataManager.getInstance().getDao(), DataManager.getInstance().getClientManager());
    }

    public ClientApplicationsResource(IDAO dao, ClientApplicationManager clientManager) {
        this.dao = dao;
        this.clientManager = clientManager;
    }

    @POST
    @javax.ws.rs.Path(CLIENTS_REGISTER)
    @Produces({ MediaType.APPLICATION_JSON })
    //    @Operation(summary = "Request registration as a trusted client application", tags = { "clients" })
    public String register() throws ContentLibException, DAOException {
        String clientIdentifier = ClientApplicationManager.getClientIdentifier(servletRequest);
        Optional<ClientApplication> existingClient = DataManager.getInstance().getClientManager().getClientByClientIdentifier(clientIdentifier);
        if (existingClient.isPresent()) {
            throw new IllegalRequestException("Client with this machine identifier is already registered");
        }
        ClientApplication client = DataManager.getInstance().getClientManager().persistNewClient(clientIdentifier, servletRequest);
        return createRegistrationResponse(client);
    }

    @GET
    @javax.ws.rs.Path(CLIENTS_REQUEST)
    @Produces({ MediaType.APPLICATION_JSON })
    //    @Operation(summary = "Request", tags = { "clients" })
    public String request() throws ContentLibException, DAOException {
        String clientIdentifier = ClientApplicationManager.getClientIdentifier(servletRequest);
        if (StringUtils.isBlank(clientIdentifier)) {
            throw new IllegalRequestException("Missing client idenifier in header");
        }
        HttpSession session = servletRequest.getSession();
        if (session == null) {
            throw new IllegalStateException("No http session available for request");
        }
        Optional<ClientApplication> client = DataManager.getInstance().getClientManager().getClientByClientIdentifier(clientIdentifier);
        if (client.isPresent()) {
            boolean allowed = this.clientManager.registerClientInSession(client.get(), session);
            return createRequestResponse(client.get(), allowed);
        }
        throw new IllegalRequestException("No client registered with given identifier. Please register the client first");
    }

    /**
     * Change properties of an existing {@link ClientApplication}.
     * 
     * @param clientIdentifier
     * @param update
     * @return {@link ClientApplication}
     * @throws DAOException If an error occurs accessing the database
     * @throws ContentNotFoundException If no 'id' or 'clientIdentier' values are given or if no matching client could be found
     */
    @PUT
    @AuthorizationBinding
    @javax.ws.rs.Path(CLIENTS_CLIENT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Set properties of an existing client",
            description = "Set properties of the client with the clientIdentifier given in the URL to the ones specified in the data json-object."
                    + "Only properties 'name', 'description', 'subnetMask' and 'accessStatus' may be altered this way."
                    + " Requires an access token in the query paramter or header field 'token'.",
            tags = { "clients" })
    @ApiResponse(responseCode = "200",
            description = "Any changes requested have been persisted. The current state of the client is contained within the response body as JSON")
    @ApiResponse(responseCode = "400", description = "The body is not a valid JSON object or contains invalid data")
    @ApiResponse(responseCode = "401",
            description = "No authorization for access to this resource. See documentation about accessing protected resources")
    @ApiResponse(responseCode = "404", description = "No client with given clientIdentifier was found in database")
    @ApiResponse(responseCode = "500", description = "In interal error occured")
    public ClientApplication setClient(
            @PathParam("id") @Parameter(description = "client identifier") String clientIdentifier,
            ClientApplication update) throws DAOException, ContentNotFoundException {
        try {

            ClientApplication databaseClient = dao.getClientApplicationByClientId(clientIdentifier);
            if (databaseClient != null) {
                if (clientManager.isNotAllClients(databaseClient)) {
                    ClientApplication tempClient = new ClientApplication(databaseClient);
                    updateClient(tempClient, update);
                    tempClient.initializeSubnetMask();
                    dao.saveClientApplication(tempClient);
                    return tempClient;
                }
                throw new IllegalArgumentException("The requested client is internal static resource. It may not be changed");
            }
            throw new ContentNotFoundException("No client found with client-identifier '{}'".replace("{}", clientIdentifier));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * List all registered {@link ClientApplication}s.
     * 
     * @return All clients from the DB
     * @throws DAOException If an error occurs accessing the database
     */
    @GET
    @AuthorizationBinding
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Get a list of all registered clients", tags = { "clients" },
            description = "Clients are returned as json objects. Requires an access token in the query paramter or header field 'token'.")
    @ApiResponse(responseCode = "401",
            description = "No authorization for access to this resource. See documentation about accessing protected resources")
    @ApiResponse(responseCode = "500", description = "In interal error occured")
    public List<ClientApplication> getAllClients() throws DAOException {
        return dao.getAllClientApplications().stream().filter(clientManager::isNotAllClients).collect(Collectors.toList());
    }

    /**
     * List all registered {@link ClientApplication}s.
     * 
     * @param clientIdentifier
     * @return Client with given clientIdentifier
     * @throws DAOException If an error occurs accessing the database
     * @throws ContentNotFoundException
     */
    @GET
    @javax.ws.rs.Path(CLIENTS_CLIENT)
    @AuthorizationBinding
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Get the client with the given client identifier", tags = { "clients" },
            description = "The client is returned as a json object. Requires an access token in the query paramter or header field 'token'.")
    @ApiResponse(responseCode = "401",
            description = "No authorization for access to this resource. See documentation about accessing protected resources")
    @ApiResponse(responseCode = "404", description = "No client with given 'id' was found in database")
    @ApiResponse(responseCode = "500", description = "In interal error occured")
    public ClientApplication getClient(@PathParam("id") @Parameter(description = "client identifier") String clientIdentifier)
            throws DAOException, ContentNotFoundException {
        ClientApplication client = dao.getClientApplicationByClientId(clientIdentifier);
        if (client == null) {
            throw new ContentNotFoundException("No client with client identifier '{}' found".replace("{}", clientIdentifier));
        } else if (clientManager.isNotAllClients(client)) {
            return client;
        } else {
            throw new WebApplicationException(
                    new IllegalArgumentException("The requested client is internal static resource and ins unavailable for external requests"));
        }
    }

    /**
     * 
     * @param target the client to change
     * @param source the client carrying the changes to the target
     * @return target
     */
    private static ClientApplication updateClient(ClientApplication target, ClientApplication source) {

        if (source.getAccessStatus() != null) {
            if (target.isRegistrationPending() && !source.isRegistrationPending()) {
                target.setDateRegistered(LocalDateTime.now());
            }
            target.setAccessStatus(source.getAccessStatus());

        }
        if (source.getName() != null) {
            target.setName(source.getName());
        }
        if (source.getDescription() != null) {
            target.setDescription(source.getDescription());
        }
        if (source.getSubnetMask() != null) {
            if (NetTools.isValidSubnetMask(source.getSubnetMask())) {
                target.setSubnetMask(source.getSubnetMask());
            } else {
                throw new IllegalArgumentException("SubnetMask '" + source.getSubnetMask() + "' has invalid syntax");
            }
        }
        return target;
    }

    private Optional<ClientApplication> getClientApplicationFromJson(JSONObject object) throws DAOException {
        if (object.has("id")) {
            Number id = object.getNumber("id");
            return Optional.ofNullable(dao.getClientApplication(id.longValue()));
        } else if (object.has("clientIdentifier")) {
            String clientIdentifier = object.getString("clientIdentifier");
            return Optional.ofNullable(dao.getClientApplicationByClientId(clientIdentifier));
        } else {
            return Optional.empty();
        }
    }

    /**
     * @param client
     * @param allowed
     * @return JSON response
     */
    private static String createRequestResponse(ClientApplication client, boolean allowed) {
        JSONObject obj = new JSONObject();
        obj.put("access", allowed);
        obj.put("status", client.getAccessStatus());
        return obj.toString();
    }

    /**
     * @param client
     * @return JSON response
     */
    private static String createRegistrationResponse(ClientApplication client) {
        JSONObject obj = new JSONObject();
        obj.put("status", client.getAccessStatus());
        return obj.toString();
    }

}
