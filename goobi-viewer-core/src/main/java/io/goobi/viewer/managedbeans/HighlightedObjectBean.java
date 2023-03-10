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

            private Optional<Long> numCreatedPages = Optional.empty();

            @Override
            public List<HighlightedObject> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                try {
                    if (StringUtils.isBlank(sortField)) {
                        sortField = "id";
                    }

                    return DataManager.getInstance()
                            .getDao()
                            .getRecordNotes(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error("Could not initialize lazy model: {}", e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (!numCreatedPages.isPresent()) {
                    try {
                        List<CMSRecordNote> notes = DataManager.getInstance()
                                .getDao()
                                .getRecordNotes(0, Integer.MAX_VALUE, null, false, filters);
                        numCreatedPages = Optional.of((long) notes.size());
                    } catch (DAOException e) {
                        logger.error("Unable to retrieve total number of cms pages", e);
                    }
                }
                return numCreatedPages.orElse(0L);
            }

            @Override
            public void resetTotalNumberOfRecords() {
                numCreatedPages = Optional.empty();
            }
        });
        dataProvider.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        dataProvider.addFilter(PI_TITLE_FILTER);
        //            lazyModelPages.addFilter("CMSCategory", "name");
    }
    
}
