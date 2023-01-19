package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import io.goobi.viewer.model.job.quartz.QuartzJobDetails;

@Named
@ApplicationScoped
public class QuartzBean implements Serializable {

    private static final long serialVersionUID = -8294562786947936886L;

    private static final Logger log = LogManager.getLogger(QuartzBean.class);

    private QuartzJobDetails quartzJobDetails;
    private List<QuartzJobDetails> activeJobs = new ArrayList<>();;

    public void initJobList() throws SchedulerException {

        Scheduler scheduler = new StdSchedulerFactory().getScheduler();

        for (String groupName : scheduler.getJobGroupNames()) {

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                QuartzJobDetails details = new QuartzJobDetails();
                activeJobs.add(details);

                String jobName = jobKey.getName();
                details.setJobName(jobName);

                String jobGroup = jobKey.getGroup();
                details.setJobGroup(jobGroup);

                JobDetail detail = scheduler.getJobDetail(jobKey);

                Class<? extends Job> clazz = detail.getJobClass();
                details.setClazz(clazz);

                //get job's trigger
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                Date nextFireTime = triggers.get(0).getNextFireTime();
                Date lastFireTime = triggers.get(0).getPreviousFireTime();
                details.setNextFireTime(nextFireTime);
                details.setPreviousFireTime(lastFireTime);
            }

        }
    }

    public List<QuartzJobDetails> getActiveJobs() {

        if (activeJobs.isEmpty()) {
            try {
                initJobList();
            } catch (SchedulerException e) {
                log.error(e);
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
        System.out.println(quartzJobDetails.getJobName());

    }

}
