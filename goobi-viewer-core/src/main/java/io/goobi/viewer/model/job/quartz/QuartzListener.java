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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.apache.commons.lang3.StringUtils;
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

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.mq.GeoMapUpdateHandler;

@WebListener
public class QuartzListener implements ServletContextListener {

    public static final String QUARTZ_LISTENER_CONTEXT_ATTRIBUTE = "io.goobi.viewer.model.job.quartz.QuartzListener";

    private static final Logger log = LogManager.getLogger(QuartzListener.class);

    private static final String DEFAULT_SCHEDULER_EXPRESSION = "0 0 0 * * ?";

    private final IDAO dao;
    private final Configuration config;
    @Inject
    private MessageQueueManager messageBroker;
    @Inject
    private ServletContext context;

    public QuartzListener() throws DAOException {
        this.dao = DataManager.getInstance().getDao();
        this.config = DataManager.getInstance().getConfiguration();
    }

    public QuartzListener(IDAO dao, Configuration config, MessageQueueManager messageBroker) {
        this.dao = dao;
        this.config = config;
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
     * 
     * @param servletContext
     * 
     * @throws SchedulerException
     */
    private void startTimedJobs(ServletContext servletContext) throws SchedulerException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
        Scheduler sched = schedFact.getScheduler();
        sched.start();
        sched.getContext().put("messageBroker", messageBroker);
        try {
            List<RecurringTaskTrigger> triggers = loadOrCreateTriggers();
            for (RecurringTaskTrigger trigger : triggers) {

                //first check trigger cron expression and update it if necessary
                String cronExpression = config.getQuartzSchedulerCronExpression(trigger.getTaskType());
                if (!StringUtils.equals(trigger.getScheduleExpression(), cronExpression)) {
                    trigger.setScheduleExpression(cronExpression);
                    this.dao.updateRecurringTaskTrigger(trigger);
                }

                //Initialize CronJob
                HandleMessageJob job = new HandleMessageJob(TaskType.valueOf(trigger.getTaskType()), trigger.getScheduleExpression(), messageBroker);
                JobDetail jobDetail = initializeCronJob(job, sched);
                Map<String, Object> params = getParams(job.getTaskType(), true, servletContext);
                sched.getContext().put(jobDetail.getKey().getName(), params);

                //set to pause depending on stored trigger status
                if (TaskTriggerStatus.PAUSED.equals(trigger.getStatus())) {
                    sched.pauseJob(jobDetail.getKey());
                }
            }
        } catch (DAOException e) {
            throw new SchedulerException(e);
        }
    }

    private Map<String, Object> getParams(TaskType taskType, boolean runInQueue, ServletContext servletContext) {
        Map<String, Object> params = new HashMap<>();
        params.put("taskType", taskType.toString());
        params.put("runInQueue", runInQueue);
        switch (taskType) {
            case UPDATE_SITEMAP:
                String rootUrl = this.config.getViewerBaseUrl();
                String realPath = servletContext.getRealPath("/");
                params.put("viewerRootUrl", rootUrl);
                params.put("baseurl", realPath);
                break;
            default:
                break;
        }
        return params;
    }

    private List<RecurringTaskTrigger> loadOrCreateTriggers() throws DAOException {
        Map<String, RecurringTaskTrigger> storedTriggers =
                dao.getRecurringTaskTriggers().stream().collect(Collectors.toMap(RecurringTaskTrigger::getTaskType, Function.identity()));
        List<RecurringTaskTrigger> triggers = new ArrayList<>();

        addTrigger(storedTriggers, triggers, TaskType.INDEX_USAGE_STATISTICS, TaskTriggerStatus.RUNNING);
        addTrigger(storedTriggers, triggers, TaskType.NOTIFY_SEARCH_UPDATE, TaskTriggerStatus.RUNNING);
        addTrigger(storedTriggers, triggers, TaskType.PURGE_EXPIRED_DOWNLOAD_TICKETS, TaskTriggerStatus.RUNNING);
        addTrigger(storedTriggers, triggers, TaskType.UPDATE_SITEMAP, TaskTriggerStatus.RUNNING);
        addTrigger(storedTriggers, triggers, TaskType.UPDATE_UPLOAD_JOBS, TaskTriggerStatus.RUNNING);
        if (GeoMapUpdateHandler.shouldUpdateGeomaps()) {
            addTrigger(storedTriggers, triggers, TaskType.CACHE_GEOMAPS, TaskTriggerStatus.RUNNING);
        } else if (storedTriggers.containsKey(TaskType.CACHE_GEOMAPS.name())) {
            dao.deleteRecurringTaskTrigger(storedTriggers.get(TaskType.CACHE_GEOMAPS.name()).getId());
        }
        return triggers;
    }

    public void addTrigger(Map<String, RecurringTaskTrigger> storedTriggers, List<RecurringTaskTrigger> triggers, TaskType taskType,
            TaskTriggerStatus defaultStatus)
            throws DAOException {
        if (storedTriggers.containsKey(taskType.name())) {
            triggers.add(storedTriggers.get(taskType.name()));
        } else {
            RecurringTaskTrigger trigger = new RecurringTaskTrigger(taskType, DEFAULT_SCHEDULER_EXPRESSION);
            trigger.setStatus(defaultStatus);
            triggers.add(trigger);
            dao.addRecurringTaskTrigger(trigger);
        }
    }

    /**
     * initializes given IViewerJob to run every minute
     *
     * @param goobiJob
     * @param sched
     * @param minutes
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
     * @param goobiJob
     * @param sched
     * @param hours
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
     * @param goobiJob
     * @param sched
     * @throws SchedulerException
     */
    public static void initializeDailyJob(IViewerJob goobiJob, Scheduler sched) throws SchedulerException {

        JobDetail jobDetail = generateJob(goobiJob);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .withSchedule(CronScheduleBuilder.cronSchedule(DEFAULT_SCHEDULER_EXPRESSION))
                .build();
        sched.scheduleJob(jobDetail, trigger);
    }

    /**
     * execute a given IViewerJob a single time
     * 
     * @param goobiJob
     * @param sched
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
     * @param goobiJob
     * @param sched
     * @return {@link JobDetail}
     * @throws SchedulerException
     */
    public static JobDetail initializeCronJob(IViewerJob goobiJob, Scheduler sched) throws SchedulerException {

        JobDetail jobDetail = generateJob(goobiJob);
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(goobiJob.getJobName(), goobiJob.getJobName())
                .withSchedule(CronScheduleBuilder.cronSchedule(goobiJob.getCronExpression()))
                .build();
        sched.scheduleJob(jobDetail, trigger);
        return jobDetail;
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        try {
            stopTimedJobs();
            log.info("Successfully stopped QuartzListener scheduler");
        } catch (SchedulerException e) {
            log.error("QuartzListener scheduler could not be stopped", e);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent evt) {
        try {
            startTimedJobs(evt.getServletContext());
            Optional.ofNullable(evt)
                    .map(ServletContextEvent::getServletContext)
                    .ifPresent(cntxt -> cntxt.setAttribute(QUARTZ_LISTENER_CONTEXT_ATTRIBUTE, this));
            log.info("Successfully started QuartzListener scheduler");
        } catch (SchedulerException e) {
            log.error("QuartzListener scheduler could not be started", e);
        }
    }

}
