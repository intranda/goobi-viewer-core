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
package io.goobi.viewer.api.rest.v1;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.bindings.AdminLoggedInBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.log.LogFile;
import io.goobi.viewer.model.log.LogLine;
import io.goobi.viewer.model.log.LogLineParser;
import io.goobi.viewer.api.rest.filters.UserLoggedInFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * REST endpoint for log file polling (WebSocket fallback).
 * Access: superusers only.
 *
 * GET /api/v1/logs/{logfile}?sinceOffset={byteOffset}
 */
@jakarta.ws.rs.Path("/logs")
@AdminLoggedInBinding
@SecurityRequirement(name = UserLoggedInFilter.SECURITY_SCHEME_BEARER)
public class LogViewerResource {

    private static final Logger logger = LogManager.getLogger(LogViewerResource.class);
    private static final int MAX_READ_BYTES = 1_048_576;

    @GET
    @jakarta.ws.rs.Path("/{logfile}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = { "monitoring" }, summary = "Requires an admin identity. Read new lines from one of the viewer log files",
            description = "Polling fallback for the log viewer's WebSocket connection. Without 'sinceOffset' the tail of the file is"
                    + " returned, its length configured by logViewer/initialLines; with an offset only the bytes appended since then."
                    + " The response always reports the file's current size as the offset for the next poll. At most 1 MiB is read per"
                    + " request. A configured log file that does not exist yet yields an empty line list rather than an error.")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = "object",
                            description = "Object holding 'lines', an array of parsed log lines with timestamp, level, thread,"
                                    + " location and message, and 'nextOffset', the byte offset to pass to the next request")),
            description = "Parsed log lines and the offset for the next request")
    @ApiResponse(responseCode = "400", description = "Unknown log file name; valid names are viewer, oai, ics and indexer")
    @ApiResponse(responseCode = "401", description = "No admin identity: neither a bearer token of an admin user nor an admin session")
    @ApiResponse(responseCode = "500", description = "Log file could not be read")
    public Response getLogLines(
            @Parameter(description = "Name of the log file to read: viewer, oai, ics or indexer") @PathParam("logfile") String logfileName,
            @Parameter(description = "Byte offset returned by a previous request; omit or pass 0 to receive the tail of the file")
            @QueryParam("sinceOffset") Long sinceOffset) {

        var optLogFile = LogFile.fromName(logfileName);
        if (optLogFile.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\":\"Unknown log file\"}")
                .build();
        }

        var optPath = optLogFile.get().getPath();
        if (optPath.isEmpty() || !Files.exists(optPath.get())) {
            return Response.ok("{\"lines\":[],\"nextOffset\":0}").build();
        }

        Path logPath = optPath.get();
        try {
            long currentSize = Files.size(logPath);
            List<LogLine> lines;

            if (sinceOffset == null || sinceOffset <= 0) {
                int initialLines = DataManager.getInstance().getConfiguration().getLogViewerInitialLines();
                lines = readLastNLines(logPath, initialLines);
            } else {
                lines = readFromOffset(logPath, sinceOffset);
            }

            StringBuilder json = new StringBuilder("{\"lines\":[");
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                json.append(lines.get(i).toJson());
            }
            json.append("],\"nextOffset\":").append(currentSize).append("}");
            return Response.ok(json.toString()).build();

        } catch (IOException e) {
            logger.error("Error reading log file {}", logPath, e);
            return Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\":\"Could not read log file\"}")
                .build();
        }
    }

    /**
     * Reads the last n parsed log entries using ReversedLinesFileReader.
     * Reads up to n*4 raw lines backward to account for multi-line entries (stacktraces).
     */
    static List<LogLine> readLastNLines(Path logPath, int n) throws IOException {
        try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                .setPath(logPath)
                .setCharset(StandardCharsets.UTF_8)
                .get()) {
            List<String> rawLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null && rawLines.size() < n * 4) {
                rawLines.add(line);
            }
            Collections.reverse(rawLines);
            List<LogLine> parsed = LogLineParser.parse(String.join("\n", rawLines));
            if (parsed.size() > n) {
                parsed = parsed.subList(parsed.size() - n, parsed.size());
            }
            return parsed;
        }
    }

    /**
     * Reads all log content after the given byte offset.
     */
    static List<LogLine> readFromOffset(Path logPath, long offset) throws IOException {
        if (!Files.exists(logPath)) {
            return Collections.emptyList();
        }
        long fileSize = Files.size(logPath);
        if (offset >= fileSize) {
            return Collections.emptyList();
        }
        try (FileInputStream fis = new FileInputStream(logPath.toFile())) {
            long skipped = fis.skip(offset);
            if (skipped < offset) {
                return Collections.emptyList();
            }
            byte[] bytes = fis.readNBytes(MAX_READ_BYTES);
            return LogLineParser.parse(new String(bytes, StandardCharsets.UTF_8));
        }
    }
}
