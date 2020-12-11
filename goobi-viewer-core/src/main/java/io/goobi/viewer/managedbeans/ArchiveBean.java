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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.ClientProtocolException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.monitoring.timer.Time;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.BaseXException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.archives.ArchiveEntry;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.archives.ArchiveTree;
import io.goobi.viewer.model.archives.BasexEADParser;
import io.goobi.viewer.model.viewer.StringPair;

@Named
@ViewScoped
public class ArchiveBean implements Serializable {

    private static final long serialVersionUID = -1755934299534933504L;

    private static final Logger logger = LoggerFactory.getLogger(ArchiveBean.class);

    private ArchiveTree archiveTree;

    private String searchString;

    private DatabaseState databaseState = DatabaseState.NOT_INITIALIZED;

    @Inject
    private PersistentStorageBean storage;

    //    @Inject
    //    private FacesContext context;

    private static enum DatabaseState {
        NOT_INITIALIZED,
        VALID,
        ERROR_NOT_CONFIGURED,
        ERROR_NOT_REACHABLE,
        ERROR_INVALID_FORMAT
    }

    /**
     * Empty constructor.
     */
    public ArchiveBean() {
        // the emptiness inside
    }

    /**
     * 
     */
    @PostConstruct
    public void init() {
        String basexUrl = DataManager.getInstance().getConfiguration().getBaseXUrl();
        String databaseName = DataManager.getInstance().getConfiguration().getBaseXDatabase();
        if (StringUtils.isNoneBlank(basexUrl, databaseName)) {
            BasexEADParser eadParser = new BasexEADParser(basexUrl);
            this.databaseState = loadDatabase(eadParser, databaseName);
        } else {
            this.databaseState = DatabaseState.ERROR_NOT_CONFIGURED;
        }
    }

    DatabaseState loadDatabase(BasexEADParser eadParser, String databaseName) {
        if (eadParser == null) {
            return DatabaseState.NOT_INITIALIZED;
        }

        //        String storageKey = databaseName + "@" + eadParser.getBasexUrl();
        //        if(context.getExternalContext().getSessionMap().containsKey(storageKey)) {
        //            eadParser = new BasexEADParser((BasexEADParser)context.getExternalContext().getSessionMap().containsKey(storageKey));
        //        } else {
        HierarchicalConfiguration baseXMetadataConfig = DataManager.getInstance().getConfiguration().getBaseXMetadataConfig();
        try {
            Document databaseDoc = eadParser.retrieveDatabaseDocument(databaseName);

            ArchiveEntry rootElement = eadParser.loadDatabase(databaseName, baseXMetadataConfig, databaseDoc);
            this.archiveTree = loadTree(rootElement);
            this.archiveTree.setTrueRootElement(rootElement);
            logger.info("Loaded EAD database: {}", databaseName);
            return DatabaseState.VALID;
        } catch (IOException | HTTPException e) {
            logger.error("Error retrieving database {} from {}", databaseName, eadParser.getBasexUrl());
            return DatabaseState.ERROR_NOT_REACHABLE;
        } catch (JDOMException | ConfigurationException e) {
            logger.error("Error reading database {} from {}", databaseName, eadParser.getBasexUrl());
            return DatabaseState.ERROR_INVALID_FORMAT;
        }
        //            context.getExternalContext().getSessionMap().put(storageKey, eadParser);
        //        }

    }

    /**
     * @param eadParser
     * @param databaseName
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     * @throws IllegalStateException
     * @throws JDOMException
     * @deprecated Storing database in application seems ineffective since verifying that database is current takes about as much time as retriving
     *             the complete database
     */
    @Deprecated
    public void loadDatabaseAndStoreInApplicationScope(BasexEADParser eadParser, String databaseName)
            throws ClientProtocolException, IOException, HTTPException, IllegalStateException, JDOMException {
        try (Time t = DataManager.getInstance().getTiming().takeTime("loadDatabase")) {
            Document databaseDoc = null;
            String storageKey = databaseName + "@" + eadParser.getBasexUrl();
            List<ArchiveResource> dbs;
            try (Time t1 = DataManager.getInstance().getTiming().takeTime("getPossibleDatabases")) {
                dbs = eadParser.getPossibleDatabases();
            }
            ArchiveResource db = dbs.stream().filter(res -> res.getCombinedName().equals(databaseName)).findAny().orElse(null);
            if (db == null) {
                throw new IllegalStateException("Configured default database not found in " + eadParser.getBasexUrl());
            } else if (storage.contains(storageKey) && !storage.olderThan(storageKey, db.lastModified)) {
                databaseDoc = (Document) storage.get(storageKey);
            } else {
                try (Time t2 = DataManager.getInstance().getTiming().takeTime("retrieveDatabaseDocument")) {
                    databaseDoc = eadParser.retrieveDatabaseDocument(databaseName);
                    storage.put(storageKey, databaseDoc);
                }
            }
            HierarchicalConfiguration baseXMetadataConfig = DataManager.getInstance().getConfiguration().getBaseXMetadataConfig();
            try (Time t3 = DataManager.getInstance().getTiming().takeTime("eadParser.loadDatabase")) {
                eadParser.loadDatabase(databaseName, baseXMetadataConfig, databaseDoc);
            } catch (ConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @return actual root element of the document, even if it's not in the displayed tree
     */
    public ArchiveEntry getTrueRoot() {
        if (archiveTree == null) {
            return null;
        }

        return archiveTree.getRootElement();
    }

    /**
     * 
     * @return
     * @throws BaseXException
     */
    public ArchiveTree getArchiveTree() throws BaseXException {
        return archiveTree;
    }

    /**
     * @param rootElement
     * @return
     */
    ArchiveTree loadTree(ArchiveEntry rootElement) {
        if (rootElement == null) {
            logger.trace("Root not found, cannot load tree.");
            return null;
        }

        ArchiveTree ret = new ArchiveTree();
        ret.generate(rootElement);
        if (ret.getSelectedEntry() == null) {
            ret.setSelectedEntry(rootElement);
        }
        // This should happen before the tree is expanded to the selected entry, otherwise the collapse level will be reset
        ret.getTreeView();

        return ret;
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
        if (archiveTree == null) {
            return;
        }
        synchronized (archiveTree) {
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
        if (archiveTree == null) {
            return;
        }

        synchronized (archiveTree) {
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

        if (archiveTree == null) {
            logger.error("Archive not loaded");
            return Collections.emptyList();
        }

        ArchiveEntry entry = archiveTree.getEntryById(identifier);
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
        if (entry == null || archiveTree == null) {
            return "";
        }

        archiveTree.setSelectedEntry(entry);

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
        if (!isDatabaseValid()) {
            logger.warn("Archive not loaded, cannot search.");
            return;
        }

        if (StringUtils.isEmpty(searchString)) {
            archiveTree.resetSearch();
            archiveTree.resetCollapseLevel(archiveTree.getRootElement(), ArchiveTree.defaultCollapseLevel);
            return;
        }

        archiveTree.search(searchString);
        List<ArchiveEntry> results = archiveTree.getFlatEntryList();
        if (results == null || results.isEmpty()) {
            return;
        }
        logger.trace("result entries: {}", results.size());

        if (resetSelectedEntry) {
            setSelectedEntryId(null);
        }
        archiveTree.collapseAll(collapseAll);
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
        this.searchString = searchString;
    }

    /**
     * 
     * @return
     */
    public String getSelectedEntryId() {
        if (archiveTree == null || archiveTree.getSelectedEntry() == null) {
            return "-";
        }

        return archiveTree.getSelectedEntry().getId();
    }

    /**
     * Setter for the URL parameter. Loads the entry that has the given ID. Loads the tree, if this is a new sessions.
     * 
     * @param id Entry ID
     * @throws BaseXException
     */
    public void setSelectedEntryId(String id) throws BaseXException {
        logger.trace("setSelectedEntryId: {}", id);
        if (!isDatabaseValid()) {
            return;
        }

        // Select root element if no ID given
        if (StringUtils.isBlank(id)) {
            id = "-";
        }
        if ("-".equals(id)) {
            archiveTree.setSelectedEntry(null);
            return;
        }
        // Requested entry is already selected
        if (archiveTree.getSelectedEntry() != null && archiveTree.getSelectedEntry().getId().equals(id)) {
            return;
        }

        // Find entry with given ID in the tree
        ArchiveEntry result = archiveTree.getEntryById(id);
        if (result != null) {
            archiveTree.setSelectedEntry(result);
            result.expandUp();
        } else {
            logger.debug("Entry not found: {}", id);
            archiveTree.setSelectedEntry(archiveTree.getRootElement());
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
        if (archiveTree == null) {
            return null;
        }

        return Optional.ofNullable(archiveTree.getSelectedEntry()).orElse(archiveTree.getRootElement());
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
            String id = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.ARCHIVE_ENTRY_ID);
            if (id.equals(entryId)) {
                if (iter.hasNext()) {
                    String nextId = SolrSearchIndex.getSingleFieldStringValue(iter.next(), SolrConstants.ARCHIVE_ENTRY_ID);
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

    public boolean isDatabaseValid() {
        return DatabaseState.VALID.equals(this.databaseState);
    }

}
