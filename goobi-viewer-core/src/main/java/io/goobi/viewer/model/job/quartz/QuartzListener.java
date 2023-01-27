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

import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;
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

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageBroker;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.job.TaskType;

@WebListener
public class QuartzListener implements ServletContextListener {

    private static final Logger log = LogManager.getLogger(QuartzListener.class);

    private static final String DEFAULT_SCHEDULER_EXPRESSION = "0 0 0 * * ?";
    
    private final IDAO dao;
    @Inject 
    transient private MessageBroker messageBroker;
    @Inject
    private ServletContext context;
    
    public QuartzListener() throws DAOException {
        this.dao = DataManager.getInstance().getDao();
    }
    
    public QuartzListener(IDAO dao, MessageBroker messageBroker) {
        this.dao = dao;
        this.messageBroker = messageBroker;
    }
    
    /**
     * Restarts timed Jobs
     * 
     * @throws SchedulerException
     */
    public void restartTimedJobs() throws SchedulerException {
        stopTimedJobs();
        startTimedJobs(this.context);
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
     * @param servletContext 
     * 
     * @throws SchedulerException
     */
    private void startTimedJobs(ServletContext servletContext) throws SchedulerException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
        Scheduler sched = schedFact.getScheduler();
        sched.start();

        try {
            List<RecurringTaskTrigger> triggers = loadOrCreateTriggers();
            for (RecurringTaskTrigger trigger : triggers) {
                HandleMessageJob job = new HandleMessageJob(TaskType.valueOf(trigger.getTaskType()), trigger.getScheduleExpression() ,messageBroker);
                
                addParams(job, servletContext);
                
                initializeCronJob(job, sched);
            }
        } catch (DAOException e) {
            throw new SchedulerException(e);
        }
        
        initializeMinutelyJob(new SampleJob(), sched, 1);

//        initializeCronJob(new NotifySearchUpdateJob(), sched);
//        initializeCronJob(new IndexUsageSstatisticsJob(), sched);
//        initializeCronJob(new PurgeExpiredDownloadTicketsJob(), sched);
//        initializeCronJob(new UpdateUploadJobsJob(), sched);
    }

    private void addParams(HandleMessageJob job, ServletContext servletContext) {
        switch(job.getTaskType()) {
            case UPDATE_SITEMAP:
                String contextPath = servletContext.getContextPath();
                String realPath =  servletContext.getRealPath("/");
                job.setParam("viewerRootUrl", contextPath);
                job.setParam("baseurl", realPath);
                break;
            default:
                break;
        }
    }

    private List<RecurringTaskTrigger> loadOrCreateTriggers() throws DAOException {
        List<RecurringTaskTrigger> triggers = dao.getRecurringTaskTriggers();
        if(triggers.isEmpty()) {
            triggers.add(new RecurringTaskTrigger(TaskType.INDEX_USAGE_STATISTICS, DEFAULT_SCHEDULER_EXPRESSION));
            triggers.add(new RecurringTaskTrigger(TaskType.NOTIFY_SEARCH_UPDATE, DEFAULT_SCHEDULER_EXPRESSION));
            triggers.add(new RecurringTaskTrigger(TaskType.PURGE_EXPIRED_DOWNLOAD_TICKETS, DEFAULT_SCHEDULER_EXPRESSION));
            triggers.add(new RecurringTaskTrigger(TaskType.UPDATE_SITEMAP, DEFAULT_SCHEDULER_EXPRESSION));
            triggers.add(new RecurringTaskTrigger(TaskType.UPDATE_UPLOAD_JOBS, DEFAULT_SCHEDULER_EXPRESSION));
            for (RecurringTaskTrigger trigger : triggers) {
                dao.addRecurringTaskTrigger(trigger);
            }
        }
        return triggers;
    }
    
    /**
     * initializes given IViewerJob to run every minute
     *
     * @throws SchedulerException
     */
    public static void initializeMinutelyJob(IViewerJob goobiJob, Scheduler sched, int minutes) throws SchedulerException {

        JobDetail jobDetail = generateJob(goobiJob);

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

        JobDetail jobDetail = generateJob(goobiJob);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(hours).repeatForever())
                .build();

        sched.scheduleJob(jobDetail, trigger);
    }

    private static JobDetail generateJob(IViewerJob goobiJob) {
        return JobBuilder.newJob(goobiJob.getClass()).withIdentity(goobiJob.getJobName(), goobiJob.getJobName()).build();
    }

    /**
     * initializes given IViewerJob to run every day at midnight
     *
     * @throws SchedulerException
     */
    public static void initializeDailyJob(IViewerJob goobiJob, Scheduler sched) throws SchedulerException {

        JobDetail jobDetail = generateJob(goobiJob);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
                .build();
        sched.scheduleJob(jobDetail, trigger);
    }

    /**
     * execute a given IViewerJob a single time
     * 
     * @throws SchedulerException
     */
    public static void executeJobOnce(IViewerJob goobiJob, Scheduler sched) throws SchedulerException {
        JobDetail jobDetail = generateJob(goobiJob);

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
    public static void initializeCronJob(IViewerJob goobiJob, Scheduler sched) throws SchedulerException {

        JobDetail jobDetail = generateJob(goobiJob);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .withSchedule(CronScheduleBuilder.cronSchedule(goobiJob.getCronExpression()))
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
    public void contextInitialized(ServletContextEvent evt) {
        log.debug("Start daily JobManager scheduler");
        try {
            startTimedJobs(evt.getServletContext());
        } catch (SchedulerException e) {
            log.error("daily JobManager could not be started", e);
        }
    }

}
