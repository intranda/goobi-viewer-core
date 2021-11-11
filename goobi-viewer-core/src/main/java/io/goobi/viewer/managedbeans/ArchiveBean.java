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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.monitoring.timer.Time;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.BaseXException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.archives.ArchiveEntry;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.archives.ArchiveTree;
import io.goobi.viewer.model.archives.BasexEADParser;
import io.goobi.viewer.model.archives.NodeType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

@Named
@SessionScoped
public class ArchiveBean implements Serializable {

    private static final long serialVersionUID = -1755934299534933504L;

    private static final Logger logger = LoggerFactory.getLogger(ArchiveBean.class);

    private String searchString;

    private DatabaseState databaseState = DatabaseState.NOT_INITIALIZED;

    private Map<ArchiveResource, ArchiveTree> archives = new HashMap<>();

    private final List<NodeType> nodeTypes;

    private final BasexEADParser eadParser;

    private String currentDatabase;
    private String currentResource;

    //    @Inject
    //    private FacesContext context;

    private static enum DatabaseState {
        NOT_INITIALIZED,
        ARCHIVES_LOADED,
        ARCHIVE_TREE_LOADED,
        ERROR_NOT_CONFIGURED,
        ERROR_NOT_REACHABLE,
        ERROR_INVALID_FORMAT
    }

    public ArchiveBean() {
        String basexUrl = DataManager.getInstance().getConfiguration().getBaseXUrl();
        BasexEADParser eadParser = null;
        if (DataManager.getInstance().getConfiguration().isArchivesEnabled()) {
            try {
                eadParser = new BasexEADParser(basexUrl);
                //initialize archives with 'null' archive tree values
                List<ArchiveResource> databases = eadParser.getPossibleDatabases();
                for (ArchiveResource db : databases) {
                    this.archives.put(db, null);
                }
                //this.archives = eadParser.getPossibleDatabases().stream().collect(Collectors.toMap(db -> db, db -> null));
                this.databaseState = DatabaseState.ARCHIVES_LOADED;
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Failed to retrieve associated records from SOLR: {}", e.toString());
                this.databaseState = DatabaseState.ERROR_NOT_CONFIGURED;
            } catch (IOException | HTTPException e) {
                logger.error("Failed to retrieve database names from '{}': {}", basexUrl, e.toString());
                this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
            }
        }
        this.eadParser = eadParser;
        this.nodeTypes = loadNodeTypes(DataManager.getInstance().getConfiguration().getArchiveNodeTypes());
    }

    /**
     * @param archiveNodeTypes
     * @return
     */
    private List<NodeType> loadNodeTypes(Map<String, String> archiveNodeTypes) {
        return archiveNodeTypes.entrySet().stream().map(entry -> new NodeType(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    public void reset() {
        this.currentDatabase = "";
        this.currentResource = "";
        this.searchString = "";
        if (this.databaseState == DatabaseState.ARCHIVE_TREE_LOADED) {
            this.databaseState = DatabaseState.ARCHIVES_LOADED;
        }
    }

    public void initializeArchiveTree() {
        initializeArchiveTree(null);
    }

    public void initializeArchiveTree(String selectedEntryId) {

        if (getCurrentArchive() != null) {
            try {
                if (this.archives.get(getCurrentArchive()) == null) {
                    ArchiveTree archiveTree = loadDatabase(eadParser, getCurrentArchive());
                    if (archiveTree != null) {
                        this.archives.put(getCurrentArchive(), archiveTree);
                        this.databaseState = DatabaseState.ARCHIVE_TREE_LOADED;
                    }
                } else {
                    this.databaseState = DatabaseState.ARCHIVE_TREE_LOADED;
                }
            } catch (IOException | HTTPException e) {
                logger.error("Error retrieving database {} from {}", getCurrentArchive().getCombinedName(), eadParser.getBasexUrl());
                this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
            } catch (JDOMException | ConfigurationException e) {
                logger.error("Error reading database {} from {}", getCurrentArchive().getCombinedName(), eadParser.getBasexUrl());
                this.databaseState = DatabaseState.ERROR_INVALID_FORMAT;
            }
            this.searchString = "";
            getArchiveTree().resetSearch();
        }

        //load selected entry
        if (isDatabaseLoaded() && StringUtils.isNotBlank(selectedEntryId)) {
            this.setSelectedEntryId(selectedEntryId);
        }
    }

    ArchiveTree loadDatabase(BasexEADParser eadParser, ArchiveResource archive)
            throws ConfigurationException, IllegalStateException, IOException, HTTPException, JDOMException {
        HierarchicalConfiguration<ImmutableNode> baseXMetadataConfig = DataManager.getInstance().getConfiguration().getArchiveMetadataConfig();
        eadParser.readConfiguration(baseXMetadataConfig);
        Document databaseDoc = eadParser.retrieveDatabaseDocument(archive);
        ArchiveEntry rootElement = eadParser.loadDatabase(archive, databaseDoc);
        logger.info("Loaded EAD database: {}", archive.getCombinedName());
        return loadTree(rootElement);
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
     * @return
     * @throws BaseXException
     */
    public ArchiveTree getArchiveTree() {
        return archives.get(getCurrentArchive());
    }

    /**
     * @param rootElement
     * @return
     */
    ArchiveTree loadTree(ArchiveEntry rootElement) {
        ArchiveTree ret = new ArchiveTree();
        ret.generate(rootElement);
        if (ret.getSelectedEntry() == null) {
            ret.setSelectedEntry(ret.getRootElement());
        }
        // This should happen before the tree is expanded to the selected entry, otherwise the collapse level will be reset
        ret.getTreeView();

        return ret;
    }

    public void toggleEntryExpansion(ArchiveEntry entry) {
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
            entry.expand();
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
     * @param List of entries An empty list if the identified node has no ancestors or doesn't exist
     */
    public List<ArchiveEntry> getArchiveHierarchyForIdentifier(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyList();
        }

        if (getArchiveTree() == null) {
            logger.error("Archive not loaded");
            return Collections.emptyList();
        }

        ArchiveEntry entry = getArchiveTree().getEntryById(identifier);
        if (entry == null) {
            //            return Collections.emptyList();
            return Collections.singletonList(getTrueRoot());
        } else if (getTrueRoot().equals(entry) || getTrueRoot().equals(entry.getParentNode())) {
            return Collections.singletonList(entry);
        } else {
            return entry.getAncestors(false).stream().skip(1).collect(Collectors.toList());
        }
    }

    /**
     * 
     * @param entry
     * @return
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
     * @return
     * @throws BaseXException
     */
    public String searchAction() throws BaseXException {
        logger.trace("searchAction: {}", searchString);
        search(true, true);

        return "";
    }

    /**
     * Executes search for searchString.
     * 
     * @param resetSelectedEntry If true, selected entry will be set to null
     * @param collapseAll If true, all elements will be collapsed before expanding path to search hits
     * @throws BaseXException
     */
    void search(boolean resetSelectedEntry, boolean collapseAll) throws BaseXException {
        if (!isDatabaseLoaded()) {
            logger.warn("Archive not loaded, cannot search.");
            return;
        }

        if (StringUtils.isEmpty(searchString)) {
            getArchiveTree().resetSearch();
            getArchiveTree().resetCollapseLevel(getArchiveTree().getRootElement(), ArchiveTree.defaultCollapseLevel);
            return;
        }

        getArchiveTree().search(searchString);
        List<ArchiveEntry> results = getArchiveTree().getFlatEntryList();
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
     * @return
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
     * @throws BaseXException
     */
    public void setSelectedEntryId(String id) {
        logger.trace("setSelectedEntryId: {}", id);
        if (!isDatabaseLoaded()) {
            return;
        }

        // Select root element if no ID given
        if (StringUtils.isBlank(id)) {
            id = "";
        }
        if ("".equals(id)) {
            getArchiveTree().setSelectedEntry(null);
            return;
        }
        // Requested entry is already selected
        if (getArchiveTree().getSelectedEntry() != null && getArchiveTree().getSelectedEntry().getId().equals(id)) {
            return;
        }

        // Find entry with given ID in the tree
        ArchiveEntry result = getArchiveTree().getEntryById(id);
        if (result != null) {
            getArchiveTree().setSelectedEntry(result);
            result.expandUp();
        } else {
            logger.debug("Entry not found: {}", id);
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
     * In the list of archive document search hits, find the id of the entry just before the given one
     * 
     * @param entry
     * @param sortOrder 'asc' to get the preceding entry, 'desc' to get the succeeding one
     * @return the neighboring entry id if it exists
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Pair<Optional<String>, Optional<String>> findIndexedNeighbours(String entryId)
            throws PresentationException, IndexUnreachableException {
        String query = SolrConstants.ARCHIVE_ENTRY_ID + ":*";
        List<StringPair> sortFields = Collections.singletonList(new StringPair(SolrConstants.PI, "asc"));
        List<String> fieldList = Arrays.asList(SolrConstants.PI, SolrConstants.ARCHIVE_ENTRY_ID);

        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, SolrSearchIndex.MAX_HITS, sortFields, fieldList);
        Optional<String> prev = Optional.empty();
        Optional<String> next = Optional.empty();

        ListIterator<SolrDocument> iter = docs.listIterator();
        while (iter.hasNext()) {
            SolrDocument doc = iter.next();
            String id = SolrTools.getSingleFieldStringValue(doc, SolrConstants.ARCHIVE_ENTRY_ID);
            if (id.equals(entryId)) {
                if (iter.hasNext()) {
                    String nextId = SolrTools.getSingleFieldStringValue(iter.next(), SolrConstants.ARCHIVE_ENTRY_ID);
                    next = Optional.of(nextId);
                }
                break;
            }
            prev = Optional.of(id);
        }
        return Pair.of(prev, next);
    }

    /**
     * @return the databaseState
     */
    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    public boolean isDatabaseLoaded() {
        return DatabaseState.ARCHIVE_TREE_LOADED.equals(this.databaseState);
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
        if (StringUtils.isNoneBlank(currentDatabase, currentResource)) {
            ArchiveResource archive = this.archives.keySet()
                    .stream()
//                    .peek(a -> System.out.println("Archive " + a.getDatabaseId() + " - " + a.getResourceId()))
                    .filter(a -> a.getDatabaseId().equals(currentDatabase))
                    .filter(a -> a.getResourceId().equals(currentResource))
                    .findAny()
                    .orElse(null);
            return archive;
        } else {
            return null;
        }
    }

    public List<ArchiveResource> getDatabases() {
        List<ArchiveResource> databases = new ArrayList<>(this.archives.keySet());
        databases.sort((db1, db2) -> db1.getCombinedName().compareTo(db2.getCombinedName()));
        return databases;
    }
    public int getNumArchives() {
        return this.archives.size();
    }

    public String getArchiveId() {
        return Optional.ofNullable(getCurrentArchive()).map(ArchiveResource::getCombinedId).orElse("");
    }

    public void setArchiveId(String archiveName) {
        ArchiveResource database = this.archives.keySet().stream().filter(db -> db.getCombinedId().equals(archiveName)).findAny().orElse(null);
        if (database != null) {
            this.currentDatabase = database.getDatabaseId();
            this.currentResource = database.getResourceId();
            this.initializeArchiveTree();
        } else {
            this.reset();
        }
    }

    public void loadDatabaseResource(String databaseId, String resourceId) {
        this.currentDatabase = databaseId;
        this.currentResource = resourceId;
        this.initializeArchiveTree();
    }

    /**
     * If only one archive database exists and database status is {@link DatabaseState#ARCHIVES_LOADED}, redirect to the matching url.
     */
    public void redirectToOnlyDatabase() {
        if (this.databaseState == DatabaseState.ARCHIVES_LOADED && this.archives.size() == 1) {
            ArchiveResource resource = this.archives.keySet().iterator().next();
            String url = PrettyUrlTools.getAbsolutePageUrl("archives2", resource.getDatabaseId(), resource.getResourceId());
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(url);
            } catch (IOException | NullPointerException e) {
                logger.error("Error redirecting to database url {}: {}", url, e.toString());
            }
        }
    }

    public NodeType getNodeType(String name) {
        return this.nodeTypes.stream().filter(node -> node.getName().equals(name)).findAny().orElse(new NodeType("", ""));
    }

    public String loadArchiveForId(String identifier) {
        URI archiveUri = URI.create(DataManager.getInstance().getConfiguration().getBaseXUrl());
        URI requestUri = UriBuilder.fromUri(archiveUri).path("dbname").path(identifier).build();

        try {
            String response = NetTools.getWebContentGET(requestUri.toString());
            Document doc = new SAXBuilder().build(new StringReader(response));
            String database = Optional.ofNullable(doc).map(Document::getRootElement).map(d -> d.getChild("record", null)).map(d -> d.getAttributeValue("database")).orElse("");
            if(StringUtils.isBlank(database)) {
                logger.warn("Error retrieving data base for " + identifier + ": empty or unexcepted response");
                return "archives/"; 
            }
            String filename = doc.getRootElement().getChild("record", null).getAttributeValue("filename");
            this.loadDatabaseResource(BasexEADParser.getIdForName(database), BasexEADParser.getIdForName(filename));
            return "archives/{database}/{filename}/?selected={identifier}#selected"
                    .replace("{database}", BasexEADParser.getIdForName(database))
                    .replace("{filename}", BasexEADParser.getIdForName(filename))
                    .replace("{identifier}", identifier);
        } catch (IOException | HTTPException | JDOMException e) {
            logger.error("Error retrieving data base for " + identifier, e);
            return "archives/";
        }
    }
}
