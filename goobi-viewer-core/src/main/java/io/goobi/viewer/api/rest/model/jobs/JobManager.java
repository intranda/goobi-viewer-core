/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.model.jobs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import io.goobi.viewer.api.rest.model.jobs.Job.JobType;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.search.SearchHitsNotifier;

/**
 * @author florian
 *
 */
public class JobManager {
    
    
    private final ConcurrentHashMap<Long, Job> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final  Duration timeToLive;
    
    /**
     * Create new JobManager
     * 
     * @param jobLiveTime   The guaranteed live time of jobs in the jobManager
     */
    public JobManager(Duration jobLiveTime) {
        this.timeToLive = jobLiveTime;
    }
    
    public Long addJob(Job job) {
        cleanOldJobs();
        jobs.put(job.id, job);
        return job.id;
    }

    /**
     * Clean out all jobs that are older than {@link #timeToLive}
     */
    private void cleanOldJobs() {
        this.jobs.values().stream().filter(
                job -> job.timeCreated.isBefore((LocalDateTime.now().minus(timeToLive))))
        .map(job -> job.id)
        .forEach(this::removeJob);
    }

    public Job getJob(long jobId) {
        return jobs.get(jobId);
    }
    
    public Job removeJob(long jobId) {
        return jobs.remove(jobId);
    }
    
    public Job triggerJobInThread(long jobId, HttpServletRequest request) {
        Job job = jobs.get(jobId);
        if(job != null) {
            executorService.execute(() -> job.doTask(request));
        }
        return job;
    }
    
    public List<Job> getJobs(JobType type) {
        return this.jobs.values().stream().filter(job -> job.type == type).collect(Collectors.toList());
    }

    /**
     * @param type
     * @return
     */
    public static BiConsumer<HttpServletRequest, Job> createTask(JobType type) {
        switch(type) {
            case NOTIFY_SEARCH_UPDATE:
                return (request, job) -> {
                    try {
                        new SearchHitsNotifier().sendNewHitsNotifications();
                    } catch (DAOException | PresentationException | IndexUnreachableException | ViewerConfigurationException e) {
                        job.setError(e.toString());
                    }
                };
            default:
                return (request, job) -> {};
        }
    }
}
