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
package io.goobi.viewer.api.rest.v1.jobs;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.filters.AdminLoggedInFilter;
import io.goobi.viewer.api.rest.filters.AuthorizationFilter;
import io.goobi.viewer.api.rest.model.jobs.Job;
import io.goobi.viewer.api.rest.model.jobs.Job.JobType;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.AuthenticationException;
import io.goobi.viewer.api.rest.model.jobs.JobDescription;
import io.goobi.viewer.api.rest.model.jobs.JobManager;

/**
 * @author florian
 *
 */
@Path(JOBS)
public class JobResource {

    private final HttpServletRequest request;

    public JobResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        this.request = request;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Job addJob(JobDescription desc) throws IllegalRequestException, AuthenticationException {
        if(desc.type == null) {
            throw new IllegalRequestException("Must provide job type");
        }
        if(isAuthorized(desc.type, request)) {
            Job job = new Job(desc.type, JobManager.createTask(desc.type));
            job.doTask(request);
            return job;
        } else {
            throw new AuthenticationException("Not authorized to create this type of job");
        }
    }
    
    @GET
    @Path(JOBS_JOB)
    @Produces({ MediaType.APPLICATION_JSON })
    public Job getJob(@PathParam("id")Long id) throws ContentNotFoundException {
        Job job = DataManager.getInstance().getRestApiJobManager().getJob(id);
        if(job == null || !isAuthorized(job.type, request)) {
            throw new ContentNotFoundException("No Job found with id " + id);
        } else {
            return job;
        }
    }

    public boolean isAuthorized(JobType type, HttpServletRequest request) {
        Job.ACCESSIBILITY access = Job.getAccessibility(type);
        switch (access) {
            case PUBLIC:
                return true;
            case TOKEN:
                return AuthorizationFilter.isAuthorized(request);
            case ADMIN:
                return AdminLoggedInFilter.isAdminLoggedIn(request);
            default:
                return false;
        }
    }
}
