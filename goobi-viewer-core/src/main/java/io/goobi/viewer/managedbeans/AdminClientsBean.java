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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.tabledata.TableDataSourceException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplication.AccessStatus;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;

/**
 * Backing bean for pages adminClientEdit.xhtml and adminClients.xhtml
 * 
 * @author florian
 *
 */
@Named
@SessionScoped
public class AdminClientsBean implements Serializable {

    /**
     * 
     */
    public static final String DEFAULT_TABLE_FILTER = "name_ip_identifier";
    private static final long serialVersionUID = -614644783330750969L;
    private static final Logger logger = LogManager.getLogger(AdminClientsBean.class);
    private static final int LIST_ENTRIES_PER_PAGE = 10;

    private final IDAO dao;

    private ClientApplication selectedClient = null;

    private TableDataProvider<ClientApplication> configuredClientsModel;

    /**
     * Constructor for testing
     * 
     * @param dao
     * @param listEntriesPerPage
     */
    AdminClientsBean(IDAO dao, int listEntriesPerPage) {
        this.dao = dao;
        configuredClientsModel = createDataTableProvider(listEntriesPerPage);
    }

    /**
     * Publi no-args constructor
     */
    public AdminClientsBean() {
        try {
            dao = DataManager.getInstance().getDao();
            configuredClientsModel = createDataTableProvider(LIST_ENTRIES_PER_PAGE);
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            throw new IllegalStateException("Cannot initialize bean without connection to dao");
        }
    }

    /**
     * Get the model used for paginated listing configured clients
     * 
     * @return the configuredClientsModel
     */
    public TableDataProvider<ClientApplication> getConfiguredClientsModel() {
        return configuredClientsModel;
    }

    /**
     * Set the client currently being edited by its database id.
     * 
     * @param id database id. If null or not matching an existing client, the selected client will be null
     * @throws DAOException
     */
    public void setSelectedClientId(Long id) throws DAOException {
        if (id != null) {
            this.setSelectedClient(dao.getClientApplication(id));
        } else {
            this.setSelectedClient(null);
        }
    }

    /**
     * Get the id of the client currently being edited
     * 
     * @return client database id or null if no client is selected
     */
    public Long getSelectedClientId() {
        if (this.selectedClient != null) {
            return this.selectedClient.getId();
        }
        return null;
    }

    /**
     * Set the currently edited client
     * 
     * @param selectedClient the selectedClient to set
     */
    public void setSelectedClient(ClientApplication selectedClient) {
        this.selectedClient = selectedClient;
    }

    /**
     * Get the currently edited client
     * 
     * @return the selectedClient
     */
    public ClientApplication getSelectedClient() {
        return selectedClient;
    }

    /**
     * 'Accept' a registered client by setting its {@link ClientApplication#getAccessStatus()} to {@link AccessStatus#GRANTED}
     * 
     * @param client
     */
    public void accept(ClientApplication client) {
        client.setAccessStatus(AccessStatus.GRANTED);
        client.initializeSubnetMask();
        save(client);
    }

    /**
     * 'Regect a registered client by calling {@link #delete(ClientApplication)} on it
     * 
     * @param client
     * @return pretty url of admin/clients overview page
     */
    public String reject(ClientApplication client) {
        delete(client);
        return "pretty:adminClients";
    }

    /**
     * Save the given client to database
     * 
     * @param client
     */
    public void save(ClientApplication client) {
        String clientId = Optional.ofNullable(client).map(ClientApplication::getClientIdentifier).orElse("-");
        try {
            if (dao.saveClientApplication(client)) {
                Messages.info(null, "admin__clients__save_client__success", clientId);
            } else {
                Messages.error(null, "admin__clients__save_client__error", clientId);
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            Messages.error(null, "admin__clients__save_client__error", clientId);
        }
    }

    /**
     * Delete given client from database
     * 
     * @param client
     * @return Navigation outcome
     */
    public String delete(ClientApplication client) {
        String clientId = Optional.ofNullable(client).map(ClientApplication::getClientIdentifier).orElse("-");
        try {
            if (client != null && dao.deleteClientApplication(client.getId())) {
                Messages.info(null, "admin__clients__delete_client__success", clientId);
            } else {
                Messages.error(null, "admin__clients__delete_client__error", clientId);
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            Messages.error(null, "admin__clients__delete_client__error", clientId);
        }
        return "pretty:adminClients";
    }

    private TableDataProvider<ClientApplication> createDataTableProvider(int entriesPerPage) {
        TableDataProvider<ClientApplication> provider = new TableDataProvider<>(new TableDataSource<ClientApplication>() {

            private long totalNumberOfRecords = 0;

            @Override
            public List<ClientApplication> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters)
                    throws TableDataSourceException {
                Stream<ClientApplication> stream;
                try {
                    //get all configured clients
                    List<ClientApplication> list = dao.getAllClientApplications();
                    stream = list.stream()
                            .filter(c -> AccessStatus.NON_APPLICABLE != c.getAccessStatus())
                            .filter(c -> AccessStatus.REQUESTED != c.getAccessStatus());

                    //filters
                    for (Entry<String, String> entry : filters.entrySet()) {
                        String filterValue = entry.getValue();
                        if (StringUtils.isNotBlank(filterValue)) {
                            stream = stream.filter(c -> matchesFilter(c, entry.getKey(), filterValue));
                        }
                    }

                    //sorting
                    int sortDirectionFactor = SortOrder.DESCENDING.equals(sortOrder) ? -1 : 1; // desc - asc
                    if ("name".equals(sortField)) {
                        stream = stream.sorted((c1, c2) -> sortDirectionFactor * StringUtils.compare(c1.getName(), c2.getName()))
                                .sorted((c1, c2) -> sortDirectionFactor * StringUtils.compare(c1.getClientIdentifier(), c2.getClientIdentifier()));
                    } else if ("ip".equals(sortField)) {
                        stream = stream.sorted((c1, c2) -> sortDirectionFactor * StringUtils.compare(c1.getClientIp(), c2.getClientIp()));
                    } else if ("dateRegistered".equals(sortField)) {
                        stream = stream.sorted((c1, c2) -> sortDirectionFactor * c1.getDateRegistered().compareTo(c2.getDateRegistered()));
                    } else if ("dateLastAccess".equals(sortField)) {
                        stream = stream.sorted((c1, c2) -> sortDirectionFactor * c1.getDateLastAccess().compareTo(c2.getDateLastAccess()));
                    }

                    //from-to
                    stream = stream.skip(first).limit(pageSize);

                    return stream.collect(Collectors.toList());

                } catch (DAOException e) {
                    throw new TableDataSourceException(e);
                }

            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (totalNumberOfRecords == 0) {
                    Stream<ClientApplication> stream;
                    try {
                        stream = dao.getAllClientApplications().stream();

                        //filters
                        for (Entry<String, String> entry : filters.entrySet()) {
                            String filterValue = entry.getValue();
                            if (StringUtils.isNotBlank(filterValue)) {
                                stream = stream.filter(c -> matchesFilter(c, entry.getKey(), filterValue));
                            }
                        }

                        this.totalNumberOfRecords = stream.count();
                    } catch (DAOException e) {
                        throw new TableDataSourceException(e);
                    }
                }
                return this.totalNumberOfRecords;
            }

            @Override
            public void resetTotalNumberOfRecords() {
                this.totalNumberOfRecords = 0;
            }

            private boolean matchesFilter(ClientApplication client, String filterName, String filterValue) {
                if (filterName.equals(DEFAULT_TABLE_FILTER)) {
                    return Optional.ofNullable(client.getName()).map(value -> value.contains(filterValue)).orElse(false)
                            || Optional.ofNullable(client.getClientIp()).map(value -> value.contains(filterValue)).orElse(false)
                            || Optional.ofNullable(client.getClientIdentifier()).map(value -> value.contains(filterValue)).orElse(false);
                }
                return true;
            }
        });

        provider.setEntriesPerPage(entriesPerPage);
        provider.getFilter(DEFAULT_TABLE_FILTER);
        return provider;
    }

    /**
     * Get a list of all clients with {@link AccessStatus#REQUESTED}
     * 
     * @return List of clients that are not configured
     * @throws DAOException
     */
    public List<ClientApplication> getNotConfiguredClients() throws DAOException {
        return dao.getAllClientApplications()
                .stream()
                .filter(c -> c.getAccessStatus().equals(AccessStatus.REQUESTED))
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all clients with {@link AccessStatus#GRANTED}
     * 
     * @return List of clients that have been granted
     * @throws DAOException
     */
    public List<ClientApplication> getAllAcceptedClients() throws DAOException {
        return dao.getAllClientApplications()
                .stream()
                .filter(c -> c.getAccessStatus().equals(AccessStatus.GRANTED))
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all clients with {@link AccessStatus#GRANTED} pr {@link AccessStatus#DENIED}
     * 
     * @return List of clients that have been either granted or denied
     * @throws DAOException
     */
    public List<ClientApplication> getAllConfiguredClients() throws DAOException {
        return dao.getAllClientApplications()
                .stream()
                .filter(c -> c.getAccessStatus().equals(AccessStatus.DENIED) || c.getAccessStatus().equals(AccessStatus.GRANTED))
                .collect(Collectors.toList());
    }

    /**
     * Get the internally created client representing all clients for access rights purposes
     * 
     * @return the allClients
     * @throws DAOException
     */
    public ClientApplication getAllClients() throws DAOException {
        return DataManager.getInstance().getClientManager().getAllClients();
    }

    /**
     * Check if the current session is with a client application, i.e. if client requests contain the client-application-id header
     * 
     * @return true if session belongs to a client application
     */
    public boolean isClientLoggedIn() {
        return !ClientApplicationManager.getClientFromSession(BeanUtils.getSession()).isEmpty();
    }

    /**
     * Check if a client application is logged in that is applicable for access privileges
     * 
     * @return true if the session contains a clientApplication with the accessStatus {@link AccessStatus#GRANTED} and if the request ip matches the
     *         client's subnet mask
     */
    public boolean isLoggedInClientAccessGranted() {
        return ClientApplicationManager.getClientFromRequest(BeanUtils.getRequest())
                .map(ClientApplication::isAccessGranted)
                .orElse(false);
    }

    public boolean isLoggedInClientFromAllowedIP() {
        HttpServletRequest request = BeanUtils.getRequest();
        if (request != null) {
            return ClientApplicationManager.getClientFromRequest(BeanUtils.getRequest())
                    .map(client -> client.matchIp(NetTools.getIpAddress(request)))
                    .orElse(false);
        }
        return false;
    }
}
