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
package io.goobi.viewer.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;

/**
 * @author florian
 *
 */
public class ClientApplicationFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(ClientApplicationFilter.class);

    private final ClientApplicationManager clientManager;

    public ClientApplicationFilter() {
        this.clientManager = getClientManagerFromDataManager();
    }

    public ClientApplicationFilter(ClientApplicationManager clientManager) {
        this.clientManager = clientManager;
    }

    private static ClientApplicationManager getClientManagerFromDataManager() {
        try {
            return DataManager.getInstance().getClientManager();
        } catch (DAOException e) {
            logger.error("Error setting up client application filter. Cannot register and track client applications", e);
            return null;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (clientManager == null) {
            logger.error("No ClientManager available. No client registration possible");
        } else if (request instanceof HttpServletRequest) {
            String clientIdentifier = ClientApplicationManager.getClientIdentifier((HttpServletRequest) request);
            if (StringUtils.isNotBlank(clientIdentifier)) {
                try {
                    ClientApplication client =
                            DataManager.getInstance().getClientManager().getClientByClientIdentifier(clientIdentifier).orElse(null);
                    if (client != null) {
                        if (clientManager.registerClientInSession(client, ((HttpServletRequest) request).getSession())) {
                            logger.trace("Registered client {} in http session", client.getName());
                        } else {
                            logger.trace("Client {} attempts to register but client rights are not granted for this client", client.getName());
                        }
                    } else {
                        logger.debug("Unknown client requests registration. Saving to database");
                        client = DataManager.getInstance().getClientManager().persistNewClient(clientIdentifier, (HttpServletRequest) request);
                    }
                    if (client != null && response instanceof HttpServletResponse) {
                        ((HttpServletResponse) response).setHeader(ClientApplicationManager.CLIENT_RESPONSE_HEADER,
                                client.getAccessStatus().toString());
                    }
                } catch (DAOException e) {
                    logger.error("Error registering client application for sesseion", e);
                }
            }
        }

        chain.doFilter(request, response);
    }
}
