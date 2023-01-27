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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import io.goobi.viewer.controller.mq.MessageBroker;

public abstract class AbstractViewerJob implements Job, IViewerJob {
    private static boolean running = false;

    private static final Logger log = LogManager.getLogger(AbstractViewerJob.class);

    protected AbstractViewerJob() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.goobi.production.flow.jobs.IGoobiJob#execute(org.quartz.
     * JobExecutionContext)
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // execute job only, if no other instance is running
        if (!running) {
            log.trace("Start scheduled Job: " + getJobName());
            if (!running) {
                setRunning(true);
                try {
                    log.trace("start history updating for all processes");
                    setRunning(true);
                    MessageBroker broker = (MessageBroker) context.getScheduler().getContext().get("messageBroker");
                    String keyName = context.getJobDetail().getKey().getName();

                    Map<String, Object> params = (Map<String, Object>) context.getScheduler().getContext().get(keyName);
                    execute(params, broker);
                    setRunning(false);
                } catch (SchedulerException e) {
                    log.error(e);
                } finally {
                    setRunning(false);
                }
            }
            log.trace("End scheduled Job: " + getJobName());
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setRunning(boolean running) {
        this.running = running;
    }
}
