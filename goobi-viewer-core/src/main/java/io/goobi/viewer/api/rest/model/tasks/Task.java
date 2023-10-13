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
package io.goobi.viewer.api.rest.model.tasks;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.goobi.viewer.api.rest.v1.tasks.TasksResource;
import io.goobi.viewer.model.job.TaskType;

/**
 * A process triggered by a REST call using POST and may be monitored via the {@link TasksResource}. Each task has a unique id of type long given
 * during object construcion. A Task has an {@link Accessibility} property defining which calls are allowed to access the task, and a {@link TaskType}
 * defining the actual process to use. Parameters of the execution as well as the type itself are determined by a {@link TaskParameter} object given
 * at task creation. Also each task has a status property signaling he current state of the task. A task starts out as {@link TaskStatus#CREATED}.
 * Once processing starts (which may be delayed by the limited thread pool if other tasks are running) the status changes to
 * {@link TaskStatus#STARTED}. After processing ends the task is set to either {@link TaskStatus#COMPLETE} or {@link TaskStatus#ERROR} depedning on
 * whether an error occured which may be recorded in the {@link #exception} property.
 *
 * @author florian
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class Task {

    private static final Logger logger = LogManager.getLogger(Task.class);
    private static final AtomicLong idCounter = new AtomicLong(0);

    public enum Accessibility {
        /**
         * Anyone may access this task
         */
        PUBLIC,
        /**
         * Anyone may access this tasks, but they are only visible within the session in which they were created
         */
        SESSION,
        /**
         * Only users with admin rights may access this task
         */
        ADMIN,
        /**
         * This task is only accessibly by requests containing a valid access token
         */
        TOKEN;
    }

    public enum TaskStatus {
        CREATED,
        STARTED,
        COMPLETE,
        ERROR
    }

    public final long id;
    public final TaskType type;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    public final LocalDateTime timeCreated;
    @JsonIgnore
    public final BiConsumer<HttpServletRequest, Task> work;
    private volatile TaskStatus status;
    @JsonIgnore
    public Optional<String> exception = Optional.empty();
    @JsonIgnore
    public Optional<String> sessionId = Optional.empty();
    @JsonIgnore
    public final TaskParameter params;

    public Task(TaskParameter params, BiConsumer<HttpServletRequest, Task> work) {
        this.type = params.getType();
        this.work = work;
        this.id = idCounter.incrementAndGet();
        this.timeCreated = LocalDateTime.now();
        this.status = TaskStatus.CREATED;
        this.params = params;
    }

    public void doTask(HttpServletRequest request) {
        logger.debug("Started Task '{}'", this);
        this.sessionId = Optional.ofNullable(request).map(r -> r.getSession().getId());
        this.status = TaskStatus.STARTED;
        this.work.accept(request, this);
        if (TaskStatus.ERROR != this.status) {
            this.status = TaskStatus.COMPLETE;
        }
        logger.debug("Finished Task '{}'", this);
    }

    public void setError(String error) {
        this.status = TaskStatus.ERROR;
        this.exception = Optional.ofNullable(error);
    }

    /**
     * 
     * @param type
     * @return
     */
    public static Accessibility getAccessibility(TaskType type) {
        switch (type) {
            case NOTIFY_SEARCH_UPDATE:
            case PURGE_EXPIRED_DOWNLOAD_TICKETS:
            case UPDATE_SITEMAP:
            case UPDATE_DATA_REPOSITORY_NAMES:
            case UPDATE_UPLOAD_JOBS:
            case INDEX_USAGE_STATISTICS:
            case PRERENDER_PDF:
                return Accessibility.TOKEN;
            case SEARCH_EXCEL_EXPORT:
                return Accessibility.SESSION;
            default:
                return Accessibility.ADMIN;
        }
    }

    /**
     * @return the status
     */
    public TaskStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return exception.orElse(null);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Task " + this.id + "; Type: " + this.type + "; Status: " + this.status;
    }

}
