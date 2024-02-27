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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Loads and parses EAD documents from the Solr index.
 */
public class SolrEADParser extends ArchiveParser {

    private static final Logger logger = LogManager.getLogger(SolrEADParser.class);

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
     * Get the database names and file names from the basex databases
     *
     * @return List<ArchiveResource>
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @Override
    public List<ArchiveResource> getPossibleDatabases() throws PresentationException, IndexUnreachableException {
        List<SolrDocument> docs = DataManager.getInstance()
                .getSearchIndex()
                .search("+" + SolrConstants.ISWORK + ":true +" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name());

        List<ArchiveResource> ret = new ArrayList<>();
        String dbName = "TODO";
        for (SolrDocument doc : docs) {
            String resourceName = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
            Long lastUpdatedTimestamp = SolrTools.getSingleFieldLongValue(doc, SolrConstants.DATEUPDATED);
            LocalDateTime ldtDateUpdated = lastUpdatedTimestamp != null ? DateTools.getLocalDateTimeFromMillis(lastUpdatedTimestamp, false) : null;
            String lastUpdated = ldtDateUpdated != null ? DateTools.FORMATTERCNDATE.format(ldtDateUpdated) : null;
            String size = "0";
            ArchiveResource eadResource = new ArchiveResource(dbName, resourceName, lastUpdated, size);
            ret.add(eadResource);
        }

        return ret;
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
        SolrDocument doc = searchIndex.getFirstDoc(SolrConstants.PI + ":" + database.getResourceName(), null);
        if (doc != null) {
            return loadHierarchyFromIndex(1, 0, doc, configuredFields, associatedRecordMap);
        }

        return null;
    }

    /**
     * @param order
     * @param hierarchy
     * @param doc
     * @param configuredFields
     * @param associatedPIs
     * @return {@link ArchiveEntry}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private ArchiveEntry loadHierarchyFromIndex(int order, int hierarchy, SolrDocument doc, List<ArchiveMetadataField> configuredFields,
            Map<String, Entry<String, Boolean>> associatedPIs) throws PresentationException, IndexUnreachableException {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (configuredFields == null) {
            throw new IllegalArgumentException("configuredFields may not be null");
        }

        ArchiveEntry entry = new ArchiveEntry(order, hierarchy);

        // Collect metadata
        for (ArchiveMetadataField emf : configuredFields) {
            List<String> stringValues = new ArrayList<>();
            if (StringUtils.isNotEmpty(emf.getIndexField())) {
                for (String value : SolrTools.getMetadataValues(doc, emf.getIndexField())) {
                    stringValues.add(value);
                }
            }

            addFieldToEntry(entry, emf, stringValues);
        }

        String id = SolrTools.getSingleFieldStringValue(doc, "MD_ARCHIVE_ENTRY_ID");
        if (StringUtils.isNotEmpty(id)) {
            entry.setId(id);
        }

        String label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
        if (StringUtils.isNotEmpty(label)) {
            entry.setLabel(label);
        }

        // nodeType
        // TODO check otherlevel first
        entry.setNodeType(SolrTools.getSingleFieldStringValue(doc, "MD_ARCHIVE_ENTRY_LEVEL"));
        if (entry.getNodeType() == null) {
            entry.setNodeType("folder");
        }

        // Associated record
        Entry<String, Boolean> associatedRecordEntry = associatedPIs.get(entry.getId());
        if (associatedRecordEntry != null) {
            entry.setAssociatedRecordPi(associatedRecordEntry.getKey());
            entry.setContainsImage(associatedRecordEntry.getValue());
        }

        // Set description level value
        entry.setDescriptionLevel(SolrTools.getSingleFieldStringValue(doc, "MD_ARCHIVE_ENTRY_LEVEL"));

        // get child elements
        SolrDocumentList clist =
                searchIndex.getDocs(SolrConstants.IDDOC_PARENT + ":" + SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC), null);
        if (clist != null) {
            int subOrder = 0;
            int subHierarchy = hierarchy + 1;
            for (SolrDocument c : clist) {
                ArchiveEntry child = loadHierarchyFromIndex(subOrder, subHierarchy, c, configuredFields, associatedPIs);
                entry.addSubEntry(child);
                child.setParentNode(entry);
                if (child.isContainsImage()) {
                    entry.setContainsImage(true);
                }
                subOrder++;
            }
        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId(String.valueOf(UUID.randomUUID()));
        }

        return entry;
    }
    
    @Override
    public String getUrl() {
        return searchIndex.getSolrServerUrl();
    }
}
