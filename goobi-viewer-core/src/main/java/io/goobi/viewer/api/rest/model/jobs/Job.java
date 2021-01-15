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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/**
 * A process triggered by a REST call with PUT 
 * 
 * @author florian
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class Job {
    
    private static final AtomicLong idCounter = new AtomicLong(0);

    public static enum Accessibility {
        PUBLIC,
        SESSION,
        ADMIN,
        TOKEN;
    }
    
    public static enum JobType {
        /**
         * Send emails to all search owners if their searches have changed results
         */
        NOTIFY_SEARCH_UPDATE,
        /**
         * Handle asynchonous generation of excel sheets with search results
         */
        SEARCH_EXCEL_EXPORT,
        /**
         * Update the sitemap 
         */
        UPDATE_SITEMAP,
        /**
         * Update data repositories
         */
        UPDATE_DATA_REPOSITORIES;
    }
    
    public static enum JobStatus {
        CREATED,
        STARTED,
        COMPLETE,
        ERROR
    }
    
    public final long id;
    public final JobType type;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    public final LocalDateTime timeCreated;
    @JsonIgnore
    public final BiConsumer<HttpServletRequest, Job> task;
    public volatile JobStatus status;
    public Optional<String> exception = Optional.empty();
    @JsonIgnore
    public Optional<String> sessionId = Optional.empty();
    @JsonIgnore
    public final SimpleJobParameter params;
    
    public Job(SimpleJobParameter params, BiConsumer<HttpServletRequest, Job> task) {
        this.type = params.type;
        this.task = task;
        this.id = idCounter.incrementAndGet();
        this.timeCreated = LocalDateTime.now();
        this.status = JobStatus.CREATED;
        this.params = params;
    }

    
    public void doTask(HttpServletRequest request) {
        this.sessionId = Optional.ofNullable(request.getSession().getId());
        this.status = JobStatus.STARTED;
        this.task.accept(request, this);
        if(JobStatus.ERROR != this.status) {
            this.status = JobStatus.COMPLETE;
        }
    }
    
    public void setError(String error) {
        this.status = JobStatus.ERROR;
        this.exception = Optional.ofNullable(error);
    }
    
    public static Accessibility getAccessibility(JobType type) {
        switch(type) {
            case NOTIFY_SEARCH_UPDATE: 
            case UPDATE_SITEMAP:
                return Accessibility.TOKEN;
            case SEARCH_EXCEL_EXPORT:
                return Accessibility.SESSION;
            default:
                return Accessibility.ADMIN;
        }
    }
    
}
