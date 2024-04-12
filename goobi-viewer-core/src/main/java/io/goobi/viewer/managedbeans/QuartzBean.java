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

package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.quartz.QuartzJobDetails;
import io.goobi.viewer.model.job.quartz.RecurringTaskTrigger;
import io.goobi.viewer.model.job.quartz.TaskTriggerStatus;

@Named
@ApplicationScoped
public class QuartzBean implements Serializable {

    private static final long serialVersionUID = -8294562786947936886L;

    private static final Logger log = LogManager.getLogger(QuartzBean.class);

    private QuartzJobDetails quartzJobDetails;

    private boolean paused;

    private Scheduler scheduler = null;
    
    @Inject
    private AdminDeveloperBean developerBean;

    public QuartzBean() throws SchedulerException {
        this.reset();
    }

    private void initializePausedState() throws SchedulerException {
        this.paused = true;
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                Trigger trigger = scheduler.getTriggersOfJob(jobKey).get(0);
                if (!TriggerState.PAUSED.equals(scheduler.getTriggerState(trigger.getKey()))) {
                    this.paused = false;
                    break;
                }
            }
        }
    }
    
    public void reset() throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
        initializePausedState();
    }

    public List<QuartzJobDetails> getActiveJobs() throws SchedulerException {
        List<QuartzJobDetails> activeJobs = new ArrayList<>();
        for (String groupName : scheduler.getJobGroupNames()) {

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                QuartzJobDetails details = new QuartzJobDetails();
                activeJobs.add(details);

                details.setJobKey(jobKey);

                String jobName = jobKey.getName();
                details.setJobName(jobName);

                String jobGroup = jobKey.getGroup();
                details.setJobGroup(jobGroup);

                //get job's trigger
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                Trigger trigger = triggers.get(0);
                if (trigger instanceof CronTrigger) {
                    CronTrigger cronTrigger = (CronTrigger) trigger;
                    String cronExpr = cronTrigger.getCronExpression();
                    details.setCronExpression(cronExpr);
                }
                if (this.paused || TriggerState.PAUSED.equals(scheduler.getTriggerState(trigger.getKey()))) {
                    details.setPaused(true);
                }

                LocalDateTime nextFireTime = DateTools.convertDateToLocalDateTimeViaInstant(trigger.getNextFireTime());
                LocalDateTime lastFireTime = DateTools.convertDateToLocalDateTimeViaInstant(trigger.getPreviousFireTime());
                details.setNextFireTime(nextFireTime);
                details.setPreviousFireTime(lastFireTime);
            }

        }

        return activeJobs;
    }

    public QuartzJobDetails getQuartzJobDetails() {
        return quartzJobDetails;
    }

    public void setQuartzJobDetails(QuartzJobDetails quartzJobDetails) {
        this.quartzJobDetails = quartzJobDetails;
    }

    public void triggerQuartzJob() {
        try {
            scheduler.triggerJob(quartzJobDetails.getJobKey());
        } catch (SchedulerException e) {
            log.error(e);
        }

    }

    public void pauseAllJobs() {
        try {
            scheduler.pauseAll();
            getActiveJobs().forEach(job -> persistTriggerStatus(job.getJobName(), TaskTriggerStatus.PAUSED));
        } catch (SchedulerException e) {
            log.error(e);
        }
        paused = true;
    }

    public void pauseJob() {
        try {
            scheduler.pauseJob(quartzJobDetails.getJobKey());
            persistTriggerStatus(quartzJobDetails.getJobName(), TaskTriggerStatus.PAUSED);
            initializePausedState();
        } catch (SchedulerException e) {
            log.error(e);
        }
    }

    private void persistTriggerStatus(String jobName, TaskTriggerStatus status) {
        try {
            IDAO dao = DataManager.getInstance().getDao();
            RecurringTaskTrigger trigger = dao.getRecurringTaskTriggerForTask(TaskType.valueOf(jobName));
            trigger.setStatus(status);
            dao.updateRecurringTaskTrigger(trigger);
        } catch (DAOException e) {
            log.error(e);
        }
    }

    public void resumeAllJobs() {
        try {
            scheduler.resumeAll();
            getActiveJobs().forEach(job -> persistTriggerStatus(job.getJobName(), TaskTriggerStatus.RUNNING));
        } catch (SchedulerException e) {
            log.error(e);
        }
        paused = false;
    }

    public void resumeJob() {
        try {
            scheduler.resumeJob(quartzJobDetails.getJobKey());
            persistTriggerStatus(quartzJobDetails.getJobName(), TaskTriggerStatus.RUNNING);
            this.paused = false;
        } catch (SchedulerException e) {
            log.error(e);
        }
    }

    public boolean isPaused() {
        return paused;
    }

}
