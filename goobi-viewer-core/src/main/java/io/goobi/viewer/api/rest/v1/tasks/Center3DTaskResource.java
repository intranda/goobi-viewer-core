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

import static io.goobi.viewer.api.rest.v1.ApiUrls.TASKS_CENTER_3D;
import static io.goobi.viewer.api.rest.v1.ApiUrls.TASKS_CENTER_3D_RECORD;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.mq.Center3DObjectHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for triggering automatic centering of GLTF/GLB 3D objects in the Voyager viewer. Centering generates a {@code *.svx.json} scene file
 * that positions the rotation pivot at the object's bounding-box centre.
 *
 * <p>
 * Both endpoints are admin-only and respond immediately with the number of queued tasks – the actual centering work happens asynchronously in the
 * message queue.
 */
@jakarta.ws.rs.Path(TASKS_CENTER_3D)
@ViewerRestServiceBinding
@AuthorizationBinding
public class Center3DTaskResource {

    private static final Logger logger = LogManager.getLogger(Center3DTaskResource.class);

    @Inject
    private MessageQueueManager messageBroker;

    /**
     * Queues centering tasks for all GLTF/GLB files belonging to the given record.
     *
     * @param pi the persistent identifier of the record
     * @param force if {@code true}, existing SVX scene files are overwritten; if {@code false} (default), records that already have a scene file are
     *            skipped
     * @return JSON with the number of queued tasks
     */
    @POST
    @jakarta.ws.rs.Path(TASKS_CENTER_3D_RECORD)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = { "tasks" },
            summary = "Queue 3D centering for all GLTF/GLB files of a single record",
            description = "Finds every .gltf/.glb file in the record's media folder and queues a centering task for each. "
                    + "Responds immediately; the work runs asynchronously.")
    @ApiResponse(responseCode = "200", description = "Tasks queued",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(example = "{\"pi\":\"PPN123\",\"queued\":2,\"force\":false}")))
    @ApiResponse(responseCode = "401", description = "Admin login required")
    @ApiResponse(responseCode = "404", description = "Record not found or has no media folder")
    @ApiResponse(responseCode = "500", description = "Internal error")
    public Response centerRecord(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Overwrite existing SVX scene files") @QueryParam("force") @DefaultValue("false") boolean force) {

        try {
            java.nio.file.Path mediaDir = DataFileTools.getMediaFolder(pi);
            List<String> gltfFiles = findGltfFiles(mediaDir);
            int queued = queueMessages(gltfFiles, pi, force);
            logger.info("Queued {} centering task(s) for record {} (force={})", queued, pi, force);
            return Response.ok(buildResult(pi, queued, force)).build();

        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Could not resolve media folder for {}: {}", pi, e.toString());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Record not found or has no media folder: " + pi + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Queues centering tasks for every GLTF/GLB file found across all records in all configured data repositories.
     *
     * @param force if {@code true}, existing SVX scene files are overwritten; if {@code false} (default), records that already have a scene file are
     *            skipped
     * @return JSON with the total number of queued tasks
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = { "tasks" },
            summary = "Queue 3D centering for all GLTF/GLB files across all records",
            description = "Walks all configured data repositories, finds every .gltf/.glb file, and queues a centering task for each. "
                    + "Responds immediately; the work runs asynchronously. This operation may queue many tasks.")
    @ApiResponse(responseCode = "200", description = "Tasks queued",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(example = "{\"queued\":42,\"force\":false}")))
    @ApiResponse(responseCode = "401", description = "Admin login required")
    @ApiResponse(responseCode = "500", description = "Internal error")
    public Response centerAll(
            @Parameter(description = "Overwrite existing SVX scene files") @QueryParam("force") @DefaultValue("false") boolean force) {

        List<GltfRecord> records;
        try {
            records = findAllGltfRecords();
        } catch (IOException e) {
            logger.error("Error scanning media repositories: {}", e.toString());
            throw new WebApplicationException(e);
        }

        int queued = 0;
        for (GltfRecord record : records) {
            queued += queueMessages(List.of(record.filename()), record.pi(), force);
        }

        logger.info("Queued {} centering task(s) across all records (force={})", queued, force);
        return Response.ok(buildResult(null, queued, force)).build();
    }

    /**
     * Finds all GLTF/GLB filenames directly inside {@code mediaDir}.
     */
    static List<String> findGltfFiles(java.nio.file.Path mediaDir) {
        if (!Files.isDirectory(mediaDir)) {
            return List.of();
        }
        try (Stream<java.nio.file.Path> stream = Files.list(mediaDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(Center3DTaskResource::isGltfFile)
                    .toList();
        } catch (IOException e) {
            logger.warn("Error listing files in {}: {}", mediaDir, e.toString());
            return List.of();
        }
    }

    /**
     * Walks all configured data repositories and the viewer home directory to collect every (PI, filename) pair where the filename is a GLTF or GLB
     * file.
     */
    List<GltfRecord> findAllGltfRecords() throws IOException {
        List<GltfRecord> result = new ArrayList<>();
        for (java.nio.file.Path mediaRoot : DataFileTools.getAllMediaRoots()) {
            collectFromMediaRoot(mediaRoot, result);
        }
        return result;
    }

    /**
     * Scans {@code mediaRoot} for PI subdirectories and collects GLTF/GLB files within them.
     *
     * @param mediaRoot path of the form {@code …/media/}; each direct child directory is a PI
     * @param result accumulator for found records
     */
    private static void collectFromMediaRoot(java.nio.file.Path mediaRoot, List<GltfRecord> result) {
        if (!Files.isDirectory(mediaRoot)) {
            return;
        }
        try (Stream<java.nio.file.Path> piDirs = Files.list(mediaRoot)) {
            piDirs.filter(Files::isDirectory).forEach(piDir -> {
                String pi = piDir.getFileName().toString();
                try (Stream<java.nio.file.Path> files = Files.list(piDir)) {
                    files.filter(Files::isRegularFile)
                            .map(p -> p.getFileName().toString())
                            .filter(Center3DTaskResource::isGltfFile)
                            .map(filename -> new GltfRecord(pi, filename))
                            .forEach(result::add);
                } catch (IOException e) {
                    logger.warn("Error listing files for PI {}: {}", pi, e.toString());
                }
            });
        } catch (IOException e) {
            logger.warn("Error scanning media root {}: {}", mediaRoot, e.toString());
        }
    }

    private int queueMessages(List<String> filenames, String pi, boolean force) {
        int count = 0;
        for (String filename : filenames) {
            try {
                messageBroker.addToQueue(Center3DObjectHandler.createMessage(pi, filename, force));
                count++;
            } catch (MessageQueueException e) {
                logger.error("Failed to queue centering task for {}/{}: {}", pi, filename, e.toString());
            }
        }
        return count;
    }

    private static boolean isGltfFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".gltf") || lower.endsWith(".glb");
    }

    private static String buildResult(String pi, int queued, boolean force) {
        StringBuilder sb = new StringBuilder("{");
        if (pi != null) {
            sb.append("\"pi\":\"").append(pi).append("\",");
        }
        sb.append("\"queued\":").append(queued).append(",");
        sb.append("\"force\":").append(force).append("}");
        return sb.toString();
    }

    /** Holds a (PI, filename) pair found during media directory scanning. */
    record GltfRecord(String pi, String filename) {
    }
}
