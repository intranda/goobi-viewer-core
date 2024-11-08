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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
            SolrConstants.IDDOC_PARENT, FIELD_ARCHIVE_ENTRY_LEVEL, SolrConstants.LOGID, SolrConstants.PI_TOPSTRUCT, SolrConstants.TITLE };

    private Map<String, Map<String, List<SolrDocument>>> archiveDocMap = new HashMap<>();
    /** Map of IDDOCs and their parent IDDOCs */
    private Map<String, String> parentIddocMap = new HashMap<>();
    /** Map of IDDOCs and loaded ArchiveEntry nodes */
    private Map<String, ArchiveEntry> loadedNodeMap = new HashMap<>();

    /**
     * Get the database names.
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
     * @param timestamp
     * @return Given timestamp formatted as an ISO instant; null if timestamp null
     * @should format timestamp correctly
     */
    static String formatDate(Long timestamp) {
        ZonedDateTime ldtDateUpdated =
                timestamp != null ? DateTools.getLocalDateTimeFromMillis(timestamp, false).atZone(ZoneOffset.UTC) : null;
        return ldtDateUpdated != null ? DateTools.FORMATTERISO8601DATETIMEINSTANT.format(ldtDateUpdated) : null;
    }

    /**
     * Loads the given database and parses the EAD document.
     *
     * @param database
     * @param lazyLoadingThreshold
     * @return Root element of the loaded tree
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @Override
    public ArchiveEntry loadDatabase(ArchiveResource database, int lazyLoadingThreshold) throws PresentationException, IndexUnreachableException {
        if (database == null) {
            throw new IllegalArgumentException("database may not be null");
        }

        logger.trace("loadDatabase: {}", database.getResourceId());
        List<String> solrFields = getSolrFields();
        SolrDocument topDoc =
                DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":\"" + database.getResourceId() + '"', solrFields);
        if (topDoc != null) {
            String query = "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.PI_TOPSTRUCT + ":\""
                    + database.getResourceId() + "\" -" + SolrConstants.PI + ":\"" + database.getResourceId() + '"';
            logger.trace("archive query: {}", query); //NOSONAR Debug
            SolrDocumentList archiveDocs = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, SolrSearchIndex.MAX_HITS,
                            Arrays.asList(new StringPair(SolrConstants.IDDOC_PARENT, "asc"), new StringPair(FIELD_ARCHIVE_ORDER, "asc")), solrFields);
            logger.trace("Loaded {} archive docs.", archiveDocs.size());

            Map<String, List<SolrDocument>> resourceArchiveDocMap = new HashMap<>();
            archiveDocMap.put(database.getResourceId(), resourceArchiveDocMap);

            // Add all Solr docs for this archive to map
            for (SolrDocument doc : archiveDocs) {
                String iddocParent = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
                if (iddocParent != null) {
                    List<SolrDocument> docList = resourceArchiveDocMap.computeIfAbsent(iddocParent, k -> new ArrayList<>());
                    docList.add(doc);
                    parentIddocMap.put(SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC), iddocParent);
                }
            }
            try {
                boolean recursion = archiveDocs.size() < lazyLoadingThreshold;
                if (!recursion) {
                    logger.debug("Using lazy loading due to the archive tree size ({} nodes).", archiveDocs.size());
                }
                return loadNode(0, 0, topDoc, null, recursion);
            } finally {
                logger.trace("Database loaded.");
            }
        }

        return null;
    }

    /**
     * @param order
     * @param hierarchy
     * @param doc
     * @param loadPath
     * @param loadChildrenRecursively
     * @return {@link ArchiveEntry}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public ArchiveEntry loadNode(int order, int hierarchy, SolrDocument doc, Set<String> loadPath, boolean loadChildrenRecursively)
            throws PresentationException, IndexUnreachableException {
        // logger.trace("loadNode: {}", order); //NOSONAR Debug
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }

        ArchiveEntry entry = new ArchiveEntry(order, hierarchy, doc);

        String id = SolrTools.getSingleFieldStringValue(doc, SolrConstants.EAD_NODE_ID);
        if (StringUtils.isNotEmpty(id)) {
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
        Entry<String, Boolean> associatedRecordEntry = associatedRecordMap.get(entry.getId());
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
        String iddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
        if (archiveDocMap.containsKey(entry.getTopstructPi()) && archiveDocMap.get(entry.getTopstructPi()).containsKey(iddoc)) {
            // logger.trace("found {} children of {}", archiveDocMap.get(entry.getTopstructPi()).get(iddoc).size(), iddoc); //NOSONAR Debug
            entry.setChildrenFound(true);
            if (loadChildrenRecursively || (loadPath != null && !loadPath.isEmpty()) || entry.equals(entry.getRootNode())) {
                loadChildren(entry, loadPath, loadChildrenRecursively);
            }
        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId(String.valueOf(UUID.randomUUID()));
        }

        loadedNodeMap.put(iddoc, entry);

        return entry;
    }

    /**
     * 
     * @param entry
     * @param loadPath
     * @param loadChildrenRecursively
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void loadChildren(ArchiveEntry entry, Set<String> loadPath, boolean loadChildrenRecursively)
            throws PresentationException, IndexUnreachableException {
        String iddoc = SolrTools.getSingleFieldStringValue(entry.getDoc(), SolrConstants.IDDOC);
        boolean loadChildren = loadChildrenRecursively;
        if (archiveDocMap.containsKey(entry.getTopstructPi()) && archiveDocMap.get(entry.getTopstructPi()).containsKey(iddoc)) {
            // logger.trace("Loading {} children of {}", archiveDocMap.get(iddoc).size(), entry.getLabel()); //NOSONAR Debug
            int subOrder = 0;
            int subHierarchy = entry.getHierarchyLevel() + 1;
            for (SolrDocument c : archiveDocMap.get(entry.getTopstructPi()).get(iddoc)) {
                if (!loadChildrenRecursively && loadPath != null) {
                    String childIddoc = SolrTools.getSingleFieldStringValue(c, SolrConstants.IDDOC);
                    loadChildren = loadPath.contains(childIddoc);
                    logger.trace("Child {}: allow recursion due to it being on the search path.", childIddoc);
                }
                ArchiveEntry child = loadNode(subOrder, subHierarchy, c, loadPath, loadChildren);
                entry.addSubEntry(child);
                child.setParentNode(entry);
                if (child.isContainsImage()) {
                    entry.setContainsImage(true);
                }
                subOrder++;
            }
            entry.setChildrenLoaded(true);
            // logger.trace("Children loaded for {}", entry.getLabel());
        }
    }

    /**
     * 
     * @param iddoc Parent IDDOC
     * @return Map&lt;String, List&lt;SolrDocument&gt;&gt;
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static List<SolrDocument> getChildDocsForIddoc(String iddoc)
            throws PresentationException, IndexUnreachableException {
        logger.trace("getChildDocMapForIddoc: {}", iddoc);
        SolrDocumentList archiveDocs = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.IDDOC_PARENT + ":\"" + iddoc + "\"");
        List<SolrDocument> ret = new ArrayList<>();
        for (SolrDocument doc : archiveDocs) {
            String iddocParent = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
            if (iddocParent != null) {
                ret.add(doc);
            }
        }

        return ret;
    }

    @Override
    public boolean searchInUnparsedNodes(ArchiveEntry node, String searchValue) {
        logger.trace("searchInUnparsedNodes");
        if (node == null) {
            throw new IllegalArgumentException("node may not be null");
        }

        String query =
                "+" + SolrConstants.PI_TOPSTRUCT + ":\"" + node.getTopstructPi() + "\" +" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name()
                        + " +(" + SolrConstants.EAD_NODE_ID + ":\"" + searchValue + "\" " + SolrConstants.TITLE + ":" + searchValue + ")"
                        + SearchHelper.getAllSuffixes();
        // logger.trace("Unparsed node search query: {}", query); //NOSONAR Debug

        boolean ret = false;
        try {
            SolrDocumentList result = DataManager.getInstance().getSearchIndex().search(query, SOLR_FIELDS_DATABASES);
            for (SolrDocument doc : result) {
                String iddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
                if (!loadedNodeMap.containsKey(iddoc)) {
                    // Load tree from given node down to hit node
                    Set<String> loadPath = new HashSet<>();
                    String parentIddoc = parentIddocMap.get(iddoc);
                    if (parentIddoc != null) {
                        loadPath.add(parentIddoc);
                        while (!loadedNodeMap.containsKey(parentIddoc) && parentIddocMap.get(parentIddoc) != null) {
                            parentIddoc = parentIddocMap.get(parentIddoc);
                            loadPath.add(parentIddoc);
                            logger.trace("added parent iddoc to hierarchy: {}", parentIddoc);
                        }
                        loadChildren(loadedNodeMap.get(parentIddoc), loadPath, false);
                        ret = true;
                    }

                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.getMessage());
        }

        return ret;
    }

    /**
     * a
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
