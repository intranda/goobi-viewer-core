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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

public class RefreshArchiveTreeHandler implements MessageHandler<MessageStatus> {

    public static final String PARAMETER_IDENTIFIERS = "identifiers";

    private static final Logger logger = LogManager.getLogger(RefreshArchiveTreeHandler.class);

    @Override
    public MessageStatus call(ViewerMessage ticket, MessageQueueManager queueManager) {
        String identifiers = ticket.getProperties().get(PARAMETER_IDENTIFIERS); // space-separated PIs

        try {
            unloadAssociatedArchiveTrees(identifiers);
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.getMessage());
            return MessageStatus.ERROR;
        }

        return MessageStatus.FINISH;
    }

    /**
     * 
     * @param identifiers List of record identifiers
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static void unloadAssociatedArchiveTrees(String identifiers) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(identifiers)) {
            return;
        }

        String query1 = "+" + SolrConstants.EAD_NODE_ID + ":* +" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name() + " +"
                + SolrConstants.PI_TOPSTRUCT + ":(" + identifiers + ")";
        logger.trace("query1: {}", query1);
        SolrDocumentList associatedRecordDocs =
                DataManager.getInstance().getSearchIndex().search(query1, Collections.singletonList(SolrConstants.EAD_NODE_ID));
        if (associatedRecordDocs.isEmpty()) {
            logger.trace("No associated records found.");
            return;
        }

        String query2 = "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.EAD_NODE_ID + ":(";
        StringBuilder sb = new StringBuilder();
        for (SolrDocument doc : associatedRecordDocs) {
            String nodeId = (String) doc.getFieldValue(SolrConstants.EAD_NODE_ID);
            sb.append(nodeId).append(" ");
        }
        query2 += sb.toString().trim();
        query2 += ")";
        logger.trace("query2: {}", query2);
        SolrDocumentList archiveDocs =
                DataManager.getInstance().getSearchIndex().search(query2, Collections.singletonList(SolrConstants.PI_TOPSTRUCT));
        if (archiveDocs.isEmpty()) {
            logger.warn("No archives to unload found with query: {}", query2);
            return;
        }

        Set<String> resourceIds = new HashSet<>(archiveDocs.size());
        for (SolrDocument doc : archiveDocs) {
            resourceIds.add((String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT));

        }
        int count = DataManager.getInstance().getArchiveManager().unloadArchives(resourceIds);
        logger.debug("{} archive(s) unloaded due to associated records having been (re)indexed.", count);
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.REFRESH_ARCHIVE_TREE.name();
    }

}
