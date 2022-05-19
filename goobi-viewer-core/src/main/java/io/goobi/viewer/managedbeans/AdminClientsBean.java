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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.tabledata.TableDataSourceException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.clients.ClientApplication;
import io.goobi.viewer.model.clients.ClientApplication.AccessStatus;

/**
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
    private static final Logger logger = LoggerFactory.getLogger(AdminClientsBean.class);
    private static final int LIST_ENTRIES_PER_PAGE = 10;

    private final IDAO dao;

    private ClientApplication selectedClient = null;
    
    private TableDataProvider<ClientApplication> configuredClientsModel;
    
    public AdminClientsBean(IDAO dao, int listEntriesPerPage) {
        this.dao = dao;
        configuredClientsModel = createDataTableProvider(listEntriesPerPage);
    }
    
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
     * @return the configuredClientsModel
     */
    public TableDataProvider<ClientApplication> getConfiguredClientsModel() {
        return configuredClientsModel;
    }
    
    public void setSelectedClientId(Long id) throws DAOException  {
        if(id != null) {
            this.setSelectedClient(dao.getClientApplication(id));
        } else {
            this.setSelectedClient(null);
        }
    }
    
    public Long getSelectedClientId() {
        if(this.selectedClient != null) {
            return this.selectedClient.getId();
        } else {
            return null;
        }
    }
    
    /**
     * @param selectedClient the selectedClient to set
     */
    public void setSelectedClient(ClientApplication selectedClient) {
        this.selectedClient = selectedClient;
    }
    
    /**
     * @return the selectedClient
     */
    public ClientApplication getSelectedClient() {
        return selectedClient;
    }
    
    public void accept(ClientApplication client) {
        client.setAccessStatus(AccessStatus.GRANTED);
        save(client);
    }
    
    public void reject(ClientApplication client) {
        client.setAccessStatus(AccessStatus.DENIED);
        save(client);
    }

    public void save(ClientApplication client) {
        try {            
            if(dao.saveClientApplication(client)) {
                Messages.info(null, "admin__clients__save_client__success", client.getClientIdentifier());
            } else {
                Messages.error(null, "admin__clients__save_client__error", client.getClientIdentifier());
            }
        } catch(DAOException e) {
            logger.error(e.toString(), e);
            Messages.error(null, "admin__clients__save_client__error", client.getClientIdentifier());
        }
    }

    private TableDataProvider<ClientApplication> createDataTableProvider(int entriesPerPage) {
        TableDataProvider<ClientApplication> provider = new TableDataProvider<ClientApplication>(new TableDataSource<ClientApplication>() {

            private long totalNumberOfRecords = 0;
            
            @Override
            public List<ClientApplication> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters)
                    throws TableDataSourceException {
                Stream<ClientApplication> stream;
                try {
                    //get all configured clients
                    List<ClientApplication> list = dao.getAllClientApplications();
                    stream = list.stream()
                            .filter(c -> AccessStatus.REQUESTED != c.getAccessStatus());
                    
                    //filters
                    for (String  filterName : filters.keySet()) {
                        String filterValue = filters.get(filterName);
                        if(StringUtils.isNotBlank(filterValue)) {                            
                            stream = stream.filter(c -> matchesFilter(c, filterName, filterValue));
                        }
                    }
                                        
                    //sorting
                    int sortDirectionFactor = SortOrder.DESCENDING.equals(sortOrder) ? -1 : 1; // desc - asc
                    if ("name".equals(sortField)) {
                        stream = stream.sorted((c1,c2) -> sortDirectionFactor * c1.getName().compareTo(c2.getName()));
                    } else if("ip".equals(sortField)) {
                        stream = stream.sorted((c1,c2) -> sortDirectionFactor * c1.getClientIp().compareTo(c2.getClientIp()));
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
                if(totalNumberOfRecords == 0) {
                    Stream<ClientApplication> stream;
                    try {
                        stream = dao.getAllClientApplications().stream();
                        
                        //filters
                        for (String  filterName : filters.keySet()) {
                            String filterValue = filters.get(filterName);
                            if(StringUtils.isNotBlank(filterValue)) {                            
                                stream = stream.filter(c -> matchesFilter(c, filterName, filterValue));
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
                if(filterName.equals(DEFAULT_TABLE_FILTER)) {
                    return Optional.ofNullable(client.getName()).map(value -> value.contains(filterValue)).orElse(false) || 
                           Optional.ofNullable(client.getClientIp()).map(value -> value.contains(filterValue)).orElse(false) ||
                           Optional.ofNullable(client.getClientIdentifier()).map(value -> value.contains(filterValue)).orElse(false);
                } else {
                    return true;
                }
            }
        });
        
        
        provider.setEntriesPerPage(entriesPerPage);
        provider.setFilters(DEFAULT_TABLE_FILTER);
        return provider;
    }

    public List<ClientApplication> getNotConfiguredClients() throws DAOException {
        return dao.getAllClientApplications()
                .stream()
                .filter(c -> c.getAccessStatus().equals(AccessStatus.REQUESTED))
                .collect(Collectors.toList());
    }

}
