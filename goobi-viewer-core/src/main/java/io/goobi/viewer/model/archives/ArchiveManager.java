package io.goobi.viewer.model.archives;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.BaseXException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Loads and holds archive tree models. This class should have an application wide scope
 * 
 * @author florian
 *
 */
public class ArchiveManager {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveManager.class);

    private DatabaseState databaseState = DatabaseState.NOT_INITIALIZED;

    private final Map<ArchiveResource, ArchiveTree> archives = new HashMap<>();

    private final List<NodeType> nodeTypes;

    private final BasexEADParser eadParser;

    public static enum DatabaseState {
        /**
         * State before the first query to the basex server
         */
        NOT_INITIALIZED,
        /**
         * Archive names are queried from the basex server
         */
        ARCHIVES_LOADED,
        /**
         * State only applicable to a single database if it was successfully loaded into memory
         */
        ARCHIVE_TREE_LOADED,
        /**
         * No basex url configured in config-viewer
         */
        ERROR_NOT_CONFIGURED,
        /**
         * basex url call not returned
         */
        ERROR_NOT_REACHABLE,
        /**
         * State only applicable to a single database if loading the database failed because the basex server answer could not be interpreted
         */
        ERROR_INVALID_FORMAT
    }

    public ArchiveManager(String basexUrl, Map<String, String> archiveNodeTypes, SolrSearchIndex searchIndex) {
        BasexEADParser eadParser = null;
        if (StringUtils.isNotBlank(basexUrl)) {
            try {
                eadParser = new BasexEADParser(basexUrl, searchIndex);
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
        this.nodeTypes = loadNodeTypes(archiveNodeTypes);
    }

    public ArchiveManager(BasexEADParser eadParser, Map<String, String> archiveNodeTypes) {
        try {
            //initialize archives with 'null' archive tree values
            List<ArchiveResource> databases = eadParser.getPossibleDatabases();
            for (ArchiveResource db : databases) {
                this.archives.put(db, null);
            }
            //this.archives = eadParser.getPossibleDatabases().stream().collect(Collectors.toMap(db -> db, db -> null));
            this.databaseState = DatabaseState.ARCHIVES_LOADED;
        } catch (IOException | HTTPException e) {
            logger.error("Failed to retrieve database names from '{}': {}", eadParser.getBasexUrl(), e.toString());
            this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
        }
        this.eadParser = eadParser;
        this.nodeTypes = loadNodeTypes(archiveNodeTypes);
    }

    public ArchiveTree getArchiveTree(String archiveId, String resourceId) {
        ArchiveResource resource = getArchive(archiveId, resourceId);
        this.initializeArchiveTree(resource);
        return this.archives.get(resource);
    }

    public ArchiveResource getArchiveResource(String archiveName) {
        ArchiveResource database = this.archives.keySet().stream().filter(db -> db.getCombinedId().equals(archiveName)).findAny().orElse(null);
        return database;
    }

    public NodeType getNodeType(String name) {
        return this.nodeTypes.stream().filter(node -> node.getName().equals(name)).findAny().orElse(new NodeType("", ""));
    }

    /**
     * If only one archive database exists and database status is {@link DatabaseState#ARCHIVES_LOADED}, redirect to the matching url.
     */
    public Optional<ArchiveResource> getOnlyDatabaseResource() {
        if (this.databaseState == DatabaseState.ARCHIVES_LOADED && this.archives.size() == 1) {
            return this.archives.keySet().stream().findFirst();
        } else {
            return Optional.empty();
        }
    }

    public ArchiveResource loadArchiveForEntry(String identifier) {
        ArchiveResource resource = getArchiveForEntry(identifier);
        this.initializeArchiveTree(resource);
        return resource;
    }

    public String getArchiveUrl(ArchiveResource resource, String entryIdentifier) {
        if (resource == null || StringUtils.isBlank(entryIdentifier)) {
            return "archives/";
        } else {
            return "archives/{database}/{filename}/?selected={identifier}#selected"
                    .replace("{database}", resource.getDatabaseId())
                    .replace("{filename}", resource.getResourceId())
                    .replace("{identifier}", entryIdentifier);
        }
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    public List<ArchiveResource> getDatabases() {
        List<ArchiveResource> databases = new ArrayList<>(this.archives.keySet());
        databases.sort((db1, db2) -> db1.getCombinedName().compareTo(db2.getCombinedName()));
        return databases;
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
     * Returns the entry hierarchy from the root down to the entry with the given identifier.
     * 
     * @param identifier Entry identifier
     * @param List of entries An empty list if the identified node has no ancestors or doesn't exist
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
            if (entry == null) {
                //            return Collections.emptyList();
                return Collections.singletonList(getTrueRoot(archives.get(resource)));
            } else if (getTrueRoot(archives.get(resource)).equals(entry) || getTrueRoot(archives.get(resource)).equals(entry.getParentNode())) {
                return Collections.singletonList(entry);
            } else {
                return entry.getAncestors(false).stream().skip(1).collect(Collectors.toList());
            }
        } else {
            return Collections.emptyList();
        }
    }

    public ArchiveResource getArchive(String databaseId, String resourceId) {
        if (StringUtils.isNoneBlank(databaseId, resourceId)) {
            ArchiveResource archive = this.archives.keySet()
                    .stream()
                    //                    .peek(a -> System.out.println("Archive " + a.getDatabaseId() + " - " + a.getResourceId()))
                    .filter(a -> a.getDatabaseId().equals(databaseId))
                    .filter(a -> a.getResourceId().equals(resourceId))
                    .findAny()
                    .orElse(null);
            return archive;
        } else {
            return null;
        }
    }

    private ArchiveResource getArchiveForEntry(String identifier) {
        URI archiveUri = URI.create(DataManager.getInstance().getConfiguration().getBaseXUrl());
        URI requestUri = UriBuilder.fromUri(archiveUri).path("dbname").path(identifier).build();

        try {
            String response = NetTools.getWebContentGET(requestUri.toString());
            Document doc = new SAXBuilder().build(new StringReader(response));
            String database = Optional.ofNullable(doc)
                    .map(Document::getRootElement)
                    .map(d -> d.getChild("record", null))
                    .map(d -> d.getAttributeValue("database"))
                    .orElse("");
            if (StringUtils.isBlank(database)) {
                logger.warn("Error retrieving data base for " + identifier + ": empty or unexcepted response");
                return null;
            }
            String filename = doc.getRootElement().getChild("record", null).getAttributeValue("filename");
            String archiveId = BasexEADParser.getIdForName(database);
            String resourceId = BasexEADParser.getIdForName(filename);
            return getArchive(database, resourceId);
        } catch (IOException | HTTPException | JDOMException e) {
            logger.error("Error retrieving data base for " + identifier, e);
            return null;
        }
    }

    /**
     * 
     * @return actual root element of the document, even if it's not in the displayed tree
     */
    private ArchiveEntry getTrueRoot(ArchiveTree tree) {
        if (tree == null) {
            return null;
        }

        return tree.getRootElement();
    }

    private void initializeArchiveTree(ArchiveResource resource) {

        if (resource != null) {
            try {
                if (this.archives.get(resource) == null || isOutdated(resource)) {
                    ArchiveTree archiveTree = loadDatabase(eadParser, resource);
                    if (archiveTree != null) {
                        this.archives.put(resource, archiveTree);
                    }
                }
            } catch (IOException | HTTPException e) {
                logger.error("Error retrieving database {} from {}", resource.getCombinedName(), eadParser.getBasexUrl());
                this.databaseState = DatabaseState.ERROR_NOT_REACHABLE;
            } catch (JDOMException | ConfigurationException e) {
                logger.error("Error reading database {} from {}", resource.getCombinedName(), eadParser.getBasexUrl());
                this.databaseState = DatabaseState.ERROR_INVALID_FORMAT;
            } catch (BaseXException e) {
                logger.error("Error reading database {} from {}", resource.getCombinedName(), eadParser.getBasexUrl());
                this.databaseState = DatabaseState.ERROR_INVALID_FORMAT;
            }
        }

    }

    /**
     * Check if the given resource is outdated compared to the last updated date from the basex server
     * 
     * @param resource
     * @return true if the resource in basex is newer than the given one
     * @throws IOException if the basex server is not reachable
     */
    private boolean isOutdated(ArchiveResource resource) throws BaseXException, IOException {
        try {
            List<ArchiveResource> resources = this.eadParser.getPossibleDatabases();
            ArchiveResource externalResource =
                    resources.stream().filter(extResource -> extResource.getCombinedId().equals(resource.getCombinedId())).findAny().orElse(null);
            if (externalResource != null) {
                return externalResource.getModifiedDate().isAfter(resource.getModifiedDate());
            } else {
                throw new BaseXException("Resource " + resource.getCombinedName() + " not found on basex server " + this.eadParser.getBasexUrl());
            }
        } catch (HTTPException e) {
            throw new IOException("BaseX server cannot be reached: " + e.toString());
        }
    }

    ArchiveTree loadDatabase(BasexEADParser eadParser, ArchiveResource archive)
            throws ConfigurationException, IllegalStateException, IOException, HTTPException, JDOMException {
        HierarchicalConfiguration<ImmutableNode> baseXMetadataConfig = DataManager.getInstance().getConfiguration().getArchiveMetadataConfig();
        eadParser.readConfiguration(baseXMetadataConfig);
        ArchiveEntry rootElement = eadParser.loadDatabase(archive);
        logger.info("Loaded EAD database: {}", archive.getCombinedName());
        return loadTree(rootElement);
    }

    private ArchiveTree loadTree(ArchiveEntry rootElement) {

        ArchiveTree ret = new ArchiveTree();
        ret.generate(rootElement);
        if (ret.getSelectedEntry() == null) {
            ret.setSelectedEntry(ret.getRootElement());
        }
        // This should happen before the tree is expanded to the selected entry, otherwise the collapse level will be reset
        ret.getTreeView();

        return ret;

    }

    private List<NodeType> loadNodeTypes(Map<String, String> archiveNodeTypes) {
        if (archiveNodeTypes != null) {
            return archiveNodeTypes.entrySet().stream().map(entry -> new NodeType(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
