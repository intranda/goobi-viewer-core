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

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.ArchiveConnectionException;
import io.goobi.viewer.exceptions.ArchiveException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.archives.ArchiveEntry;
import io.goobi.viewer.model.archives.ArchiveManager;
import io.goobi.viewer.model.archives.ArchiveManager.DatabaseState;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.archives.ArchiveTree;
import io.goobi.viewer.model.archives.NodeType;

@Named
@SessionScoped
public class ArchiveBean implements Serializable {

    private static final long serialVersionUID = -1755934299534933504L;

    private static final Logger logger = LogManager.getLogger(ArchiveBean.class);

    private String searchString;
    private boolean databaseLoaded = false;
    private ArchiveTree archiveTree = null;
    private String currentDatabase;
    private String currentResource;
    private final ArchiveManager archiveManager;

    public ArchiveBean() {
        this.archiveManager = DataManager.getInstance().getArchiveManager();
    }

    public ArchiveBean(ArchiveManager archiveManager) {
        this.archiveManager = archiveManager;
    }

    public void reset() {
        this.currentDatabase = "";
        this.currentResource = "";
        this.searchString = "";
        this.archiveTree = null;
        this.databaseLoaded = false;
    }

    public void initializeArchiveTree() throws ArchiveException {
        initializeArchiveTree(null);
    }

    public void initializeArchiveTree(String selectedEntryId) throws ArchiveException {
        logger.trace("initializeArchiveTree: {}", selectedEntryId);
        if (getCurrentArchive() != null) {
            try {
                // this.archiveTree = new ArchiveTree(archiveManager.getArchiveTree(getCurrentDatabase(), getCurrentResource()));
                this.archiveTree = archiveManager.getArchiveTree(getCurrentDatabase(), getCurrentResource());
                this.databaseLoaded = true;
                this.searchString = "";
                this.archiveTree.resetSearch();
                if (StringUtils.isNotBlank(selectedEntryId)) {
                    this.setSelectedEntryId(selectedEntryId);
                }
            } catch (PresentationException | IllegalStateException | IndexUnreachableException e) {
                logger.error("Error initializing archive tree: {}", e.getMessage());
                Messages.error("Error initializing archive tree: " + e.getMessage());
                this.databaseLoaded = false;
                throw new ArchiveConnectionException("Error retrieving database {} from {}", getCurrentResource(), getCurrentDatabase());
            }
        }
    }

    /**
     *
     * @return actual root element of the document, even if it's not in the displayed tree
     */
    public ArchiveEntry getTrueRoot() {
        if (getArchiveTree() == null) {
            return null;
        }

        return getArchiveTree().getRootElement();
    }

    /**
     *
     * @return the archiveTree
     */
    public ArchiveTree getArchiveTree() {
        // logger.trace("getArchiveTree"); //NOSONAR Debug
        return archiveTree;
    }

    public void toggleEntryExpansion(ArchiveEntry entry) {
        logger.trace("toggleEntryExpansion: {}", entry);
        if (entry.isExpanded()) {
            collapseEntry(entry);
        } else {
            expandEntry(entry);
        }
    }

    /**
     * <p>
     * expandEntry.
     * </p>
     *
     * @param entry a {@link io.goobi.viewer.model.toc.TOCElement} object.
     */
    public void expandEntry(ArchiveEntry entry) {
        logger.trace("expandEntry: {}", entry);
        if (getArchiveTree() == null) {
            return;
        }
        synchronized (getArchiveTree()) {
            boolean updateTree = entry.isChildrenFound() && !entry.isChildrenLoaded();
            entry.expand();
            if (updateTree) {
                logger.trace("Updating tree");
                getArchiveTree().update(entry.getRootNode());
            }
        }
    }

    /**
     * <p>
     * collapseEntry.
     * </p>
     *
     * @param entry a {@link io.goobi.viewer.model.toc.TOCElement} object.
     */
    public void collapseEntry(ArchiveEntry entry) {
        logger.trace("collapseEntry: {}", entry);
        if (getArchiveTree() == null) {
            return;
        }

        synchronized (getArchiveTree()) {
            entry.collapse();
        }
    }

    /**
     * Returns the entry hierarchy from the root down to the entry with the given identifier.
     *
     * @param identifier Entry identifier
     * @return List<ArchiveEntry>
     */
    public List<ArchiveEntry> getArchiveHierarchyForIdentifier(String identifier) {
        return archiveManager.getArchiveHierarchyForIdentifier(getCurrentArchive(), identifier);
    }

    /**
     *
     * @param entry
     * @return empty string
     */
    public String selectEntryAction(ArchiveEntry entry) {
        if (entry == null || getArchiveTree() == null) {
            return "";
        }

        getArchiveTree().setSelectedEntry(entry);

        return "";
    }

    /**
     *
     * @return empty string
     */
    public String searchAction() {
        logger.trace("searchAction: {}", searchString);
        search(true, true);

        return "";
    }

    /**
     * Executes search for searchString.
     *
     * @param resetSelectedEntry If true, selected entry will be set to null
     * @param collapseAll If true, all elements will be collapsed before expanding path to search hits
     */
    void search(boolean resetSelectedEntry, boolean collapseAll) {

        if (!isDatabaseLoaded()) {
            logger.warn("Archive not loaded, cannot search.");
            return;
        }

        if (StringUtils.isEmpty(searchString)) {
            getArchiveTree().resetSearch();
            getArchiveTree().resetCollapseLevel(getArchiveTree().getRootElement(), ArchiveTree.DEFAULT_COLLAPSE_LEVEL);
            return;
        }

        getArchiveTree().search(searchString);
        List<ArchiveEntry> results;
        results = getArchiveTree().getFlatEntryList();
        if (results == null || results.isEmpty()) {
            return;
        }
        logger.trace("result entries: {}", results.size());

        if (resetSelectedEntry) {
            setSelectedEntryId(null);
        }
        getArchiveTree().collapseAll(collapseAll);
        for (ArchiveEntry entry : results) {
            if (entry.isSearchHit()) {
                entry.expandUp();
            }
        }
    }

    /**
     * @return the searchString
     */
    public String getSearchString() {
        return searchString;
    }

    /**
     * @param searchString the searchString to set
     */
    public void setSearchString(String searchString) {
        logger.trace("setSearchString: {}", searchString);
        if (!StringUtils.equals(this.searchString, searchString)) {
            this.setSelectedEntryId(null);
        }
        this.searchString = searchString;
    }

    /**
     *
     * @return ID of the selected entry
     */
    public String getSelectedEntryId() {
        if (getArchiveTree() == null || getArchiveTree().getSelectedEntry() == null) {
            return null;
        }

        return getArchiveTree().getSelectedEntry().getId();
    }

    /**
     * Setter for the URL parameter. Loads the entry that has the given ID. Loads the tree, if this is a new sessions.
     *
     * @param id Entry ID
     */
    public void setSelectedEntryId(final String id) {
        logger.trace("setSelectedEntryId: {}", id);
        if (!isDatabaseLoaded()) {
            return;
        }

        String localId = id;
        // Select root element if no ID given
        if (StringUtils.isBlank(localId)) {
            localId = "";
        }
        if ("".equals(localId)) {
            getArchiveTree().setSelectedEntry(null);
            return;
        }
        // Requested entry is already selected
        if (getArchiveTree().getSelectedEntry() != null && getArchiveTree().getSelectedEntry().getId().equals(localId)) {
            return;
        }

        // Find entry with given ID in the tree
        ArchiveEntry result = getArchiveTree().getEntryById(localId);
        if (result != null) {
            getArchiveTree().setSelectedEntry(result);
            result.expandUp();
        } else {
            logger.debug("Entry not found: {}", localId);
            getArchiveTree().setSelectedEntry(getArchiveTree().getRootElement());
        }

    }

    public boolean isSearchActive() {
        return StringUtils.isNotBlank(searchString);
    }

    /**
     *
     * @return the {@link ArchiveEntry} to display in the metadata section of the archives view. Either {@link ArchiveTree#getSelectedEntry()} or
     *         {@link ArchiveTree#getRootElement()} if the former is null
     */
    public ArchiveEntry getDisplayEntry() {
        if (getArchiveTree() == null) {
            return null;
        }

        return Optional.ofNullable(getArchiveTree().getSelectedEntry()).orElse(getArchiveTree().getRootElement());
    }

    /**
     * @return the databaseState
     */
    public DatabaseState getDatabaseState() {
        // logger.trace("getDatabaseState"); //NOSONAR Debug
        if (isDatabaseLoaded()) {
            return DatabaseState.ARCHIVE_TREE_LOADED;
        } else if (archiveManager.isInErrorState()) {
            return archiveManager.getDatabaseState();
        } else {
            archiveManager.updateArchiveList();
            return archiveManager.getDatabaseState();
        }
    }

    public boolean isDatabaseLoaded() {
        return this.databaseLoaded;
    }

    /**
     * @return the currentDatabase
     */
    public String getCurrentDatabase() {
        return currentDatabase;
    }

    /**
     * @return the currentResource
     */
    public String getCurrentResource() {
        return currentResource;
    }

    /**
     * @param currentDatabase the currentDatabase to set
     */
    public void setCurrentDatabase(String currentDatabase) {
        this.currentDatabase = currentDatabase;
    }

    /**
     * @param currentResource the currentResource to set
     */
    public void setCurrentResource(String currentResource) {
        this.currentResource = StringTools.decodeUrl(currentResource);
    }

    public ArchiveResource getCurrentArchive() {
        return archiveManager.getArchive(currentDatabase, currentResource);
    }

    public List<ArchiveResource> getDatabases() {
        return archiveManager.getDatabases();
    }

    public int getNumArchives() {
        return getDatabases().size();
    }

    public String getArchiveId() {
        return Optional.ofNullable(getCurrentArchive()).map(ArchiveResource::getCombinedId).orElse("");
    }

    public void setArchiveId(String archiveName) throws ArchiveException {
        ArchiveResource database = this.archiveManager.getArchiveResource(archiveName);
        if (database != null) {
            this.currentDatabase = database.getDatabaseId();
            this.currentResource = database.getResourceId();
            this.initializeArchiveTree();
        } else {
            this.reset();
        }
    }

    public void loadDatabaseResource(String databaseId, String resourceId) throws ArchiveException {
        this.currentDatabase = databaseId;
        this.currentResource = resourceId;
        this.initializeArchiveTree();
    }

    /**
     * If only one archive database exists and database status is {@link DatabaseState#ARCHIVES_LOADED}, redirect to the matching url.
     */
    public void redirectToOnlyDatabase() {
        if (!this.databaseLoaded) {
            this.archiveManager.getOnlyDatabaseResource().ifPresent(resource -> {
                String url = PrettyUrlTools.getAbsolutePageUrl("archives2", resource.getDatabaseId(), resource.getResourceId());
                logger.trace(url);
                try {
                    FacesContext.getCurrentInstance().getExternalContext().redirect(url);
                } catch (IOException | NullPointerException e) {
                    logger.error("Error redirecting to database url {}: {}", url, e.toString());
                }
            });
        }
    }

    public Map<String, NodeType> getUpdatedNodeTypes() {
        return DataManager.getInstance().getArchiveManager().getUpdatedNodeTypes();
    }

    /**
     * 
     * @param name Node type name
     * @return NoedType with the given name; null if none found
     */
    public NodeType getNodeType(String name) {
        return DataManager.getInstance().getArchiveManager().getNodeType(name);
    }

    public void updateArchives() {
        // logger.trace("updateArchives"); //NOSONAR Debug
        this.archiveManager.updateArchiveList();

    }

}
