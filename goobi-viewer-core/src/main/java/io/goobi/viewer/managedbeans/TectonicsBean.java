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
import io.goobi.viewer.model.ead.BasexEADParser;
import io.goobi.viewer.model.ead.EADTree;
import io.goobi.viewer.model.ead.EadEntry;
import io.goobi.viewer.model.ead.EadResource;
import io.goobi.viewer.model.viewer.StringPair;

@Named
@ViewScoped
public class TectonicsBean implements Serializable {

    private static final long serialVersionUID = -1755934299534933504L;

    private static final Logger logger = LoggerFactory.getLogger(TectonicsBean.class);

    private static final Object lock = new Object();

    private BasexEADParser eadParser = null;

    private EADTree tectonicsTree;

    private String searchString;

    @Inject
    private PersistentStorageBean storage;

    //    @Inject
    //    private FacesContext context;

    /**
     * Empty constructor.
     */
    public TectonicsBean() {
        // the emptiness inside
    }

    /**
     * 
     */
    @PostConstruct
    public void init() {
        try {
            this.eadParser = new BasexEADParser(DataManager.getInstance().getConfiguration().getBaseXUrl());
            loadDatabase(DataManager.getInstance().getConfiguration().getBaseXDatabase());
        } catch (IOException | HTTPException | ConfigurationException e) {
            logger.error("Error initializing database: {}", e.getMessage());
        }

    }

    public void loadDatabase(String databaseName) throws ClientProtocolException, IOException, HTTPException {

        //        String storageKey = databaseName + "@" + eadParser.getBasexUrl();
        //        if(context.getExternalContext().getSessionMap().containsKey(storageKey)) {
        //            eadParser = new BasexEADParser((BasexEADParser)context.getExternalContext().getSessionMap().containsKey(storageKey));
        //        } else {
        Document databaseDoc = eadParser.retrieveDatabaseDocument(databaseName);
        HierarchicalConfiguration baseXMetadataConfig = DataManager.getInstance().getConfiguration().getBaseXMetadataConfig();
        eadParser.loadDatabase(databaseName, baseXMetadataConfig, databaseDoc);
        //            context.getExternalContext().getSessionMap().put(storageKey, eadParser);
        //        }

    }

    /**
     * @param databaseName
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     */
    public void loadDatabaseAndStoreInApplicationScope(String databaseName) throws ClientProtocolException, IOException, HTTPException {
        try (Time t = DataManager.getInstance().getTiming().takeTime("loadDatabase")) {
            Document databaseDoc = null;
            String storageKey = databaseName + "@" + eadParser.getBasexUrl();
            List<EadResource> dbs;
            try (Time t1 = DataManager.getInstance().getTiming().takeTime("getPossibleDatabases")) {
                dbs = eadParser.getPossibleDatabases();
            }
            EadResource db = dbs.stream().filter(res -> res.getCombinedName().equals(databaseName)).findAny().orElse(null);
            if (db == null) {
                throw new IllegalStateException("Configured default database not found in " + this.eadParser.getBasexUrl());
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
            }
        }
    }

    /**
     * 
     * @return actual root element of the document, even if it's not in the displayed tree
     */
    public EadEntry getTrueRoot() {
        if (eadParser == null || !eadParser.isDatabaseLoaded()) {
            return null;
        }

        return eadParser.getRootElement();
    }

    /**
     * 
     * @return
     * @throws BaseXException
     */
    public EADTree getTectonicsTree() throws BaseXException {
        // logger.trace("getTectonicsTree");
        if (eadParser == null || !eadParser.isDatabaseLoaded()) {
            throw new BaseXException("No BaseX connection");
        }

        EADTree h = tectonicsTree;
        if (h == null) {
            synchronized (lock) {
                // Another thread might have initialized hierarchy by now
                h = tectonicsTree;
                if (h == null) {
                    h = generateHierarchy();
                    tectonicsTree = h;
                }
            }
        }

        return tectonicsTree;
    }

    /**
     * 
     * @return
     */
    EADTree generateHierarchy() {
        if (eadParser == null || !eadParser.isDatabaseLoaded()) {
            return null;
        }

        EADTree ret = new EADTree();
        ret.generate(eadParser.getRootElement());
        if (ret.getSelectedEntry() == null) {
            ret.setSelectedEntry(eadParser.getRootElement());
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
    public void expandEntry(EadEntry entry) {
        logger.trace("expandEntry: {}", entry);
        if (tectonicsTree == null) {
            return;
        }
        synchronized (tectonicsTree) {
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
    public void collapseEntry(EadEntry entry) {
        logger.trace("collapseEntry: {}", entry);
        if (tectonicsTree == null) {
            return;
        }

        synchronized (tectonicsTree) {
            entry.collapse();
        }
    }

    /**
     * Returns the entry hierarchy from the root down to the entry with the given identifier.
     * 
     * @param identifier Entry identifier
     * @param List of entries An empty list if the identified node has no anchestors or doesn't exist
     */
    public List<EadEntry> getTectonicsHierarchyForIdentifier(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyList();
        }

        if (eadParser == null) {
            logger.error("EAD parser not intialized");
            return Collections.emptyList();
        }

        EadEntry entry = eadParser.getEntryById(identifier);
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
    public String selectEntryAction(EadEntry entry) {
        if (entry == null || tectonicsTree == null) {
            return "";
        }

        tectonicsTree.setSelectedEntry(entry);

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
        if (eadParser == null || !eadParser.isDatabaseLoaded() || tectonicsTree == null) {
            logger.warn("Tree not loaded, cannot search.");
            return;
        }

        if (StringUtils.isEmpty(searchString)) {
            eadParser.resetSearch();
            tectonicsTree.resetCollapseLevel(tectonicsTree.getRootElement(), EADTree.defaultCollapseLevel);
            return;
        }

        eadParser.search(searchString);
        List<EadEntry> results = eadParser.getFlatEntryList();
        if (results == null || results.isEmpty()) {
            return;
        }
        logger.trace("result entries: {}", results.size());

        if (resetSelectedEntry) {
            setSelectedEntryId(null);
        }
        tectonicsTree.collapseAll(collapseAll);
        for (EadEntry entry : results) {
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
        if (tectonicsTree == null || tectonicsTree.getSelectedEntry() == null) {
            return "-";
        }

        return tectonicsTree.getSelectedEntry().getId();
    }

    /**
     * Setter for the URL parameter. Loads the entry that has the given ID. Loads the tree, if this is a new sessions.
     * 
     * @param id Entry ID
     * @throws BaseXException 
     */
    public void setSelectedEntryId(String id) throws BaseXException {
        logger.trace("setSelectedEntryId: {}", id);

        // getTectonicsTree() will also load the tree, if not yet loaded
        if (getTectonicsTree() == null || eadParser == null) {
            return;
        }
        // Select root element if no ID given
        if (StringUtils.isBlank(id)) {
            id = "-";
        }
        if ("-".equals(id)) {
            tectonicsTree.setSelectedEntry(null);
            return;
        }
        // Requested entry is already selected
        if (tectonicsTree.getSelectedEntry() != null && tectonicsTree.getSelectedEntry().getId().equals(id)) {
            return;
        }

        // Find entry with given ID in the tree
        EadEntry result = eadParser.getEntryById(id);
        if (result != null) {
            tectonicsTree.setSelectedEntry(result);
            result.expandUp();
        } else {
            logger.debug("Entry not found: {}", id);
            tectonicsTree.setSelectedEntry(eadParser.getRootElement());
        }

    }

    public boolean isSearchActive() {
        return StringUtils.isNotBlank(searchString);
    }

    /**
     * 
     * @return the {@link EadEntry} to display in the metadata section of the archives view. Either {@link EADTree#getSelectedEntry()} or
     *         {@link EADTree#getRootElement()} if the former is null
     */
    public EadEntry getDisplayEntry() {
        return Optional.ofNullable(tectonicsTree.getSelectedEntry()).orElse(tectonicsTree.getRootElement());
    }

    /**
     * In the list of tectonic document search hits, find the id of the entry just before the given one
     * 
     * @param entry
     * @param sortOrder 'asc' to get the preceding entry, 'desc' to get the succeeding one
     * @return the neighouring entry id if it exists
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Pair<Optional<String>, Optional<String>> findIndexedNeighbours(String tectonicsId)
            throws PresentationException, IndexUnreachableException {
        String query = SolrConstants.TECTONICS_ID + ":*";
        List<StringPair> sortFields = Collections.singletonList(new StringPair(SolrConstants.PI, "asc"));
        List<String> fieldList = Arrays.asList(SolrConstants.PI, SolrConstants.TECTONICS_ID);

        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, SolrSearchIndex.MAX_HITS, sortFields, fieldList);
        Optional<String> prev = Optional.empty();
        Optional<String> next = Optional.empty();

        ListIterator<SolrDocument> iter = docs.listIterator();
        while (iter.hasNext()) {
            SolrDocument doc = iter.next();
            String id = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.TECTONICS_ID);
            if (id.equals(tectonicsId)) {
                if (iter.hasNext()) {
                    String nextId = SolrSearchIndex.getSingleFieldStringValue(iter.next(), SolrConstants.TECTONICS_ID);
                    next = Optional.of(nextId);
                }
                break;
            }
            prev = Optional.of(id);
        }
        return Pair.of(prev, next);
    }

}
