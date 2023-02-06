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

import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.goobi.viewer.controller.mq.MessageQueueManager;
import it.burning.cron.CronExpressionDescriptor;

public interface IViewerJob extends Job {

    @Override
    public abstract void execute(JobExecutionContext context) throws JobExecutionException;

    public abstract void setRunning(String jobName, boolean running);

    public abstract boolean isRunning(String jobName);

    public abstract String getJobName();

    public abstract void execute(Map<String, Object> params, MessageQueueManager messageBroker);

    default String getCronExpression() {
        return "";
    }

    default String getHumanReadableCronTime() {
        if (StringUtils.isBlank(getCronExpression())) {
            return "";
        }
        return CronExpressionDescriptor.getDescription(getCronExpression());
    }

}