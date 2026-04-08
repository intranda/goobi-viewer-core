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
package io.goobi.viewer.api.rest.v1.tasks;

import static io.goobi.viewer.api.rest.v1.ApiUrls.TASKS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.TASKS_TASK;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import io.goobi.viewer.api.rest.filters.AdminLoggedInFilter;
import io.goobi.viewer.api.rest.filters.AuthorizationFilter;
import io.goobi.viewer.api.rest.model.PrerenderPdfsRequestParameters;
import io.goobi.viewer.api.rest.model.SitemapRequestParameters;
import io.goobi.viewer.api.rest.model.ToolsRequestParameters;
import io.goobi.viewer.api.rest.model.tasks.Task;
import io.goobi.viewer.api.rest.model.tasks.TaskManager;
import io.goobi.viewer.api.rest.model.tasks.TaskParameter;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.model.job.ITaskType;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for creating and monitoring asynchronous {@link io.goobi.viewer.api.rest.model.tasks.TaskManager} tasks within the viewer, such as PDF
 * pre-rendering.
 *
 * @author Florian Alpers
 */
@Path(TASKS)
@ViewerRestServiceBinding
public class TasksResource {

    private static final Logger logger = LogManager.getLogger(TasksResource.class);
    private final HttpServletRequest request;

    @Context
    private HttpServletRequest httpRequest;
    @Inject
    private MessageQueueManager messageBroker;
    @Context
    private IDAO dao;

    public TasksResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        this.request = request;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "tasks" }, summary = "Create a (possibly time consuming) task to execute in a limited thread pool. See javadoc for details")
    @ApiResponse(responseCode = "200", description = "Task has been accepted and started")
    @ApiResponse(responseCode = "400", description = "No task type provided or task type is invalid")
    @ApiResponse(responseCode = "401", description = "Not authorized to create this type of task")
    @ApiResponse(responseCode = "500", description = "Message queue unavailable or internal error")
    // Explicit @Content required to produce a valid OpenAPI requestBody with a content map;
    // @RequestBody(required=true) alone generates {"required":true} which is invalid per OpenAPI 3.
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TaskParameter.class)))
    public Response addTask(TaskParameter desc) throws WebApplicationException {
        if (desc == null || desc.getType() == null) {
            throw new WebApplicationException(new IllegalRequestException("Must provide job type"));
        }
        if (!isAuthorized(desc.getType(), Optional.empty(), request)) {
            throw new WebApplicationException(new AccessDeniedException("Not authorized to create this type of job"));
        }

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            ViewerMessage message = null;
            message = new ViewerMessage(desc.getType().name());
            message.getProperties().put("taskType", desc.getType().toString());
            switch (desc.getType()) {

                case UPDATE_SITEMAP:
                    SitemapRequestParameters params = Optional.ofNullable(desc)
                            .filter(SitemapRequestParameters.class::isInstance)
                            .map(SitemapRequestParameters.class::cast)
                            .orElse(null);

                    String viewerRootUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(request);
                    String outputPath = request.getServletContext().getRealPath("/");
                    if (params != null && StringUtils.isNotBlank(params.getOutputPath())) {
                        outputPath = params.getOutputPath();
                    }
                    message.getProperties().put("viewerRootUrl", viewerRootUrl);
                    message.getProperties().put("baseurl", outputPath);
                    break;

                case UPDATE_DATA_REPOSITORY_NAMES:
                    ToolsRequestParameters tools = Optional.ofNullable(desc)
                            .filter(ToolsRequestParameters.class::isInstance)
                            .map(ToolsRequestParameters.class::cast)
                            .orElse(null);
                    if (tools == null) {
                        return null;
                    }
                    String identifier = tools.getPi();
                    String dataRepositoryName = tools.getDataRepositoryName();
                    message.getProperties().put("identifier", identifier);
                    message.getProperties().put("dataRepositoryName", dataRepositoryName);
                    break;

                case PRERENDER_PDF:
                    PrerenderPdfsRequestParameters prerenderParams = Optional.ofNullable(desc)
                            .filter(PrerenderPdfsRequestParameters.class::isInstance)
                            .map(PrerenderPdfsRequestParameters.class::cast)
                            .orElse(null);
                    if (prerenderParams == null) {
                        return null;
                    }
                    String pi = prerenderParams.getPi();
                    String variant = Optional.ofNullable(prerenderParams.getVariant()).orElse(ContentServerConfiguration.DEFAULT_CONFIG_VARIANT);
                    boolean force = Optional.ofNullable(prerenderParams.getForce()).orElse(false);
                    message.getProperties().put("pi", pi);
                    message.getProperties().put("variant", variant);
                    message.getProperties().put("force", Boolean.toString(force));
                    break;

                default:
                    // unknown type
                    return null;
            }

            try {
                String messageId = this.messageBroker.addToQueue(message);
                message.setMessageId(messageId);
            } catch (MessageQueueException e) {
                throw new WebApplicationException(e);
            }

            // TODO create useful response, containing the message id
            return Response.ok(message).build();
        }
        Task job = new Task(desc, TaskManager.createTask(desc.getType()));
        logger.debug("Created new task REST API task '{}'", job);
        DataManager.getInstance().getRestApiJobManager().addTask(job);
        DataManager.getInstance().getRestApiJobManager().triggerTaskInThread(job.getId(), request);
        return Response.ok(job).build();
    }

    @GET
    @Path(TASKS_TASK)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "tasks" },
            summary = "Return the task with the given id, provided it is accessible by the request (determined by session or access token)")
    @ApiResponse(responseCode = "200", description = "The task with the given id")
    // 400 is returned when the id contains characters not allowed by the schema pattern ^[A-Za-z0-9_-]+$
    @ApiResponse(responseCode = "400", description = "Invalid task id — must match ^[A-Za-z0-9_-]+$")
    @ApiResponse(responseCode = "404", description = "No task found for the given id, or the request is not authorized to access it")
    public Response getTask(@Parameter(description = "The id of the task",
            schema = @Schema(pattern = "^[A-Za-z0-9_-]+$")) @PathParam("id") String id) throws ContentNotFoundException {

        // Enforce the pattern documented in the schema to return 400 instead of accepting
        // IDs with special characters (e.g. semicolons, non-ASCII) that would result in
        // empty results (200) or unexpected database queries.
        if (id == null || !id.matches("[A-Za-z0-9_-]+")) {
            throw new WebApplicationException(
                    jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                            .entity("{\"status\":400,\"message\":\"Invalid task id: must match ^[A-Za-z0-9_-]+$\"}")
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }

        if (id.matches("\\d+")) {
            Long idLong = Long.parseLong(id);
            Task job = DataManager.getInstance().getRestApiJobManager().getTask(idLong);
            if (job == null || !isAuthorized(job.getType(), job.getSessionId(), request)) {
                throw new ContentNotFoundException("No Job found with id " + id);
            }
            return Response.ok(job).build();
        }
        // Guard against a null broker (message queue not configured) to avoid NPE → HTTP 500.
        ViewerMessage message = (this.messageBroker != null)
                ? this.messageBroker.getMessageById(id).orElse(getMessageFromDAO(id).orElse(null))
                : getMessageFromDAO(id).orElse(null);
        if (message != null && isAuthorized(getTaskType(message.getTaskName()).orElse(null), Optional.empty(), request)) {
            return Response.ok(message).build();
        }
        throw new ContentNotFoundException("No Job found with id " + id);
    }

    private static Optional<ITaskType> getTaskType(String taskName) {
        try {
            return Optional.ofNullable(TaskType.getByName(taskName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<ViewerMessage> getMessageFromDAO(String messageId) {
        try {
            return Optional.ofNullable(dao.getViewerMessageByMessageID(messageId));
        } catch (DAOException e) {
            return Optional.empty();
        } catch (jakarta.persistence.PersistenceException e) {
            // DB collation or other persistence errors (e.g. utf8mb3 vs utf8mb4 mismatch
            // when message_id contains 4-byte Unicode characters) must not propagate as HTTP 500.
            logger.warn("DB error looking up message '{}': {}", messageId, e.getMessage());
            return Optional.empty();
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "tasks" }, summary = "Return a list of all tasks accessible to the request (determined by session or access token)")
    @ApiResponse(responseCode = "200", description = "List of tasks accessible to the current request")
    public List<Task> getTasks() {
        return DataManager.getInstance()
                .getRestApiJobManager()
                .getTasks()
                .stream()
                .filter(job -> this.isAuthorized(job.getType(), job.getSessionId(), request))
                .collect(Collectors.toList());
    }

    public boolean isAuthorized(ITaskType type, Optional<String> jobSessionId, HttpServletRequest request) {
        if (type == null) {
            return false;
        }
        Task.Accessibility access = Task.getAccessibility(type);
        switch (access) {
            case PUBLIC:
                return true;
            case TOKEN:
                return AuthorizationFilter.isAuthorized(request);
            case ADMIN:
                return AdminLoggedInFilter.isAdminLoggedIn(request);
            case SESSION:
                String sessionId = request.getSession().getId();
                if (sessionId != null) {
                    return jobSessionId.map(id -> id.equals(sessionId)).orElse(false);
                }
                return false;
            default:
                return false;
        }
    }
}
