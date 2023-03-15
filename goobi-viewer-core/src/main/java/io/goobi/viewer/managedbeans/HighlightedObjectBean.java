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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.model.cms.HighlightedObject;
import io.goobi.viewer.model.cms.recordnotes.CMSRecordNote;

@Named
@ViewScoped
public class HighlightedObjectBean implements Serializable {

    private static final int NUM_ITEMS_PER_PAGE = 12;
    private static final long serialVersionUID = -6647395682752991930L;
    private static final Logger logger = LogManager.getLogger(HighlightedObjectBean.class);
    
    private TableDataProvider<HighlightedObject> dataProvider;

    @PostConstruct
    public void init() {
        if (dataProvider == null) {
            initDataProvider();
        }
    }

    /**
     * @return the dataProvider
     */
    public TableDataProvider<HighlightedObject> getDataProvider() {
        return dataProvider;
    }
    
    private void initDataProvider() {
        dataProvider = new TableDataProvider<>(new TableDataSource<HighlightedObject>() {

            private Optional<Long> numItems = Optional.empty();

            @Override
            public List<HighlightedObject> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                try {
                    if (StringUtils.isBlank(sortField)) {
                        sortField = "id";
                    }

                    return DataManager.getInstance()
                            .getDao()
                            .getHighlightedObjects(first, pageSize, sortField, sortOrder.asBoolean(), filters)
                            .stream().map(HighlightedObject::new).collect(Collectors.toList());
                } catch (DAOException e) {
                    logger.error("Could not initialize lazy model: {}", e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (!numItems.isPresent()) {
                    try {
                        numItems = Optional.of( DataManager.getInstance()
                                .getDao()
                                .getHighlightedObjects(0, Integer.MAX_VALUE, null, false, filters)
                                .stream().count());
                    } catch (DAOException e) {
                        logger.error("Unable to retrieve total number of cms pages", e);
                    }
                }
                return numItems.orElse(0L);
            }

            @Override
            public void resetTotalNumberOfRecords() {
                numItems = Optional.empty();
            }
        });
        dataProvider.setEntriesPerPage(NUM_ITEMS_PER_PAGE);
    }
    
}
