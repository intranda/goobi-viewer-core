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
        if (StringUtils.isNotBlank(identifiers)) {
            String query1 = "+" + SolrConstants.EAD_NODE_ID + ":* +" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name() + " +"
                    + SolrConstants.PI_TOPSTRUCT + ":(" + identifiers + ")";
            try {
                SolrDocumentList docs1 =
                        DataManager.getInstance().getSearchIndex().search(query1, Collections.singletonList(SolrConstants.EAD_NODE_ID));
                if (!docs1.isEmpty()) {
                    String query2 = "+" + SolrConstants.PI + ":* +" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +"
                            + SolrConstants.EAD_NODE_ID + ":(";
                    StringBuilder sb = new StringBuilder();
                    for (SolrDocument doc : docs1) {
                        String nodeId = (String) doc.getFieldValue(SolrConstants.EAD_NODE_ID);
                        sb.append(nodeId).append(" ");
                    }
                    query2 += sb.toString().trim();
                    query2 += ")";
                    SolrDocumentList docs2 =
                            DataManager.getInstance().getSearchIndex().search(query2, Collections.singletonList(SolrConstants.PI));
                }

            } catch (PresentationException | IndexUnreachableException e) {
                logger.error(e.getMessage());
                return MessageStatus.ERROR;
            }
        }

        return MessageStatus.FINISH;
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.DELETE_RESOURCE.name();
    }

}
