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

package io.goobi.viewer.model.job.quartz;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;

import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.model.job.TaskType;

/**
 * Cronjob
 * 
 * @author florian
 *
 */
public class HandleMessageJob extends AbstractViewerJob implements IViewerJob, Job {

    private static final Logger logger = LogManager.getLogger(HandleMessageJob.class);

    private final TaskType taskType;
    private final String cronSchedulerExpression;

    public HandleMessageJob(TaskType taskType, String cronSchedulerExpression, MessageQueueManager messageBroker) {
        this.taskType = taskType;
        this.cronSchedulerExpression = cronSchedulerExpression;
    }

    public HandleMessageJob() {
        this.taskType = null;
        this.cronSchedulerExpression = "";
    }

    @Override
    public String getJobName() {
        return Optional.ofNullable(taskType).map(TaskType::name).orElse("");
    }

    @Override
    public String getCronExpression() {
        return cronSchedulerExpression;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    @Override
    public void execute(Map<String, Object> params, MessageQueueManager messageBroker) {
        TaskType type = TaskType.valueOf(params.get("taskType").toString());
        boolean runInQueue = (boolean) params.get("runInQueue");
        ViewerMessage message = new ViewerMessage(type.name());
        params.forEach((key, value) -> {
            message.getProperties().put(key, value.toString());
        });
        if (runInQueue) {
            try {
                messageBroker.addToQueue(message);
            } catch (MessageQueueException e) {
                logger.error("Cannot add job to message queue: {}", e.toString());
            }
        } else {
            messageBroker.handle(message);
        }
    }

}
