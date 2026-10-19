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
package io.goobi.viewer.model.job.mq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Message handler that unloads cached archive trees for a set of record identifiers so that they are reloaded from Solr on next access.
 */
public class RefreshArchiveTreeHandler implements MessageHandler<MessageStatus> {

    public static final String PARAMETER_IDENTIFIERS = "identifiers";

    /** Maximum number of identifiers per Solr query (one boolean clause each; Solr's default maxBooleanClauses is 1024). */
    static final int MAX_IDENTIFIERS_PER_QUERY = 500;

    private static final Logger logger = LogManager.getLogger(RefreshArchiveTreeHandler.class);

    /**
     * Identifiers of (re)indexed records whose associated archive trees still need to be unloaded. Requests from the indexer only add to
     * this set while a refresh task is already queued, so that a bulk import results in a few refresh tasks instead of one per record.
     */
    private static final Set<String> PENDING_IDENTIFIERS = ConcurrentHashMap.newKeySet();
    /** True while a refresh task is queued but has not started yet (it then takes all pending identifiers). */
    private static final AtomicBoolean REFRESH_QUEUED = new AtomicBoolean(false);

    @Override
    public MessageStatus call(ViewerMessage ticket, MessageQueueManager queueManager) {
        // Reset first: identifiers added from now on are handled by a new task
        REFRESH_QUEUED.set(false);
        Set<String> identifiers = takePendingIdentifiers();
        String ticketIdentifiers = ticket.getProperties().get(PARAMETER_IDENTIFIERS); // space-separated PIs
        if (StringUtils.isNotBlank(ticketIdentifiers)) {
            identifiers.addAll(Arrays.asList(ticketIdentifiers.trim().split("\\s+")));
        }

        try {
            unloadAssociatedArchiveTrees(identifiers);
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.getMessage());
            return MessageStatus.ERROR;
        }

        return MessageStatus.FINISH;
    }

    /**
     * Registers identifiers of (re)indexed records whose associated archive trees need to be unloaded.
     *
     * @param identifiers Record identifiers
     * @return true if the caller must queue a new refresh task; false if a queued task will already handle the identifiers
     * @should only request one task until it has started
     */
    public static boolean addPendingIdentifiers(Collection<String> identifiers) {
        PENDING_IDENTIFIERS.addAll(identifiers);
        return REFRESH_QUEUED.compareAndSet(false, true);
    }

    /**
     * @return true if a refresh task is queued but has not started yet
     */
    public static boolean isRefreshQueued() {
        return REFRESH_QUEUED.get();
    }

    /**
     * Resets the queued state, e.g. if queueing the refresh task failed.
     */
    public static void resetRefreshQueued() {
        REFRESH_QUEUED.set(false);
    }

    /**
     * 
     * @return All pending identifiers (removed from the pending set)
     */
    static Set<String> takePendingIdentifiers() {
        Set<String> ret = new HashSet<>();
        for (Iterator<String> it = PENDING_IDENTIFIERS.iterator(); it.hasNext();) {
            ret.add(it.next());
            it.remove();
        }
        return ret;
    }

    /**
     * Unloads the archive trees associated with the given records, so that they are rebuilt on their next use.
     * 
     * @param identifiers Record identifiers
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static void unloadAssociatedArchiveTrees(Collection<String> identifiers) throws PresentationException, IndexUnreachableException {
        if (identifiers.isEmpty()) {
            return;
        }

        // Associated archive node IDs of the given records
        Set<String> nodeIds = new HashSet<>();
        for (List<String> chunk : chunks(identifiers)) {
            String query1 = "+" + SolrConstants.EAD_NODE_ID + ":* +" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name() + " +"
                    + SolrConstants.PI_TOPSTRUCT + ":(" + String.join(" ", chunk) + ")";
            logger.trace("query1: {}", query1);
            for (SolrDocument doc : searchAll(query1, SolrConstants.EAD_NODE_ID)) {
                String nodeId = (String) doc.getFieldValue(SolrConstants.EAD_NODE_ID);
                if (StringUtils.isNotEmpty(nodeId)) {
                    nodeIds.add(nodeId);
                }
            }
        }
        if (nodeIds.isEmpty()) {
            logger.trace("No associated records found.");
            return;
        }

        // Archives containing these nodes
        Set<String> resourceIds = new HashSet<>();
        for (List<String> chunk : chunks(nodeIds)) {
            String query2 = "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.EAD_NODE_ID + ":("
                    + String.join(" ", chunk) + ")";
            logger.trace("query2: {}", query2);
            for (SolrDocument doc : searchAll(query2, SolrConstants.PI_TOPSTRUCT)) {
                resourceIds.add((String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT));
            }
        }
        if (resourceIds.isEmpty()) {
            logger.warn("No archives to unload found for {} associated archive node(s).", nodeIds.size());
            return;
        }

        int count = DataManager.getInstance().getArchiveManager().unloadArchives(resourceIds);
        logger.debug("{} archive(s) unloaded due to associated records having been (re)indexed.", count);
    }

    /**
     * Returns all hits for the given query, requesting only as many rows as there are hits (Solr allocates its result buffer according to
     * the requested number of rows).
     *
     * @param query Solr query
     * @param field Field to return
     * @return Matching documents
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static SolrDocumentList searchAll(String query, String field) throws PresentationException, IndexUnreachableException {
        long hits = DataManager.getInstance().getSearchIndex().getHitCount(query);
        if (hits == 0) {
            return new SolrDocumentList();
        }
        return DataManager.getInstance()
                .getSearchIndex()
                .search(query, (int) Math.min(hits, SolrSearchIndex.MAX_HITS), null, Collections.singletonList(field));
    }

    /**
     * 
     * @param values Values to split
     * @return Lists of at most {@link #MAX_IDENTIFIERS_PER_QUERY} values
     * @should split values into chunks of at most max size
     */
    static List<List<String>> chunks(Collection<String> values) {
        List<String> all = new ArrayList<>(values);
        List<List<String>> ret = new ArrayList<>();
        for (int i = 0; i < all.size(); i += MAX_IDENTIFIERS_PER_QUERY) {
            ret.add(all.subList(i, Math.min(i + MAX_IDENTIFIERS_PER_QUERY, all.size())));
        }
        return ret;
    }

    public static boolean hasArchiveAssociatedRecords(String identifiers)
            throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(identifiers)) {
            return false;
        }
        String query = "+" + SolrConstants.EAD_NODE_ID + ":* +"
                + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name()
                + " +" + SolrConstants.PI_TOPSTRUCT + ":(" + identifiers + ")";
        return DataManager.getInstance().getSearchIndex().getHitCount(query) > 0;
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.REFRESH_ARCHIVE_TREE.name();
    }

}
