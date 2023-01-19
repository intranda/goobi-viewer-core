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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

@WebListener
public class QuartzListener implements ServletContextListener {

    private static final Logger log = LogManager.getLogger(QuartzListener.class);

    /**
     * Restarts timed Jobs
     * 
     * @throws SchedulerException
     */
    public static void restartTimedJobs() throws SchedulerException {
        stopTimedJobs();
        startTimedJobs();
    }

    /**
     * Stops timed updates of HistoryManager
     * 
     * @throws SchedulerException
     */
    private static void stopTimedJobs() throws SchedulerException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
        schedFact.getScheduler().shutdown(false);
    }

    /**
     * Starts timed updates of {@link HistoryAnalyserJob}
     * 
     * @throws SchedulerException
     */
    private static void startTimedJobs() throws SchedulerException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
        Scheduler sched = schedFact.getScheduler();
        sched.start();

        initializeMinutelyJob(new SampleJob(), sched, 5);
    }

    /**
     * initializes given IViewerJob to run every minute
     *
     * @throws SchedulerException
     */
    public static void initializeMinutelyJob(IViewerJob goobiJob, Scheduler sched, int minutes) throws SchedulerException {

        JobDetail jobDetail = JobBuilder.newJob(goobiJob.getClass())

                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(60 * minutes).repeatForever())
                .build();

        sched.scheduleJob(jobDetail, trigger);
    }

    /**
     * initializes given IViewerJob to run every hour
     *
     * @throws SchedulerException
     */
    public static void initializeHourlyJob(IViewerJob goobiJob, Scheduler sched, int hours) throws SchedulerException {

        JobDetail jobDetail = JobBuilder.newJob(goobiJob.getClass())

                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(hours).repeatForever())
                .build();

        sched.scheduleJob(jobDetail, trigger);
    }

    /**
     * initializes given IViewerJob to run every day at midnight
     *
     * @throws SchedulerException
     */
    public static void initializeDailyJob(IViewerJob goobiJob, Scheduler sched) throws SchedulerException {
        initializeCronJob(goobiJob, sched, "0 0 0 * * ?");
    }

    /**
     * execute a given IViewerJob a single time
     * 
     * @throws SchedulerException
     */
    public static void executeJobOnce(IViewerJob goobiJob, Scheduler sched) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(goobiJob.getClass()).withIdentity(goobiJob.getJobName(), goobiJob.getJobName()).build();

        SimpleTrigger trigger =
                (SimpleTrigger) TriggerBuilder.newTrigger().withIdentity(goobiJob.getJobName(), goobiJob.getJobName()).startNow().build();
        sched.scheduleJob(jobDetail, trigger);
    }

    /**
     * initializes given IViewerJob to run at specified times
     * 
     * @param cronConfiguration e.g. "0 0 10 ? * *" for 10 AM every day. The time is based on the server time <br/>
     *            See {@link https://docs.oracle.com/cd/E12058_01/doc/doc.1014/e12030/cron_expressions.htm}
     * 
     * @throws SchedulerException
     */
    public static void initializeCronJob(IViewerJob goobiJob, Scheduler sched, String cronConfiguration) throws SchedulerException {

        JobDetail jobDetail = JobBuilder.newJob(goobiJob.getClass()).withIdentity(goobiJob.getJobName(), goobiJob.getJobName()).build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .withSchedule(CronScheduleBuilder.cronSchedule(cronConfiguration))
                .build();
        sched.scheduleJob(jobDetail, trigger);
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        log.debug("Stop daily JobManager scheduler");
        try {
            stopTimedJobs();
        } catch (SchedulerException e) {
            log.error("Daily JobManager could not be stopped", e);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        log.debug("Start daily JobManager scheduler");
        try {
            startTimedJobs();
        } catch (SchedulerException e) {
            log.error("daily JobManager could not be started", e);
        }
    }

}
