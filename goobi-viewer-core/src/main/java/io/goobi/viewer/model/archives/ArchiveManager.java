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
package io.goobi.viewer.model.archives;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.ArchiveConnectionException;
import io.goobi.viewer.exceptions.ArchiveParseException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Loads and holds archive tree models. This class should have an application wide scope
 *
 * @author florian
 *
 */
public class ArchiveManager implements Serializable {

    private static final long serialVersionUID = -2417652614144759711L;

    private static final Logger logger = LogManager.getLogger(ArchiveManager.class);

    private DatabaseState databaseState = DatabaseState.NOT_INITIALIZED;

    private Map<ArchiveResource, ArchiveTree> archives = new HashMap<>();

    private final Map<String, NodeType> nodeTypes;

    private final ArchiveParser eadParser;

    public enum DatabaseState {
        /**
         * State before the first query to the server
         */
        NOT_INITIALIZED,
        /**
         * Archive names are queried from the server
         */
        ARCHIVES_LOADED,
        /**
         * State only applicable to a single database if it was successfully loaded into memory
         */
        ARCHIVE_TREE_LOADED,
        /**
         * url call not returned
         */
        ERROR_NOT_REACHABLE,
        /**
         * State only applicable to a single database if loading the database failed because the server answer could not be interpreted
         */
        ERROR_INVALID_FORMAT,
        /**
         * State indicating that a database could not be parsed due to errors in the configuration for building the tree entries
         */
        ERROR_INVALID_CONFIGURATION;
    }

    /**
     * 
     * @param archiveNodeTypes
     */
    public ArchiveManager(Map<String, String> archiveNodeTypes) {
        ArchiveParser parser = null;
        try {
            parser = new SolrEADParser();
            initArchives(parser);
            this.databaseState = DatabaseState.ARCHIVES_LOADED;
        } catch (PresentationException | IndexUnreachableException | IOException | HTTPException e) {
            logger.error("Failed to retrieve database names from Solr: {}", e.toString());
            this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
        }
        this.eadParser = parser;
        this.nodeTypes = loadNodeTypes(archiveNodeTypes);
    }

    /**
     * 
     * @param eadParser
     * @param archiveNodeTypes
     */
    public ArchiveManager(ArchiveParser eadParser, Map<String, String> archiveNodeTypes) {
        try {
            initArchives(eadParser);
            this.databaseState = DatabaseState.ARCHIVES_LOADED;
        } catch (IOException | HTTPException | PresentationException | IndexUnreachableException e) {
            logger.error("Failed to retrieve database names from '{}': {}", eadParser, e.toString());
            this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
        }
        this.eadParser = eadParser;
        this.nodeTypes = loadNodeTypes(archiveNodeTypes);
    }

    /**
     * Queries the list of databases from the server and updated the internal database list from it.
     *
     * @param eadParser
     * @return true if the internal list of databases was updated, either because a database was outdated, didn't exist before or doesn't exist
     *         anymore
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private boolean initArchives(ArchiveParser eadParser) throws IOException, HTTPException, PresentationException, IndexUnreachableException {
        logger.trace("initArchives");
        if (eadParser == null) {
            logger.error("eadParser is null; cannot init archives");
            return false;
        }
        //initialize archives with 'null' archive tree values
        Map<ArchiveResource, ArchiveTree> cachedDatabases = this.archives;
        this.archives = new HashMap<>();
        boolean updated = false;
        for (ArchiveResource db : eadParser.getPossibleDatabases()) {
            if (db == null) {
                continue;
            }
            ArchiveResource cachedResource =
                    cachedDatabases.keySet().stream().filter(res -> res.getCombinedId().equals(db.getCombinedId())).findAny().orElse(null);
            ArchiveTree cachedTree = cachedResource != null ? cachedDatabases.get(cachedResource) : null;
            if (cachedTree == null || cachedResource == null) {
                logger.trace("Archive {} is not yet loaded.", db.getResourceName());
                this.archives.put(db, null);
                updated = true;
            } else if (isOutdated(cachedResource, db)) {
                logger.trace("Archive {} is outdated, (re)loading...", db.getResourceName());
                this.archives.put(db, null);
                updated = true;
            } else {
                this.archives.put(cachedResource, cachedTree);
                cachedDatabases.remove(cachedResource);
            }
        }
        // cached databases that are included in the response are removed from the cachedDatabases list.
        // If it is still not empty at this point, databases were removed
        updated = updated || !cachedDatabases.isEmpty();
        return updated;
    }

    /**
     * 
     * @param cachedResource
     * @param currentResource
     * @return true if cached resource is out of date; false otherwise
     */
    private static boolean isOutdated(ArchiveResource cachedResource, ArchiveResource currentResource) {
        if (cachedResource == null) {
            return true;
        } else if (currentResource == null) {
            return true;
        } else {
            logger.trace("Loaded resource date: {}", currentResource.getModifiedDate());
            logger.trace("Cached resource date: {}", cachedResource.getModifiedDate());
            return currentResource.getModifiedDate().isAfter(cachedResource.getModifiedDate());
        }
    }

    /**
     * 
     * @param archiveId
     * @param resourceId
     * @return {@link ArchiveTree}
     * @throws IllegalStateException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should load ArchiveTree correctly
     */
    public ArchiveTree getArchiveTree(String archiveId, String resourceId)
            throws IllegalStateException, PresentationException, IndexUnreachableException {
        logger.trace("getArchiveTree: {}", archiveId);
        ArchiveResource resource = getArchive(archiveId, resourceId);
        this.initializeArchiveTree(resource);
        return this.archives.get(resource);
    }

    /**
     * 
     * @param archiveName
     * @return ArchiveResource
     */
    public ArchiveResource getArchiveResource(String archiveName) {
        return this.archives.keySet().stream().filter(db -> db.getCombinedId().equals(archiveName)).findAny().orElse(null);
    }

    /**
     * 
     * @param name
     * @return {@link NodeType}
     */
    public NodeType getNodeType(String name) {
        return this.nodeTypes.getOrDefault(name, new NodeType("", ""));
    }

    /**
     * If only one archive database exists and database status is {@link DatabaseState#ARCHIVES_LOADED}, redirect to the matching url.
     * 
     * @return Optional<ArchiveResource>
     */
    public Optional<ArchiveResource> getOnlyDatabaseResource() {
        if (this.databaseState == DatabaseState.ARCHIVES_LOADED && this.archives.size() == 1) {
            return this.archives.keySet().stream().findFirst();
        }

        return Optional.empty();
    }

    /**
     * 
     * @param identifier
     * @return ArchiveResource
     * @throws IllegalStateException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public ArchiveResource loadArchiveForEntry(String identifier) throws IllegalStateException, PresentationException, IndexUnreachableException {
        ArchiveResource resource = getArchiveForEntrySolr(identifier);
        this.initializeArchiveTree(resource);
        return resource;
    }

    /**
     * 
     * @param resource
     * @param entryIdentifier
     * @return Constructed URL
     */
    public String getArchiveUrl(ArchiveResource resource, String entryIdentifier) {
        if (resource == null || StringUtils.isBlank(entryIdentifier)) {
            return "archives/";
        }

        return "archives/{database}/{filename}/?selected={identifier}#selected"
                .replace("{database}", resource.getDatabaseId())
                .replace("{filename}", resource.getResourceId())
                .replace("{identifier}", entryIdentifier);
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    /**
     * 
     * @return List<ArchiveResource>
     */
    public List<ArchiveResource> getDatabases() {
        List<ArchiveResource> databases = new ArrayList<>(this.archives.keySet());
        databases.sort((db1, db2) -> db1.getCombinedName().compareTo(db2.getCombinedName()));
        return databases;
    }

    /**
     * In the list of archive document search hits, find the id of the entry just before the given one
     *
     * @param entryId
     * @return the neighboring entry id if it exists
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Pair<Optional<String>, Optional<String>> findIndexedNeighbours(String entryId) throws PresentationException, IndexUnreachableException {
        String query = "+" + SolrConstants.EAD_NODE_ID + ":* +" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name();
        List<StringPair> sortFields = Collections.singletonList(new StringPair(SolrConstants.PI, "asc"));
        List<String> fieldList = Arrays.asList(SolrConstants.PI, SolrConstants.EAD_NODE_ID);

        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, SolrSearchIndex.MAX_HITS, sortFields, fieldList);
        Optional<String> prev = Optional.empty();
        Optional<String> next = Optional.empty();

        ListIterator<SolrDocument> iter = docs.listIterator();
        while (iter.hasNext()) {
            SolrDocument doc = iter.next();
            String id = SolrTools.getSingleFieldStringValue(doc, SolrConstants.EAD_NODE_ID);
            if (id.equals(entryId)) {
                if (iter.hasNext()) {
                    String nextId = SolrTools.getSingleFieldStringValue(iter.next(), SolrConstants.EAD_NODE_ID);
                    next = Optional.of(nextId);
                }
                break;
            }
            prev = Optional.of(id);
        }
        return Pair.of(prev, next);
    }

    /**
     * Returns the entry hierarchy from the root down to the entry with the given identifier.
     *
     * @param resource
     * @param identifier Entry identifier
     * @return List of entries An empty list if the identified node has no ancestors or doesn't exist
     */
    public List<ArchiveEntry> getArchiveHierarchyForIdentifier(ArchiveResource resource, String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyList();
        } else if (resource == null) {
            logger.error("Archive not loaded");
            return Collections.emptyList();
        }

        ArchiveTree tree = archives.get(resource);
        if (tree != null) {
            ArchiveEntry entry = tree.getEntryById(identifier);
            ArchiveEntry trueRoot = getTrueRoot(archives.get(resource));
            if (entry == null) {
                if (trueRoot == null) {
                    Collections.emptyList();
                }
                return Collections.singletonList(trueRoot);
            } else if (trueRoot != null && (trueRoot.equals(entry) || trueRoot.equals(entry.getParentNode()))) {
                return Collections.singletonList(entry);
            } else {
                return entry.getAncestors(false).stream().skip(1).toList();
            }
        }

        return Collections.emptyList();
    }

    public ArchiveResource getArchive(String databaseId, String resourceId) {
        logger.trace("getArchive: {}, {}", databaseId, resourceId);
        if (StringUtils.isNoneBlank(databaseId, resourceId)) {
            return this.archives.keySet()
                    .stream()
                    // .peek(a -> System.out.println("Archive " + a.getDatabaseId() + " - " + a.getResourceId()))
                    .filter(a -> a.getDatabaseId().equals(databaseId))
                    .filter(a -> a.getResourceId().equals(resourceId))
                    .findAny()
                    .orElse(null);
        }

        return null;
    }

    /**
     * 
     * @param identifier EAD node ID
     * @return {@link ArchiveResource}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private ArchiveResource getArchiveForEntrySolr(String identifier) throws PresentationException, IndexUnreachableException {
        // logger.trace("getArchiveForEntrySolr: {}", identifier); //NOSONAR Debug
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc("+" + SolrConstants.EAD_NODE_ID + ":\"" + identifier + "\" +" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name(),
                        Collections.singletonList(SolrConstants.PI_TOPSTRUCT));
        if (doc != null) {
            String pi = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
            return getArchive(SolrEADParser.DATABASE_NAME, pi);
        }

        return null;
    }

    /**
     * @param tree
     * @return actual root element of the document, even if it's not in the displayed tree
     */
    private static ArchiveEntry getTrueRoot(ArchiveTree tree) {
        if (tree == null) {
            return null;
        }

        return tree.getRootElement();
    }

    /**
     * 
     * @param resource
     * @throws IllegalStateException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private void initializeArchiveTree(ArchiveResource resource) throws IllegalStateException, PresentationException, IndexUnreachableException {
        if (resource != null) {
            try {
                if (this.archives.get(resource) == null || isOutdated(resource)) {
                    logger.trace("Archive {} is not yet loaded or outdated, (re)loading...", resource.getResourceName());
                    ArchiveTree archiveTree = loadDatabase(eadParser, resource);
                    if (archiveTree != null) {
                        this.archives.put(resource, archiveTree);
                    }
                }
            } catch (IOException | HTTPException e) {
                this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
                throw new ArchiveConnectionException("Error retrieving database {} from {}", resource.getCombinedName(), eadParser.getUrl(), e);
            } catch (JDOMException e) {
                this.databaseState = DatabaseState.ERROR_INVALID_FORMAT;
                throw new ArchiveParseException("Error reading database {} from {}", resource.getCombinedName(), eadParser.getUrl(), e);
            }
        }
    }

    /**
     * Check if the given resource is outdated compared to the last updated date from the server
     *
     * @param resource
     * @return true if the resource in the database is newer than the given one
     * @throws IOException if the the database server is not reachable
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private boolean isOutdated(ArchiveResource resource) throws IOException, PresentationException, IndexUnreachableException {
        try {
            List<ArchiveResource> resources = this.eadParser.getPossibleDatabases();
            ArchiveResource externalResource =
                    resources.stream().filter(extResource -> extResource.getCombinedId().equals(resource.getCombinedId())).findAny().orElse(null);
            if (externalResource != null) {
                return externalResource.getModifiedDate().isAfter(resource.getModifiedDate());
            }
            throw new PresentationException("Resource " + resource.getCombinedName() + " not found on server " + this.eadParser.getUrl());
        } catch (HTTPException e) {
            throw new IOException("Solr server cannot be reached: " + e.toString());
        }
    }

    /**
     * 
     * @param eadParser
     * @param archive
     * @return {@link ArchiveTree}
     * @throws IllegalStateException
     * @throws IOException
     * @throws HTTPException
     * @throws JDOMException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    ArchiveTree loadDatabase(ArchiveParser eadParser, ArchiveResource archive)
            throws IllegalStateException, IOException, HTTPException, JDOMException, PresentationException, IndexUnreachableException {
        ArchiveEntry rootElement = eadParser.loadDatabase(archive, DataManager.getInstance().getConfiguration().getArchivesLazyLoadingThreshold());
        if (rootElement != null) {
            logger.info("Loaded EAD database: {}", archive.getCombinedName());
            return loadTree(rootElement);
        }

        logger.error("Failed to load EAD database: {}", archive.getCombinedName());
        return null;
    }

    /**
     * 
     * @param rootElement
     * @return {@link ArchiveTree}
     * @should load tree correctly
     */
    static ArchiveTree loadTree(ArchiveEntry rootElement) {
        ArchiveTree ret = new ArchiveTree();
        ret.update(rootElement);

        return ret;
    }

    /**
     * 
     * @param archiveNodeTypes
     * @return Map<String, NodeType>
     */
    private static Map<String, NodeType> loadNodeTypes(Map<String, String> archiveNodeTypes) {
        if (archiveNodeTypes != null) {
            Map<String, NodeType> ret = new HashMap<>(archiveNodeTypes.size());
            for (Entry<String, String> entry : archiveNodeTypes.entrySet()) {
                ret.put(entry.getKey(), new NodeType(entry.getKey(), entry.getValue()));
            }

            return ret;
        }

        return Collections.emptyMap();
    }

    /**
     * Checks the list of ead archives for updates. An update occurs if either the "lastModifiedDate" of an archive has changed since the last
     * request, or if an archive was added or removed. In these cases, the list of records associated with an archive entry is updated as well
     */
    public void updateArchiveList() {
        logger.trace("updateArchiveList"); //NOSONAR Debug
        try {

            if (this.initArchives(eadParser)) {
                this.eadParser.updateAssociatedRecordMap();
                this.databaseState = DatabaseState.ARCHIVES_LOADED;
            }
        } catch (IOException | HTTPException e) {
            logger.error("Failed to retrieve database names from '{}': {}", eadParser != null ? eadParser.getUrl() : "null", e.toString());
            this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Failed to retrieve associated records from SOLR: {}", e.toString());
            this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
        }
    }

    public boolean isInErrorState() {
        return this.databaseState == DatabaseState.ERROR_INVALID_CONFIGURATION
                || this.databaseState == DatabaseState.ERROR_INVALID_FORMAT
                || this.databaseState == DatabaseState.ERROR_NOT_REACHABLE;
    }

    /**
     * @return the eadParser
     */
    public ArchiveParser getEadParser() {
        return eadParser;
    }
}
