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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.cms.CMSArchiveConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

@Named
@SessionScoped
public class CmsArchivesBean implements Serializable {

    private static final long serialVersionUID = -3281814737728435031L;

    private static final Logger logger = LogManager.getLogger(CmsArchivesBean.class);

    static final int DEFAULT_ROWS_PER_PAGE = 10;

    private TableDataProvider<ArchiveResourceWrapper> lazyModelArchiveConfigurations;

    private ArchiveResourceWrapper selectedArchiveWrapper;

    private Map<String, ArchiveResourceWrapper> archiveMap = new HashMap<>();

    @PostConstruct
    public void init() {

        lazyModelArchiveConfigurations = new TableDataProvider<>(new TableDataSource<ArchiveResourceWrapper>() {

            @Override
            public List<ArchiveResourceWrapper> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder,
                    Map<String, String> filters) {
                logger.trace("getEntries<ArchiveResourceWrapper>, {}-{}", first, first + pageSize);
                try {
                    String useSortField = sortField;
                    SortOrder useSortOrder = sortOrder;
                    if (StringUtils.isBlank(useSortField)) {
                        useSortField = "id";
                    }
                    List<ArchiveResourceWrapper> ret = new ArrayList<>();
                    for (ArchiveResource resource : DataManager.getInstance().getArchiveManager().getDatabases()) {
                        logger.trace("Processing archive resource: {}", resource.getResourceId());
                        ArchiveResourceWrapper wrapper = new ArchiveResourceWrapper(resource);
                        ret.add(wrapper);
                        archiveMap.put(resource.getResourceId(), wrapper);
                        Optional<CMSArchiveConfig> config =
                                DataManager.getInstance().getDao().getCmsArchiveConfigForArchive(resource.getResourceId());
                        if (config.isPresent()) {
                            logger.trace("Found configuration for archive resource: {}", resource.getResourceId());
                            wrapper.setArchiveConfig(config.get());
                        }
                    }
                    return ret;
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                }
                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                return DataManager.getInstance().getArchiveManager().getDatabases().size();
            }

            @Override
            public void resetTotalNumberOfRecords() {

            }

        });
        lazyModelArchiveConfigurations.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelArchiveConfigurations.getFilter("title_description");
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelArchiveConfigurations</code>.
     * </p>
     *
     * @return the lazyModelArchiveConfigurations
     */
    public TableDataProvider<ArchiveResourceWrapper> getLazyModelArchiveConfigurations() {
        return lazyModelArchiveConfigurations;
    }

    /**
     * <p>
     * getPageArchiveWrappers.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<ArchiveResourceWrapper> getPageArchiveWrappers() {
        return lazyModelArchiveConfigurations.getPaginatorList();
    }

    /**
     * Wrapper class for archive and configuration pairs.
     */
    public class ArchiveResourceWrapper {
        private final ArchiveResource archiveResource;
        private CMSArchiveConfig archiveConfig;

        public ArchiveResourceWrapper(ArchiveResource archiveResource) {
            this.archiveResource = archiveResource;
        }

        /**
         * @return the archiveResource
         */
        public ArchiveResource getArchiveResource() {
            return archiveResource;
        }

        /**
         * @return the archiveConfig
         */
        public CMSArchiveConfig getArchiveConfig() {
            return archiveConfig;
        }

        /**
         * @param archiveConfig the archiveConfig to set
         */
        public void setArchiveConfig(CMSArchiveConfig archiveConfig) {
            this.archiveConfig = archiveConfig;
        }
    }

    /**
     * @return the selectedArchiveWrapper
     */
    public ArchiveResourceWrapper getSelectedArchiveWrapper() {
        return selectedArchiveWrapper;
    }

    /**
     * @param selectedArchiveWrapper the selectedArchiveWrapper to set
     */
    public void setSelectedArchiveWrapper(ArchiveResourceWrapper selectedArchiveWrapper) {
        this.selectedArchiveWrapper = selectedArchiveWrapper;
    }

    public void setsetSelectedResourceId(String resourceId) {
        logger.trace("setsetSelectedResourceId: {");
        if (StringUtils.isNotEmpty(resourceId)) {
            this.selectedArchiveWrapper = archiveMap.get(resourceId);
        }

        this.selectedArchiveWrapper = null;
    }
}
