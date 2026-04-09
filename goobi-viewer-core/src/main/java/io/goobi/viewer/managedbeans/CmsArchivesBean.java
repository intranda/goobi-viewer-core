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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.CmsCollectionsBean.CMSCollectionImageMode;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.archives.ArchiveResourceWrapper;
import io.goobi.viewer.model.cms.CMSArchiveConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

/**
 * JSF backing bean for the CMS archive administration view. Manages the list of archive resources
 * and their CMS configurations, and supports editing display settings such as image modes for
 * archive tiles and headers.
 */
@Named
@SessionScoped
public class CmsArchivesBean implements Serializable {

    private static final long serialVersionUID = -3281814737728435031L;

    private static final Logger logger = LogManager.getLogger(CmsArchivesBean.class);

    static final int DEFAULT_ROWS_PER_PAGE = 10;

    private TableDataProvider<ArchiveResourceWrapper> lazyModelArchiveConfigurations;

    private Map<String, ArchiveResourceWrapper> archiveMap = new HashMap<>();

    private CMSCollectionImageMode imageModeTile = CMSCollectionImageMode.NONE;

    private CMSCollectionImageMode imageModeHeader = CMSCollectionImageMode.NONE;
    
    private ArchiveResourceWrapper selectedArchiveWrapper;

    @PostConstruct
    public void init() {
        logger.trace("init");

        for (ArchiveResource resource : DataManager.getInstance().getArchiveManager().getDatabases()) {
            logger.trace("Processing archive resource: {}", resource.getResourceId());
            ArchiveResourceWrapper wrapper = new ArchiveResourceWrapper(resource);
            archiveMap.put(resource.getResourceId(), wrapper);
            try {
                Optional<CMSArchiveConfig> config = DataManager.getInstance().getDao().getCmsArchiveConfigForArchive(resource.getResourceId());
                if (config.isPresent()) {
                    logger.trace("Found configuration for archive resource: {}", resource.getResourceId());
                    wrapper.setArchiveConfig(new CMSArchiveConfig(config.get())); // Clone DB object for editing
                } else {
                    // Make sure the CMSArchiveConfig is available early
                    wrapper.setArchiveConfig(new CMSArchiveConfig(resource.getResourceId()));
                }
            } catch (DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        lazyModelArchiveConfigurations = new TableDataProvider<>(new TableDataSource<ArchiveResourceWrapper>() {

            @Override
            public List<ArchiveResourceWrapper> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder,
                    Map<String, String> filters) {
                List<ArchiveResourceWrapper> ret = new ArrayList<>();
                for (ArchiveResource resource : DataManager.getInstance().getArchiveManager().getDatabases()) {
                    ret.add(archiveMap.get(resource.getResourceId()));
                }
                return ret;
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                return archiveMap.size();
            }

            @Override
            public void resetTotalNumberOfRecords() {
                // not applicable
            }

        });
        lazyModelArchiveConfigurations.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        // lazyModelArchiveConfigurations.getFilter("title_description");
    }

    public String saveSelectedArchiveAction() {
        if (selectedArchiveWrapper == null || selectedArchiveWrapper.getArchiveConfig() == null) {
            return "";
        }

        try {
            if (saveArchiveConfig(selectedArchiveWrapper.getArchiveConfig())) {
                Messages.info(null, "button__save__success", selectedArchiveWrapper.getArchiveResource().getResourceName());
                return "pretty:adminCmsArchives";
            }
            Messages.error("button__save__error");
        } catch (DAOException e) {
            Messages.error("button__save__error", e.getMessage());
            logger.error(e.getMessage());
        }

        return "";
    }

    public boolean saveArchiveConfig(CMSArchiveConfig config) throws DAOException {
        if (config == null) {
            return false;
        }

        config.setDateUpdated(LocalDateTime.now());
        return DataManager.getInstance().getDao().saveCMSArchiveConfig(config);
    }

    /**
     * Getter for the field <code>lazyModelArchiveConfigurations</code>.
     *
     * @return the lazy-loading table data provider for archive resource configurations
     */
    public TableDataProvider<ArchiveResourceWrapper> getLazyModelArchiveConfigurations() {
        return lazyModelArchiveConfigurations;
    }

    /**
     * getPageArchiveWrappers.
     *
     * @return a list of archive resource wrappers for the current page in the paginated archive configurations list
     */
    public List<ArchiveResourceWrapper> getPageArchiveWrappers() {
        return lazyModelArchiveConfigurations.getPaginatorList();
    }

    
    public ArchiveResourceWrapper getSelectedArchiveWrapper() {
        return selectedArchiveWrapper;
    }

    
    public void setSelectedArchiveWrapper(ArchiveResourceWrapper selectedArchiveWrapper) {
        this.selectedArchiveWrapper = selectedArchiveWrapper;
    }

    public void setSelectedResourceId(String resourceId) {
        logger.trace("setsetSelectedResourceId: {}", resourceId);
        if (StringUtils.isNotEmpty(resourceId)) {
            this.selectedArchiveWrapper = archiveMap.get(resourceId);
            if (this.selectedArchiveWrapper != null) {
                logger.trace("resource found");
                initImageMode();
                return;
            }
        }

        this.selectedArchiveWrapper = null;
    }

    
    public CMSCollectionImageMode getImageModeTile() {
        return imageModeTile;
    }

    
    public void setImageModeTile(CMSCollectionImageMode imageModeTile) {
        this.imageModeTile = imageModeTile;
    }

    
    public CMSCollectionImageMode getImageModeHeader() {
        return imageModeHeader;
    }

    
    public void setImageModeHeader(CMSCollectionImageMode imageModeHeader) {
        this.imageModeHeader = imageModeHeader;
    }

    /**
     * Sets the value of <code>imageMode</code> depending on the properties of <code>currentCollection</code>.
     *
     * @should set imageMode correctly
     */
    public void initImageMode() {
        if (selectedArchiveWrapper == null) {
            return;
        }

        if (selectedArchiveWrapper.getArchiveConfig().hasMediaItem(0)) {
            imageModeTile = CMSCollectionImageMode.IMAGE;
        } else {
            imageModeTile = CMSCollectionImageMode.NONE;
        }

        if (selectedArchiveWrapper.getArchiveConfig().hasMediaItem(1)) {
            imageModeHeader = CMSCollectionImageMode.IMAGE;
        } else {
            imageModeHeader = CMSCollectionImageMode.NONE;
        }
    }
}
