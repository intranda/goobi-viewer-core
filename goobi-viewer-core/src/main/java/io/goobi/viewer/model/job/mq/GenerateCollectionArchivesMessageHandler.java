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

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.model.archive.CollectionArchiveConfig;
import io.goobi.viewer.model.archive.CollectionArchiveService;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.collections.CollectionView;

/**
 * Scheduled dispatcher for per-collection BagIt archive generation. On each run it enumerates the configured collection fields, selects the
 * eligible collections (top-level only, unless sub-collections are enabled in the configuration), and enqueues one
 * {@link TaskType#GENERATE_COLLECTION_ARCHIVE} worker message per collection that has at least one enabled content type. It also prunes
 * archive files on disk that no longer correspond to a live collection.
 *
 * <p>
 * The actual (expensive) archive building happens in the worker handler on the dedicated {@code archives} queue, so this dispatcher only
 * performs light-weight Solr faceting and message submission.
 */
public class GenerateCollectionArchivesMessageHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(GenerateCollectionArchivesMessageHandler.class);

    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {
        Configuration config = DataManager.getInstance().getConfiguration();
        if (!config.isCollectionArchivesEnabled()) {
            logger.trace("Collection archives are disabled, skipping dispatch.");
            return MessageStatus.FINISH;
        }

        CollectionArchiveService service = new CollectionArchiveService(config);
        int enqueued = 0;
        try {
            for (String field : config.getConfiguredArchiveCollectionFields()) {
                enqueued += dispatchField(config, service, queueManager, field);
            }
        } catch (IndexUnreachableException e) {
            logger.error("Solr index unreachable while dispatching collection archives.", e);
            return MessageStatus.ERROR;
        } catch (IOException e) {
            logger.error("Error pruning orphaned collection archives.", e);
            return MessageStatus.ERROR;
        } catch (MessageQueueException e) {
            logger.error("Error enqueuing collection archive worker messages.", e);
            return MessageStatus.ERROR;
        }

        message.getProperties().put("result", "Enqueued %s collection archive job(s)".formatted(enqueued));
        return MessageStatus.FINISH;
    }

    private int dispatchField(Configuration config, CollectionArchiveService service, MessageQueueManager queueManager, String field)
            throws IndexUnreachableException, IOException, MessageQueueException {
        String splittingChar = config.getCollectionSplittingChar(field);
        boolean includeSub = config.isCollectionArchivesIncludeSubcollections();

        Map<String, CollectionResult> collections =
                SearchHelper.findAllCollectionsFromField(field, null, null, true, true, splittingChar);

        Set<String> liveSlugs = new HashSet<>();
        int enqueued = 0;
        for (String collectionName : collections.keySet()) {
            if (!includeSub && CollectionView.getLevel(collectionName, splittingChar) > 0) {
                continue;
            }
            CollectionArchiveConfig archiveConfig = config.getCollectionArchiveConfig(field, collectionName);
            if (archiveConfig == null || !archiveConfig.hasEnabledTypes()) {
                continue;
            }
            liveSlugs.add(CollectionArchiveService.slugify(collectionName));

            ViewerMessage ticket = new ViewerMessage(TaskType.GENERATE_COLLECTION_ARCHIVE.name());
            ticket.getProperties().put("field", field);
            ticket.getProperties().put("collection", collectionName);
            if (queueManager != null) {
                queueManager.addToQueue(ticket);
            }
            enqueued++;
        }

        // Remove archives for collections that are gone or no longer eligible.
        service.pruneOrphans(field, liveSlugs);
        return enqueued;
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.GENERATE_COLLECTION_ARCHIVES.name();
    }
}
