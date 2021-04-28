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
package io.goobi.viewer.api.rest.model.tasks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.model.SitemapRequestParameters;
import io.goobi.viewer.api.rest.model.ToolsRequestParameters;
import io.goobi.viewer.api.rest.model.tasks.Task.TaskType;
import io.goobi.viewer.api.rest.v1.tasks.TasksResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.search.SearchHitsNotifier;
import io.goobi.viewer.model.sitemap.SitemapBuilder;

/**
 * Manages (possibly timeconsuming) {@link Task tasks} within the viewer which can be triggered and monitored via the {@link TasksResource}. The tasks
 * are not executed sequentially or queued in any way, except through the limit of the internal thread pool (5 parallel tasks)
 * 
 * @author florian
 *
 */
public class TaskManager {

    private final ConcurrentHashMap<Long, Task> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Duration timeToLive;

    /**
     * Create new JobManager
     * 
     * @param jobLiveTime The guaranteed live time of jobs in the jobManager
     */
    public TaskManager(Duration jobLiveTime) {
        this.timeToLive = jobLiveTime;
    }

    public Long addTask(Task job) {
        cleanOldTasks();
        tasks.put(job.id, job);
        return job.id;
    }

    /**
     * Clean out all jobs that are older than {@link #timeToLive}
     */
    private void cleanOldTasks() {
        this.tasks.values()
                .stream()
                .filter(
                        job -> job.timeCreated.isBefore((LocalDateTime.now().minus(timeToLive))))
                .map(job -> job.id)
                .forEach(this::removeTask);
    }

    public Task getTask(long jobId) {
        return tasks.get(jobId);
    }

    public Task removeTask(long jobId) {
        return tasks.remove(jobId);
    }

    public Future triggerTaskInThread(long jobId, HttpServletRequest request) {
        Task job = tasks.get(jobId);
        if (job != null) {
            //            System.out.println("executorService " + executorService.toString());
            return executorService.submit(() -> job.doTask(request));
        }
        return CompletableFuture.completedFuture(null);
    }

    public List<Task> getTasks(TaskType type) {
        return this.tasks.values().stream().filter(job -> job.type == type).collect(Collectors.toList());
    }

    public List<Task> getTasks() {
        return this.tasks.values().stream().collect(Collectors.toList());
    }

    /**
     * @param type
     * @return
     */
    public static BiConsumer<HttpServletRequest, Task> createTask(TaskType type) {
        switch (type) {
            case NOTIFY_SEARCH_UPDATE:
                return (request, job) -> {
                    try {
                        new SearchHitsNotifier().sendNewHitsNotifications();
                    } catch (DAOException | PresentationException | IndexUnreachableException | ViewerConfigurationException e) {
                        job.setError(e.toString());
                    }
                };
            case UPDATE_SITEMAP:
                return (request, job) -> {
                    SitemapRequestParameters params = Optional.ofNullable(job.params)
                            .filter(p -> p instanceof SitemapRequestParameters)
                            .map(p -> (SitemapRequestParameters) p)
                            .orElse(null);
                    try {
                        new SitemapBuilder(request).updateSitemap(params);
                    } catch (IllegalRequestException | AccessDeniedException | JSONException | PresentationException e) {
                        job.setError(e.getMessage());
                    }
                };
            case UPDATE_DATA_REPOSITORY_NAMES:
                return (request, job) -> {
                    ToolsRequestParameters params = Optional.ofNullable(job.params)
                            .filter(p -> p instanceof ToolsRequestParameters)
                            .map(p -> (ToolsRequestParameters) p)
                            .orElse(null);
                    DataManager.getInstance().getSearchIndex().updateDataRepositoryNames(params.getPi(), params.getDataRepositoryName());
                    // Reset access condition and view limit for record
                    DataManager.getInstance().getRecordLockManager().emptyCacheForRecord(params.getPi());
                };
            default:
                return (request, job) -> {
                };
        }
    }
}
