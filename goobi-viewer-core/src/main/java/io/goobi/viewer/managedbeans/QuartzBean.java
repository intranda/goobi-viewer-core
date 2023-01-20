package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import io.goobi.viewer.model.job.quartz.IViewerJob;
import io.goobi.viewer.model.job.quartz.QuartzJobDetails;

@Named
@ApplicationScoped
public class QuartzBean implements Serializable {

    private static final long serialVersionUID = -8294562786947936886L;

    private static final Logger log = LogManager.getLogger(QuartzBean.class);

    private QuartzJobDetails quartzJobDetails;

    private boolean paused;

    private Scheduler scheduler = null;

    public QuartzBean() throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
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

                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                Class<IViewerJob> job = (Class<IViewerJob>) jobDetail.getJobClass();

                //get job's trigger
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                Trigger trigger = triggers.get(0);
                if (trigger instanceof CronTrigger) {
                    CronTrigger cronTrigger = (CronTrigger) trigger;
                    String cronExpr = cronTrigger.getCronExpression();
                    details.setCronExpression(cronExpr);
                }
                if (TriggerState.PAUSED.equals(scheduler.getTriggerState(trigger.getKey()))) {
                    details.setPaused(true);
                }

                Date nextFireTime = trigger.getNextFireTime();
                Date lastFireTime = trigger.getPreviousFireTime();
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
        } catch (SchedulerException e) {
            log.error(e);
        }
        paused = true;
    }

    public void pauseJob() {
        try {
            scheduler.pauseJob(quartzJobDetails.getJobKey());
        } catch (SchedulerException e) {
            log.error(e);
        }
    }

    public void resumeAllJobs() {
        try {
            scheduler.resumeAll();
        } catch (SchedulerException e) {
            log.error(e);
        }
        paused = false;
    }

    public void resumeJob() {
        try {
            scheduler.resumeJob(quartzJobDetails.getJobKey());
        } catch (SchedulerException e) {
            log.error(e);
        }
    }

    public boolean isPaused() {
        return paused;
    }

}
