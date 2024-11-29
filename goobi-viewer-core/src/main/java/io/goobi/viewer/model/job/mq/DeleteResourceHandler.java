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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.model.job.TaskType;

public class DeleteResourceHandler implements MessageHandler<MessageStatus> {

    public static final String PARAMETER_RESOURCE_PATH = "path";

    private static final Logger logger = LogManager.getLogger(DeleteResourceHandler.class);

    @Override
    public MessageStatus call(ViewerMessage ticket, MessageQueueManager queueManager) {

        String pathString = ticket.getProperties().get(PARAMETER_RESOURCE_PATH);

        if (StringUtils.isNotBlank(pathString)) {
            Path path = Path.of(pathString);
            if (Files.exists(path)) {
                try {
                    FileUtils.deleteDirectory(path.toFile());
                    return MessageStatus.FINISH;
                } catch (IOException e) {
                    logger.error("Error deleting path {}. Reason: {}", path, e.toString());
                    return MessageStatus.ERROR;
                }
            } else {
                logger.error("Cannot delete resource at  {}. file location does not exist", path);
                ticket.setDoNotRetry();
                return MessageStatus.ERROR;
            }
        } else {
            logger.error("Error deleting path. Path is empty");
            ticket.setDoNotRetry();
            return MessageStatus.ERROR;
        }

    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.DELETE_RESOURCE.name();
    }

}
