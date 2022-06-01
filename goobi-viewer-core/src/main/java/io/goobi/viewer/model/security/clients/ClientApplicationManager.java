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
package io.goobi.viewer.model.security.clients;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.clients.ClientApplication.AccessStatus;

/**
 * @author florian
 *
 */
public class ClientApplicationManager {

    /**
     * client identifier for the core clientApplication representing all clients
     */
    public static final String GENERAL_CLIENT_IDENTIFIER = "74b2b989-753f-4eea-a3f9-8fa7243f3966";

    public static final String CLIENT_SESSION_ATTRIBUTE = "registered-client";
    public static final String CLIENT_IDENTIFIER_HEADER = "X-goobi-content-protection";
    public static final String CLIENT_RESPONSE_HEADER = "X-goobi-content-protection-status";


    private final IDAO dao;
    private ClientApplication allClients;
    
    public ClientApplicationManager(IDAO dao) throws DAOException {
        this.dao = dao;
        this.allClients = dao.getClientApplicationByClientId(GENERAL_CLIENT_IDENTIFIER);
    }
    
    /**
     * @throws DAOException 
     * 
     */
    public void addGeneralClientApplicationToDB() throws DAOException {
        
        ClientApplication client = dao.getClientApplicationByClientId(GENERAL_CLIENT_IDENTIFIER);
        if(client == null) {            
            client = new ClientApplication(GENERAL_CLIENT_IDENTIFIER);
            client.setAccessStatus(AccessStatus.NON_APPLICABLE);
            dao.saveClientApplication(client);
            this.allClients = client;
        }
        
    }
    
    /**
     * @return the allClients
     */
    public ClientApplication getAllClients() {
        return allClients;
    }
    
    public static boolean registerClientInSession(ClientApplication client, HttpSession session) {
        if(AccessStatus.GRANTED.equals(client.getAccessStatus())) {            
            session.setAttribute(CLIENT_SESSION_ATTRIBUTE, client);
            return true;
        } else {
            return false;
        }
    }
    
    public static Optional<ClientApplication> getClientFromSession(HttpSession session) {
        Object client = session.getAttribute(CLIENT_SESSION_ATTRIBUTE);
        if(client != null && client instanceof ClientApplication) {
            return Optional.of((ClientApplication)client);
        } else {
            return Optional.empty();
        }
    }
    

    /**
     * @param clientIdentifier
     * @return
     * @throws DAOException 
     */
    public Optional<ClientApplication> getClientByClientIdentifier(String clientIdentifier) throws DAOException {
        return dao.getAllClientApplications().stream()
                .filter(c -> c.matchesClientIdentifier(clientIdentifier))
                .findAny();
    }

    /**
     * @param servletRequest2
     * @return
     */
    public static String getClientIdentifier(HttpServletRequest request) {
       return request.getHeader(ClientApplicationManager.CLIENT_IDENTIFIER_HEADER);
    }
    

    public ClientApplication persistNewClient(String clientIdentifier, HttpServletRequest request) throws DAOException {
        ClientApplication client = new ClientApplication(clientIdentifier);
        client.setAccessStatus(AccessStatus.REQUESTED);
        String ip = NetTools.getIpAddress(request);
        if(StringUtils.isNotBlank(ip)) {
            client.setClientIp(ip);
        }
        if(dao.saveClientApplication(client)) {
            return client;
        } else {
            throw new DAOException("Failed to persist client");
        }

    }
}
