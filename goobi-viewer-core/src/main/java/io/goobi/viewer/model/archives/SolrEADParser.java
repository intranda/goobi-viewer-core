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
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Loads and parses EAD documents from the Solr index.
 */
public class SolrEADParser extends ArchiveParser {

    private static final Logger logger = LogManager.getLogger(SolrEADParser.class);

    private static final String FIELD_ARCHIVE_ENTRY_LEVEL = "MD_ARCHIVE_ENTRY_LEVEL";
    public static final String DATABASE_NAME = "EAD";

    private static final List<String> SOLR_FIELDS_DATABASES =
            Arrays.asList(SolrConstants.DATEINDEXED, SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE);
    private static final String[] SOLR_FIELDS_ENTRIES = { SolrConstants.IDDOC,
            SolrConstants.IDDOC_PARENT, SolrConstants.EAD_NODE_ID, FIELD_ARCHIVE_ENTRY_LEVEL, SolrConstants.TITLE };

    private Map<String, List<SolrDocument>> archiveDocMap = new HashMap<>();

    /**
     *
     * @param searchIndex
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public SolrEADParser(SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        super(searchIndex);
        updateAssociatedRecordMap();
    }

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
        String dbName = DATABASE_NAME;
        for (SolrDocument doc : docs) {
            String resourceName = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
            if (resourceName == null) {
                logger.warn("Indexed archive tree is missing field: {}", SolrConstants.TITLE);
                resourceName = SolrConstants.TITLE + " NOT FOUND";
            }
            String resourceIdentifier = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI);
            String lastUpdated = null;
            List<String> lastUpdatedList = SolrTools.getMetadataValues(doc, SolrConstants.DATEINDEXED);
            if (!lastUpdatedList.isEmpty()) {
                lastUpdated = formatDate(Long.parseLong(lastUpdatedList.get(lastUpdatedList.size() - 1)));
                logger.trace("Last updated: {}", lastUpdated);
            }

            String size = "0";
            ArchiveResource eadResource = new ArchiveResource(dbName, resourceName, resourceIdentifier, lastUpdated, size);
            ret.add(eadResource);
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
     * @return Root element of the loaded tree
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @Override
    public ArchiveEntry loadDatabase(ArchiveResource database) throws PresentationException, IndexUnreachableException {
        if (database == null) {
            throw new IllegalArgumentException("database may not be null");
        }

        logger.trace("loadDatabase: {}", database.getResourceId());
        List<String> solrFields = getSolrFields("");
        SolrDocument topDoc = searchIndex.getFirstDoc(SolrConstants.PI + ":\"" + database.getResourceId() + '"', solrFields);
        if (topDoc != null) {
            SolrDocumentList archiveDocs = searchIndex.search(SolrConstants.PI_TOPSTRUCT + ":\"" + database.getResourceId() + "\" -"
                    + SolrConstants.PI + ":\"" + database.getResourceId() + '"' + SearchHelper.getAllSuffixes(), solrFields);

            // Add all Solr docs for this archive to map
            for (SolrDocument doc : archiveDocs) {
                String iddocParent = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
                if (iddocParent != null) {
                    List<SolrDocument> docList = archiveDocMap.computeIfAbsent(iddocParent, k -> new ArrayList<>());
                    docList.add(doc);
                }
            }
            try {
                boolean recursion = archiveDocs.size() < 10000;
                return loadHierarchyFromIndex(null, 0, 0, topDoc, false);
            } finally {
                logger.trace("Database loaded.");
            }
        }

        return null;
    }

    /**
     * @param inEntry
     * @param order
     * @param hierarchy
     * @param doc
     * @param loadChildren
     * @return {@link ArchiveEntry}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public ArchiveEntry loadHierarchyFromIndex(ArchiveEntry inEntry, int order, int hierarchy, SolrDocument doc, boolean loadChildren)
            throws PresentationException, IndexUnreachableException {
        // logger.trace("loadHierarchyFromIndex: {}", order);
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }

        ArchiveEntry entry = inEntry;
        if (entry == null) {
            // Only create new entry, if none was passed
            entry = new ArchiveEntry(order, hierarchy, doc);

            String id = SolrTools.getSingleFieldStringValue(doc, SolrConstants.EAD_NODE_ID);
            if (StringUtils.isNotEmpty(id)) {
                entry.setId(id);
            }

            String label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
            if (StringUtils.isNotEmpty(label)) {
                entry.setLabel(label);
            }

            // nodeType
            // TODO check otherlevel first
            entry.setNodeType(SolrTools.getSingleFieldStringValue(doc, FIELD_ARCHIVE_ENTRY_LEVEL));
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
        }

        // get child elements
        String iddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
        if (archiveDocMap.containsKey(iddoc)) {
            logger.trace("found {} children of {}", archiveDocMap.get(iddoc).size(), iddoc); //NOSONAR Debug
            entry.setChildrenFound(true);
            if (loadChildren || archiveDocMap.get(iddoc).size() == 1) {
                loadChildren(entry, loadChildren);
            }
        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId(String.valueOf(UUID.randomUUID()));
        }

        return entry;
    }

    /**
     * 
     * @param entry
     * @param loadRecursively
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void loadChildren(ArchiveEntry entry, boolean loadRecursively)
            throws PresentationException, IndexUnreachableException {
        String iddoc = SolrTools.getSingleFieldStringValue(entry.getDoc(), SolrConstants.IDDOC);
        if (archiveDocMap.containsKey(iddoc)) {
            logger.trace("Loading {} children of {}", archiveDocMap.get(iddoc).size(), entry.getLabel()); //NOSONAR Debug
            int subOrder = 0;
            int subHierarchy = entry.getHierarchyLevel() + 1;
            for (SolrDocument c : archiveDocMap.get(iddoc)) {
                ArchiveEntry child = loadHierarchyFromIndex(null, subOrder, subHierarchy, c, loadRecursively);
                entry.addSubEntry(child);
                child.setParentNode(entry);
                if (child.isContainsImage()) {
                    entry.setContainsImage(true);
                }
                subOrder++;
            }
            entry.setChildrenLoaded(true);
            logger.trace("Children loaded for {}", entry.getLabel());
        }
    }

    /**
     * 
     * @param iddoc Parent IDDOC
     * @return Map<String, List<SolrDocument>>
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

    /**
     * a
     * 
     * @param template Metadata template name
     * @return List of Solr field names
     */
    static List<String> getSolrFields(String template) {
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getArchiveMetadataForTemplate(template);

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
        return searchIndex.getSolrServerUrl();
    }
}
