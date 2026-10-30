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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Loads and parses EAD documents from the Solr index.
 */
public class SolrEADParser extends ArchiveParser {

    private static final long serialVersionUID = -434134931986709301L;

    private static final Logger logger = LogManager.getLogger(SolrEADParser.class);

    private static final String FIELD_ARCHIVE_ENTRY_LEVEL = "MD_ARCHIVE_ENTRY_LEVEL";
    private static final String FIELD_ARCHIVE_ENTRY_OTHERLEVEL = "MD_ARCHIVE_ENTRY_OTHERLEVEL";
    private static final String FIELD_ARCHIVE_ORDER = "SORTNUM_ARCHIVE_ORDER";

    private static final List<String> SOLR_FIELDS_DATABASES =
            Arrays.asList(SolrConstants.ACCESSCONDITION, SolrConstants.DATEINDEXED, SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE);
    private static final String[] SOLR_FIELDS_ENTRIES = { SolrConstants.ACCESSCONDITION, SolrConstants.EAD_NODE_ID, SolrConstants.IDDOC,
            SolrConstants.IDDOC_PARENT, FIELD_ARCHIVE_ENTRY_LEVEL, FIELD_ARCHIVE_ENTRY_OTHERLEVEL, SolrConstants.LOGID, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.TITLE };

    private static final List<String> SOLR_FIELDS_PATH = Arrays.asList(SolrConstants.IDDOC, SolrConstants.IDDOC_PARENT);
    /** Maximum number of search hits in not yet loaded nodes for which the tree path is loaded. */
    static final int MAX_UNPARSED_SEARCH_HITS = 100;
    /**
     * Maximum number of nodes loaded by one search in not yet loaded nodes. Loading the path to a hit loads every level along it in full, so a
     * broad search could otherwise load (almost) the entire archive.
     */
    static final int MAX_NODES_LOADED_PER_SEARCH = 200_000;
    /** Minimum length of a search term to also match it as a word prefix. */
    static final int MIN_WILDCARD_TERM_LENGTH = 3;
    /** Maximum supported tree depth when loading the path to a node (guards against loops in the IDDOC_PARENT hierarchy). */
    private static final int MAX_TREE_DEPTH = 100;

    /** Solr docs of eagerly loaded archives: archive PI -> parent IDDOC -> child docs. */
    private final Map<String, Map<String, List<SolrDocument>>> archiveDocMap = new ConcurrentHashMap<>();
    /** Child counts of archives that are loaded on demand: archive PI -> parent IDDOC -> number of children. */
    private final Map<String, Map<String, Integer>> onDemandChildCounts = new ConcurrentHashMap<>();
    /** Tracks EAD_NODE_ID values seen since the last loadDatabase() call to detect duplicates. */
    private final Set<String> seenNodeIds = ConcurrentHashMap.newKeySet();

    /**
     * Gets the database names.
     *
     * @return List<ArchiveResource>
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should return all resources
     */
    @Override
    public List<ArchiveResource> getPossibleDatabases() throws PresentationException, IndexUnreachableException {
        List<SolrDocument> docs = DataManager.getInstance()
                .getSearchIndex()
                .search("+" + SolrConstants.PI + ":* +" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name(), SOLR_FIELDS_DATABASES);

        List<ArchiveResource> ret = new ArrayList<>();
        for (SolrDocument doc : docs) {
            String resourceIdentifier = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI);
            String resourceName = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
            if (resourceName == null) {
                logger.warn("Indexed archive tree '{}' is missing field: {}. Using PI instead.", resourceIdentifier, SolrConstants.TITLE);
                resourceName = resourceIdentifier;
            }
            String lastUpdated = null;
            List<String> lastUpdatedList = SolrTools.getMetadataValues(doc, SolrConstants.DATEINDEXED);
            if (!lastUpdatedList.isEmpty()) {
                lastUpdated = formatDate(Long.parseLong(lastUpdatedList.get(lastUpdatedList.size() - 1)));
                // logger.trace("Last updated: {}", lastUpdated); //NOSONAR Debug
            }

            String size = "0";
            ArchiveResource eadResource = new ArchiveResource(resourceName, resourceIdentifier, lastUpdated, size);
            ret.add(eadResource);

            for (String accessCondition : SolrTools.getMetadataValues(doc, SolrConstants.ACCESSCONDITION)) {
                if (!SolrConstants.OPEN_ACCESS_VALUE.equals(accessCondition)) {
                    eadResource.getAccessConditions().add(accessCondition);
                    logger.trace("Archive {} has access condition: {}", eadResource.getResourceName(), accessCondition);
                }
            }
        }

        return ret;
    }

    /**
     *
     * @param timestamp milliseconds since epoch, or null
     * @return Given timestamp formatted as an ISO instant; null if timestamp null
     * @should format timestamp correctly
     */
    static String formatDate(Long timestamp) {
        ZonedDateTime ldtDateUpdated =
                timestamp != null ? DateTools.getLocalDateTimeFromMillis(timestamp, false).atZone(ZoneOffset.UTC) : null;
        return ldtDateUpdated != null ? DateTools.FORMATTERISO8601DATETIMEINSTANT.format(ldtDateUpdated) : null;
    }

    /**
     * Loads the given database and parses the EAD document. Archives with fewer nodes than the given threshold are loaded entirely in one
     * query. Larger archives are loaded on demand: only the root and its children are built here, deeper levels are loaded from Solr when they
     * are expanded, selected or found by a search.
     *
     * @param database the archive resource to load
     * @param lazyLoadingThreshold minimum tree size to switch to on-demand loading
     * @return Root element of the loaded tree
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should load entire tree if below threshold
     * @should only load root and its children if at or above threshold
     */
    @Override
    public ArchiveEntry loadDatabase(ArchiveResource database, int lazyLoadingThreshold) throws PresentationException, IndexUnreachableException {
        if (database == null) {
            throw new IllegalArgumentException("database may not be null");
        }

        String pi = database.getResourceId();
        logger.trace("loadDatabase: {}", pi);
        seenNodeIds.clear();
        List<String> solrFields = getSolrFields();
        SolrDocument topDoc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + quote(pi), solrFields);
        if (topDoc == null) {
            return null;
        }

        String query = getArchiveNodesQuery(pi);

        // Count the nodes and determine which nodes have children (and how many) without fetching any node documents
        QueryResponse facetResponse = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(query, null, Collections.singletonList(SolrConstants.IDDOC_PARENT), 1, false);
        long nodeCount = facetResponse.getResults().getNumFound();
        if (nodeCount >= lazyLoadingThreshold) {
            logger.debug("Loading archive tree '{}' on demand due to its size ({} nodes).", pi, nodeCount);
            Map<String, Integer> childCounts = new HashMap<>();
            FacetField facetField = facetResponse.getFacetField(SolrConstants.IDDOC_PARENT);
            if (facetField != null && facetField.getValues() != null) {
                for (FacetField.Count value : facetField.getValues()) {
                    childCounts.put(value.getName(), (int) value.getCount());
                }
            }
            archiveDocMap.remove(pi);
            onDemandChildCounts.put(pi, childCounts);
            // Root children are always loaded by loadNode()
            return loadNode(0, 0, topDoc, false);
        }

        logger.trace("archive query: {}", query); //NOSONAR Debug
        SolrDocumentList archiveDocs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, SolrSearchIndex.MAX_HITS,
                        Arrays.asList(new StringPair(SolrConstants.IDDOC_PARENT, "asc"), new StringPair(FIELD_ARCHIVE_ORDER, "asc")), solrFields);
        logger.trace("Loaded {} archive docs.", archiveDocs.size());

        // Add all Solr docs for this archive to map
        Map<String, List<SolrDocument>> resourceArchiveDocMap = new HashMap<>();
        for (SolrDocument doc : archiveDocs) {
            String iddocParent = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
            if (iddocParent != null) {
                resourceArchiveDocMap.computeIfAbsent(iddocParent, k -> new ArrayList<>()).add(doc);
            }
        }
        onDemandChildCounts.remove(pi);
        archiveDocMap.put(pi, resourceArchiveDocMap);
        try {
            return loadNode(0, 0, topDoc, true);
        } finally {
            logger.trace("Database loaded.");
        }
    }

    /**
     * @param pi Archive PI
     * @return Solr query matching all nodes of the given archive except its root
     */
    private static String getArchiveNodesQuery(String pi) {
        return "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.PI_TOPSTRUCT + ":" + quote(pi) + " -"
                + SolrConstants.PI + ":" + quote(pi);
    }

    /**
     * @param pi Archive PI
     * @return true if the archive with the given PI is loaded on demand; false otherwise
     */
    boolean isLoadedOnDemand(String pi) {
        return pi != null && onDemandChildCounts.containsKey(pi);
    }

    /**
     * Creates an {@link ArchiveEntry} for the given Solr document. The children of the root node (hierarchy 0) are always loaded.
     *
     * @param order position of this node within its parent's children
     * @param hierarchy depth level of this node in the tree
     * @param doc Solr document representing this archive entry
     * @param loadChildrenRecursively if true, child nodes are loaded recursively
     * @return {@link ArchiveEntry}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public ArchiveEntry loadNode(int order, int hierarchy, SolrDocument doc, boolean loadChildrenRecursively)
            throws PresentationException, IndexUnreachableException {
        // logger.trace("loadNode: {}", order); //NOSONAR Debug
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }

        ArchiveEntry entry = new ArchiveEntry(order, hierarchy, doc);

        String id = SolrTools.getSingleFieldStringValue(doc, SolrConstants.EAD_NODE_ID);
        if (StringUtils.isNotEmpty(id)) {
            if (!seenNodeIds.add(id)) {
                logger.warn("Duplicate EAD node ID '{}' encountered while loading archive '{}'. "
                        + "Archive tree rendering and node lookups may be unreliable. "
                        + "Please ensure node IDs are unique in the source EAD document.",
                        id, doc.getFieldValue(SolrConstants.PI_TOPSTRUCT));
            }
            entry.setId(id);
        }

        String label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
        if (StringUtils.isNotEmpty(label)) {
            entry.setLabel(label);
        }

        String date = SolrTools.getSingleFieldStringValue(doc, SolrConstants.MD_DATECREATED);
        if (StringUtils.isNotEmpty(date)) {
            entry.setUnitdate(date);
        }

        String topstructPi = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
        if (StringUtils.isNotEmpty(topstructPi)) {
            entry.setTopstructPi(topstructPi);
        }

        String logId = SolrTools.getSingleFieldStringValue(doc, SolrConstants.LOGID);
        if (StringUtils.isNotEmpty(logId)) {
            entry.setLogId(logId);
        }

        // nodeType
        String level = SolrTools.getSingleFieldStringValue(doc, FIELD_ARCHIVE_ENTRY_OTHERLEVEL);
        if (StringUtils.isEmpty(level)) {
            level = SolrTools.getSingleFieldStringValue(doc, FIELD_ARCHIVE_ENTRY_LEVEL);
        }
        entry.setNodeType(level);
        if (entry.getNodeType() == null) {
            entry.setNodeType("folder");
        }

        // Associated record
        Entry<String, Boolean> associatedRecordEntry = associatedRecordMap != null ? associatedRecordMap.get(entry.getId()) : null;
        if (associatedRecordEntry != null) {
            entry.setAssociatedRecordPi(associatedRecordEntry.getKey());
            entry.setContainsImage(associatedRecordEntry.getValue());
        }

        // Set description level value
        entry.setDescriptionLevel(SolrTools.getSingleFieldStringValue(doc, FIELD_ARCHIVE_ENTRY_LEVEL));

        // Set access conditions (omitting OPENACCESS to save some memory)
        if (doc.containsKey(SolrConstants.ACCESSCONDITION)) {
            for (String accessCondition : SolrTools.getMetadataValues(doc, SolrConstants.ACCESSCONDITION)) {
                if (!SolrConstants.OPEN_ACCESS_VALUE.equals(accessCondition)) {
                    entry.getAccessConditions().add(accessCondition);
                }
            }
        }

        // get child elements
        if (hasChildDocs(entry.getTopstructPi(), getIddoc(entry))) {
            entry.setChildrenFound(true);
            if (loadChildrenRecursively || hierarchy == 0) {
                loadChildren(entry, loadChildrenRecursively);
            }
        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId(String.valueOf(UUID.randomUUID()));
        }

        return entry;
    }

    /**
     * Loads the immediate children of the given entry, if not yet loaded.
     *
     * @param entry the parent entry whose children are to be loaded
     * @return true if children were loaded by this call; false if they were already loaded or the entry has no children
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should load only immediate children
     * @should not load children twice
     */
    @Override
    public boolean loadChildren(ArchiveEntry entry) throws PresentationException, IndexUnreachableException {
        return loadChildren(entry, false);
    }

    /**
     * Loads the children of the given entry, if not yet loaded. Nodes may be shared between the global archive tree and session copies, so
     * loading is synchronized on the entry and the child list is published in one step.
     *
     * @param entry the parent entry whose children are to be loaded
     * @param loadChildrenRecursively if true, child nodes are loaded recursively
     * @return true if children were loaded by this call; false otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private boolean loadChildren(ArchiveEntry entry, boolean loadChildrenRecursively) throws PresentationException, IndexUnreachableException {
        if (entry == null) {
            throw new IllegalArgumentException("entry may not be null");
        }

        synchronized (entry) {
            if (entry.isChildrenLoaded()) {
                return false;
            }
            List<SolrDocument> childDocs = getChildDocs(entry);
            if (childDocs.isEmpty()) {
                return false;
            }
            // logger.trace("Loading {} children of {}", childDocs.size(), entry.getLabel()); //NOSONAR Debug
            List<ArchiveEntry> children = new ArrayList<>(childDocs.size());
            int subHierarchy = entry.getHierarchyLevel() + 1;
            boolean containsImage = entry.isContainsImage();
            for (SolrDocument c : childDocs) {
                ArchiveEntry child = loadNode(children.size(), subHierarchy, c, loadChildrenRecursively);
                child.setParentNode(entry);
                if (child.isContainsImage()) {
                    containsImage = true;
                }
                children.add(child);
            }
            entry.setSubEntryList(children);
            entry.setContainsImage(containsImage);
            entry.setChildrenLoaded(true);
            // logger.trace("Children loaded for {}", entry.getLabel());
            return true;
        }
    }

    /**
     *
     * @param pi Archive PI
     * @param iddoc IDDOC of the parent node
     * @return true if the node with the given IDDOC has child nodes; false otherwise
     */
    private boolean hasChildDocs(String pi, String iddoc) {
        if (pi == null || iddoc == null) {
            return false;
        }
        Map<String, Integer> childCounts = onDemandChildCounts.get(pi);
        if (childCounts != null) {
            return childCounts.containsKey(iddoc);
        }
        Map<String, List<SolrDocument>> docMap = archiveDocMap.get(pi);
        return docMap != null && docMap.containsKey(iddoc);
    }

    /**
     *
     * @param entry Parent entry
     * @return Child docs of the given entry, sorted by archive order; empty list if none found
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private List<SolrDocument> getChildDocs(ArchiveEntry entry) throws PresentationException, IndexUnreachableException {
        String pi = entry.getTopstructPi();
        String iddoc = getIddoc(entry);
        if (pi == null || iddoc == null) {
            return Collections.emptyList();
        }

        Map<String, Integer> childCounts = onDemandChildCounts.get(pi);
        if (childCounts == null) {
            Map<String, List<SolrDocument>> docMap = archiveDocMap.get(pi);
            return docMap != null ? docMap.getOrDefault(iddoc, Collections.emptyList()) : Collections.emptyList();
        }
        Integer childCount = childCounts.get(iddoc);
        if (childCount == null) {
            return Collections.emptyList();
        }

        return getChildDocsForIddoc(pi, iddoc, childCount);
    }

    /**
     * Queries the child nodes of the given parent node.
     *
     * @param pi Archive PI
     * @param iddoc Parent IDDOC
     * @param rows Maximum number of child docs to return
     * @return List of child docs, sorted by archive order
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    static List<SolrDocument> getChildDocsForIddoc(String pi, String iddoc, int rows)
            throws PresentationException, IndexUnreachableException {
        logger.trace("getChildDocsForIddoc: {}", iddoc);
        String query = "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.PI_TOPSTRUCT + ":" + quote(pi) + " +"
                + SolrConstants.IDDOC_PARENT + ":" + quote(iddoc);
        return DataManager.getInstance()
                .getSearchIndex()
                .search(query, rows, Collections.singletonList(new StringPair(FIELD_ARCHIVE_ORDER, "asc")), getSolrFields());
    }

    /**
     * Loads all levels of the tree between the given root node and the node with the given EAD node ID.
     *
     * @param root Root node of the tree
     * @param nodeId EAD node ID of the requested node
     * @return Loaded {@link ArchiveEntry} with the given ID; null if not found
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should load path to deep node
     * @should return null if node not found
     */
    @Override
    public ArchiveEntry loadEntryPath(ArchiveEntry root, String nodeId) throws PresentationException, IndexUnreachableException {
        if (root == null || root.getTopstructPi() == null || StringUtils.isBlank(nodeId)) {
            return null;
        }

        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc("+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.PI_TOPSTRUCT + ":"
                        + quote(root.getTopstructPi()) + " +" + SolrConstants.EAD_NODE_ID + ":" + quote(nodeId), SOLR_FIELDS_PATH);
        if (doc == null) {
            logger.debug("Archive node not found in index: {}", nodeId);
            return null;
        }

        List<String> path = getPathIddocs(root, doc, new HashMap<>());
        return path != null ? loadPath(root, path, new MutableBoolean()) : null;
    }

    /**
     * Collects the IDDOCs between the given root and the given node doc by walking up the IDDOC_PARENT hierarchy in the index.
     *
     * @param root Root node of the tree
     * @param doc Solr doc of the requested node (requires IDDOC and IDDOC_PARENT)
     * @param parentIddocCache Cache of already resolved parent IDDOCs
     * @return IDDOCs from the first level below the root down to the requested node; empty list if the doc is the root itself; null if the node
     *         does not belong to the tree
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static List<String> getPathIddocs(ArchiveEntry root, SolrDocument doc, Map<String, String> parentIddocCache)
            throws PresentationException, IndexUnreachableException {
        String rootIddoc = getIddoc(root);
        String iddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
        if (rootIddoc == null || iddoc == null) {
            return null;
        }
        if (iddoc.equals(rootIddoc)) {
            return Collections.emptyList();
        }

        Deque<String> path = new ArrayDeque<>();
        path.addFirst(iddoc);
        String parentIddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
        while (parentIddoc != null && !parentIddoc.equals(rootIddoc)) {
            if (path.size() >= MAX_TREE_DEPTH) {
                logger.warn("Archive node {} is nested deeper than {} levels or its ancestry contains a loop.", iddoc, MAX_TREE_DEPTH);
                return null;
            }
            path.addFirst(parentIddoc);
            parentIddoc = getParentIddoc(parentIddoc, parentIddocCache);
        }
        if (parentIddoc == null) {
            logger.debug("Archive node {} does not belong to the tree with the root {}.", iddoc, rootIddoc);
            return null;
        }

        return new ArrayList<>(path);
    }

    /**
     * Counts the nodes that would be created by loading the given path, i.e. the children of each node along the path that are not yet loaded.
     *
     * @param root Root node of the tree
     * @param path IDDOCs from the first level below the root down to the requested node
     * @return Number of nodes to load
     */
    private int countNodesToLoad(ArchiveEntry root, List<String> path) {
        String pi = root.getTopstructPi();
        int ret = 0;
        ArchiveEntry node = root;
        String parentIddoc = getIddoc(root);
        for (String pathIddoc : path) {
            if (node != null && node.isChildrenLoaded()) {
                node = findChildByIddoc(node, pathIddoc);
            } else {
                // Neither this level nor any level below is built yet
                node = null;
                ret += getChildCount(pi, parentIddoc);
            }
            parentIddoc = pathIddoc;
        }

        return ret;
    }

    /**
     * Loads the tree levels along the given path, starting from the root.
     *
     * @param root Root node of the tree
     * @param path IDDOCs from the first level below the root down to the requested node
     * @param childrenLoaded Set to true if any children were loaded
     * @return Loaded {@link ArchiveEntry} at the end of the path; null if not found
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private ArchiveEntry loadPath(ArchiveEntry root, List<String> path, MutableBoolean childrenLoaded)
            throws PresentationException, IndexUnreachableException {
        ArchiveEntry node = root;
        for (String pathIddoc : path) {
            if (loadChildren(node, false)) {
                childrenLoaded.setTrue();
            }
            node = findChildByIddoc(node, pathIddoc);
            if (node == null) {
                return null;
            }
        }

        return node;
    }

    /**
     *
     * @param pi Archive PI
     * @param iddoc IDDOC of the parent node
     * @return Number of children of the node with the given IDDOC
     */
    private int getChildCount(String pi, String iddoc) {
        if (pi == null || iddoc == null) {
            return 0;
        }
        Map<String, Integer> childCounts = onDemandChildCounts.get(pi);
        if (childCounts != null) {
            return childCounts.getOrDefault(iddoc, 0);
        }
        Map<String, List<SolrDocument>> docMap = archiveDocMap.get(pi);
        return docMap != null && docMap.containsKey(iddoc) ? docMap.get(iddoc).size() : 0;
    }

    /**
     *
     * @param iddoc IDDOC of the child node
     * @param parentIddocCache Cache of already resolved parent IDDOCs
     * @return IDDOC_PARENT value of the node with the given IDDOC; null if not found
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static String getParentIddoc(String iddoc, Map<String, String> parentIddocCache) throws PresentationException, IndexUnreachableException {
        if (parentIddocCache.containsKey(iddoc)) {
            return parentIddocCache.get(iddoc);
        }
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.IDDOC + ":" + quote(iddoc), SOLR_FIELDS_PATH);
        String ret = doc != null ? SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT) : null;
        parentIddocCache.put(iddoc, ret);
        return ret;
    }

    /**
     *
     * @param parent Parent node
     * @param iddoc IDDOC of the requested child
     * @return Child of parent with the given IDDOC; null if not found
     */
    private static ArchiveEntry findChildByIddoc(ArchiveEntry parent, String iddoc) {
        for (ArchiveEntry child : parent.getSubEntryList()) {
            if (iddoc.equals(getIddoc(child))) {
                return child;
            }
        }

        return null;
    }

    /**
     *
     * @param entry Archive entry
     * @return IDDOC of the given entry's Solr doc; null if not available
     */
    private static String getIddoc(ArchiveEntry entry) {
        return entry.getDoc() != null ? SolrTools.getSingleFieldStringValue(entry.getDoc(), SolrConstants.IDDOC) : null;
    }

    /**
     * Searches the index for nodes of an on-demand loaded archive that match the given value and loads the tree path to each hit. Paths are
     * loaded for at most {@code MAX_UNPARSED_SEARCH_HITS} hits and for at most {@code MAX_NODES_LOADED_PER_SEARCH} newly loaded nodes in total;
     * if either limit is reached, the result is flagged as incomplete.
     *
     * @param node Any node of the archive tree
     * @param searchValue Search terms
     * @return {@link UnparsedNodeSearchResult}
     * @should load path to matching nodes
     * @should match word prefixes
     */
    @Override
    public UnparsedNodeSearchResult searchInUnparsedNodes(ArchiveEntry node, String searchValue) {
        return searchInUnparsedNodes(node, searchValue, MAX_UNPARSED_SEARCH_HITS, MAX_NODES_LOADED_PER_SEARCH);
    }

    /**
     *
     * @param node Any node of the archive tree
     * @param searchValue Search terms
     * @param maxHits Maximum number of hits for which to load the tree path
     * @param maxNodesToLoad Maximum number of nodes to load in total
     * @return {@link UnparsedNodeSearchResult}
     * @should flag result incomplete if more hits than allowed
     * @should skip hits whose path exceeds the node budget
     */
    UnparsedNodeSearchResult searchInUnparsedNodes(ArchiveEntry node, String searchValue, int maxHits, int maxNodesToLoad) {
        logger.trace("searchInUnparsedNodes");
        if (node == null) {
            throw new IllegalArgumentException("node may not be null");
        }
        ArchiveEntry root = node.getRootNode();
        String pi = root.getTopstructPi();
        if (StringUtils.isBlank(searchValue) || !isLoadedOnDemand(pi)) {
            return UnparsedNodeSearchResult.NONE;
        }

        String query = getArchiveNodesQuery(pi) + " +(" + SolrConstants.EAD_NODE_ID + ":" + quote(searchValue.trim()) + " " + SolrConstants.TITLE
                + ":(" + buildTitleQuery(searchValue) + "))" + SearchHelper.getAllSuffixes();
        // logger.trace("Unparsed node search query: {}", query); //NOSONAR Debug

        MutableBoolean childrenLoaded = new MutableBoolean();
        boolean incomplete = false;
        try {
            SolrDocumentList result = DataManager.getInstance().getSearchIndex().search(query, maxHits, null, SOLR_FIELDS_PATH);
            if (result.getNumFound() > maxHits) {
                logger.debug("Archive search for '{}' found {} nodes, loading only the first {}.", searchValue, result.getNumFound(), maxHits);
                incomplete = true;
            }
            Map<String, String> parentIddocCache = new HashMap<>();
            int remainingNodes = maxNodesToLoad;
            int skippedHits = 0;
            for (SolrDocument doc : result) {
                List<String> path = getPathIddocs(root, doc, parentIddocCache);
                if (path == null) {
                    continue;
                }
                // Each hit in a level that is not loaded yet loads that entire level, which may hold many thousands of nodes
                int nodesToLoad = countNodesToLoad(root, path);
                if (nodesToLoad > remainingNodes) {
                    skippedHits++;
                    continue;
                }
                remainingNodes -= nodesToLoad;
                loadPath(root, path, childrenLoaded);
            }
            if (skippedHits > 0) {
                logger.debug("Archive search for '{}': {} hits skipped, loading their tree paths would exceed {} nodes.", searchValue,
                        skippedHits, maxNodesToLoad);
                incomplete = true;
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.getMessage());
        }

        return new UnparsedNodeSearchResult(childrenLoaded.isTrue(), incomplete);
    }

    /**
     * Builds a query for the title field that requires all terms of the given search value. Terms with at least
     * {@code MIN_WILDCARD_TERM_LENGTH} characters also match as word prefixes; shorter terms only match whole words, since prefix searches
     * for one or two characters would match almost every node.
     *
     * @param searchValue Search terms
     * @return Query string for the title field
     * @should require all terms
     * @should add prefix wildcard to terms with at least three characters
     * @should escape special characters
     */
    static String buildTitleQuery(String searchValue) {
        return Arrays.stream(searchValue.trim().split("\\s+")).map(term -> {
            String escapedTerm = ClientUtils.escapeQueryChars(term);
            if (term.length() < MIN_WILDCARD_TERM_LENGTH) {
                return "+" + escapedTerm;
            }
            // Keep the plain term so that it is still analyzed (wildcard terms are not), e.g. for case-insensitive or stemmed matches
            return "+(" + escapedTerm + " " + escapedTerm + "*)";
        }).collect(Collectors.joining(" "));
    }

    /**
     * @param value Value to quote
     * @return Given value escaped and wrapped in double quotes for use in a Solr query
     */
    private static String quote(String value) {
        return "\"" + ClientUtils.escapeQueryChars(value) + "\"";
    }

    /**
     * A.
     * 
     * @param template Metadata template name
     * @return List of Solr field names
     */
    static List<String> getSolrFields() {
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getArchiveMetadata();

        List<String> ret = new ArrayList<>(SOLR_FIELDS_ENTRIES.length + metadataList.size());
        ret.addAll(Arrays.asList(SOLR_FIELDS_ENTRIES));

        // Collect mapped metadata field names
        Set<String> additionalMetadataFields = new HashSet<>();
        for (Metadata md : metadataList) {
            additionalMetadataFields.addAll(md.getParamFieldNames());
        }
        ret.addAll(additionalMetadataFields);

        return ret;
    }

    @Override
    public String getUrl() {
        return DataManager.getInstance().getSearchIndex().getSolrServerUrl();
    }

    @Override
    public String toString() {
        return getUrl();
    }
}
