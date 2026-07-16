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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.archive.CollectionArchiveConfig;
import io.goobi.viewer.model.archive.CollectionArchiveService;
import io.goobi.viewer.model.job.TaskType;

/**
 * Worker that (re)generates the BagIt archive for a single collection. The collection is identified by the {@code field} and
 * {@code collection} message properties set by {@link GenerateCollectionArchivesMessageHandler}. Generation is skipped when the collection
 * is unchanged since the last run, empty, or has no enabled content types (see
 * {@link CollectionArchiveService#generateIfChanged(String, String, CollectionArchiveConfig)}).
 *
 * <p>
 * Transient Solr/IO failures return {@link MessageStatus#ERROR} so the message queue retries the job.
 */
public class GenerateCollectionArchiveMessageHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(GenerateCollectionArchiveMessageHandler.class);

    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {
        String field = message.getProperties().get("field");
        String collection = message.getProperties().get("collection");
        if (StringUtils.isAnyBlank(field, collection)) {
            logger.error("Missing 'field' or 'collection' property, cannot generate collection archive.");
            return MessageStatus.ERROR;
        }

        Configuration config = DataManager.getInstance().getConfiguration();
        if (!config.isCollectionArchivesEnabled()) {
            return MessageStatus.FINISH;
        }

        CollectionArchiveService service = new CollectionArchiveService(config);
        CollectionArchiveConfig archiveConfig = config.getCollectionArchiveConfig(field, collection);
        try {
            CollectionArchiveService.GenerationResult result = service.generateIfChanged(field, collection, archiveConfig);
            message.getProperties().put("result", (result.generated() ? "generated: " : "skipped: ") + result.detail());
            return MessageStatus.FINISH;
        } catch (IndexUnreachableException | PresentationException e) {
            logger.error("Solr error generating archive for {}:{}", field, collection, e);
            return MessageStatus.ERROR;
        } catch (DAOException e) {
            logger.error("DAO error generating archive for {}:{}", field, collection, e);
            return MessageStatus.ERROR;
        } catch (IOException e) {
            logger.error("I/O error generating archive for {}:{}", field, collection, e);
            return MessageStatus.ERROR;
        } catch (ViewerConfigurationException e) {
            logger.error("Configuration error generating archive for {}:{}", field, collection, e);
            return MessageStatus.ERROR;
        }
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.GENERATE_COLLECTION_ARCHIVE.name();
    }
}
