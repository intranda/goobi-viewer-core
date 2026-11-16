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
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Abstract base class for parsing EAD archive structures and resolving associated Goobi viewer
 * record identifiers from the Solr index.
 */
public abstract class ArchiveParser implements Serializable {

    private static final long serialVersionUID = -7986836324388942249L;

    private static final Logger logger = LogManager.getLogger(ArchiveParser.class);

    private static final String ASSOCIATED_RECORDS_QUERY =
            "+" + SolrConstants.EAD_NODE_ID + ":* +" + SolrConstants.PI + ":* +" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name();

    protected transient volatile Map<String, Entry<String, Boolean>> associatedRecordMap;

    /** State of the linked records in the index when associatedRecordMap was loaded. */
    private transient volatile String associatedRecordMapFingerprint;

    protected ArchiveParser() {
    }

    public void updateAssociatedRecordMap() throws PresentationException, IndexUnreachableException {
        SolrSearchIndex searchIndex = DataManager.getInstance().getSearchIndex();
        String fingerprint = getAssociatedRecordsFingerprint(searchIndex);
        this.associatedRecordMap = getAssociatedRecordPis(searchIndex);
        this.associatedRecordMapFingerprint = fingerprint;
    }

    /**
     * Loads the map of records linked to archive nodes, unless it is already loaded and no linked record was added, removed or reindexed since.
     *
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void updateAssociatedRecordMapIfChanged() throws PresentationException, IndexUnreachableException {
        updateAssociatedRecordMapIfChanged(DataManager.getInstance().getSearchIndex());
    }

    /**
     * Loads the map of records linked to archive nodes, unless it is already loaded and no linked record was added, removed or reindexed since.
     *
     * @param searchIndex Solr search index to query
     * @return true if the map was (re)loaded; false otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should load map if not yet loaded
     * @should not reload map if linked records are unchanged
     * @should reload map if linked records changed
     */
    boolean updateAssociatedRecordMapIfChanged(SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        String fingerprint = getAssociatedRecordsFingerprint(searchIndex);
        if (associatedRecordMap != null && fingerprint.equals(associatedRecordMapFingerprint)) {
            return false;
        }
        logger.debug("Loading records linked to archive nodes ({}).", fingerprint);
        this.associatedRecordMap = getAssociatedRecordPis(searchIndex);
        this.associatedRecordMapFingerprint = fingerprint;
        return true;
    }

    /**
     * Returns the number of records linked to archive nodes and the latest time one of them was indexed. Unlike loading the records
     * themselves, this needs only a single document from the index.
     *
     * @param searchIndex Solr search index to query
     * @return Fingerprint that changes whenever a linked record is added, removed or reindexed
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    static String getAssociatedRecordsFingerprint(SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        SolrDocumentList docs = searchIndex.search(ASSOCIATED_RECORDS_QUERY, 1,
                Collections.singletonList(new StringPair(SolrConstants.DATEINDEXED, "desc")), Collections.singletonList(SolrConstants.DATEINDEXED));
        long latest = 0;
        if (!docs.isEmpty()) {
            Collection<Object> values = docs.get(0).getFieldValues(SolrConstants.DATEINDEXED);
            if (values != null) {
                for (Object value : values) {
                    if (value instanceof Number number) {
                        latest = Math.max(latest, number.longValue());
                    }
                }
            }
        }
        return docs.getNumFound() + "/" + latest;
    }

    /**
     * 
     * @param searchIndex Solr search index to query
     * @return Map&lt;String, Entry&lt;String, Boolean&gt;&gt;
     * @should keep first record if several records are linked to the same node
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    protected static Map<String, Entry<String, Boolean>> getAssociatedRecordPis(SolrSearchIndex searchIndex)
            throws PresentationException, IndexUnreachableException {
        if (searchIndex != null) {
            return searchIndex
                    .search(ASSOCIATED_RECORDS_QUERY,
                            Arrays.asList(SolrConstants.EAD_NODE_ID, SolrConstants.PI, SolrConstants.BOOL_IMAGEAVAILABLE))
                    .stream()
                    .collect(Collectors.toMap(doc -> SolrTools.getAsString(doc.getFieldValue(SolrConstants.EAD_NODE_ID)),
                            doc -> new SimpleEntry<String, Boolean>(SolrTools.getAsString(doc.getFieldValue(SolrConstants.PI)),
                                    SolrTools.getAsBoolean(doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE))),
                            (first, second) -> {
                                // Several records linked to the same archive node: keep the first one
                                logger.warn("Records {} and {} are linked to the same archive node; using {}.", first.getKey(), second.getKey(),
                                        first.getKey());
                                return first;
                            }));
        }
        return Collections.emptyMap();
    }

    /**
     * Gets the database names and file names.
     *
     * @return List<ArchiveResource>
     * @throws HTTPException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws PresentationException
     */
    public abstract List<ArchiveResource> getPossibleDatabases() throws PresentationException, IndexUnreachableException, IOException, HTTPException;

    /**
     * Loads the given database and parses the EAD document.
     *
     * @param database archive resource to load
     * @param lazyLoadingThreshold Number of total archive nodes from which to no longer eager load the entire tree
     * @return Root element of the loaded tree
     * @throws HTTPException
     * @throws IllegalStateException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws PresentationException
     */
    public abstract ArchiveEntry loadDatabase(ArchiveResource database, int lazyLoadingThreshold)
            throws PresentationException, IndexUnreachableException, IllegalStateException, IOException, HTTPException;

    /**
     * Result of a search in archive nodes that are not yet loaded.
     *
     * @param nodesLoaded true if new nodes were loaded into the tree
     * @param incomplete true if the search was too broad to load all matching nodes
     */
    public record UnparsedNodeSearchResult(boolean nodesLoaded, boolean incomplete) {

        /** Result for a search that loaded nothing and is not incomplete. */
        public static final UnparsedNodeSearchResult NONE = new UnparsedNodeSearchResult(false, false);
    }

    public static String getIdForName(String name) {
        return name.replaceAll("(?i)\\.xml", "");
    }

    public abstract String getUrl();

    /**
     * 
     * @param node archive entry node to search within
     * @param searchValue search term to match against unparsed nodes
     * @return {@link UnparsedNodeSearchResult}
     */
    public abstract UnparsedNodeSearchResult searchInUnparsedNodes(ArchiveEntry node, String searchValue);

    /**
     * Loads the immediate children of the given entry, if the parser supports loading nodes on demand. The default implementation does nothing.
     *
     * @param entry the parent entry whose children are to be loaded
     * @return true if children were loaded by this call; false otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean loadChildren(ArchiveEntry entry) throws PresentationException, IndexUnreachableException {
        return false;
    }

    /**
     * Loads all tree levels between the given root and the node with the given EAD node ID, if the parser supports loading nodes on demand. The
     * default implementation does nothing.
     *
     * @param root Root node of the tree
     * @param nodeId EAD node ID of the requested node
     * @return Loaded {@link ArchiveEntry} with the given ID; null if not found
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public ArchiveEntry loadEntryPath(ArchiveEntry root, String nodeId) throws PresentationException, IndexUnreachableException {
        return null;
    }
}
