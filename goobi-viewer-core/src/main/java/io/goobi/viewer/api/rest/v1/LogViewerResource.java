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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint for log file polling (WebSocket fallback).
 * Access: superusers only.
 *
 * GET /api/v1/logs/{logfile}?sinceOffset={byteOffset}
 */
@jakarta.ws.rs.Path("/logs")
@AdminLoggedInBinding
public class LogViewerResource {

    private static final Logger logger = LogManager.getLogger(LogViewerResource.class);

    @GET
    @jakarta.ws.rs.Path("/{logfile}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogLines(
            @PathParam("logfile") String logfileName,
            @QueryParam("sinceOffset") Long sinceOffset) {

        var optLogFile = LogFile.fromName(logfileName);
        if (optLogFile.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Unknown log file: " + logfileName.replace("\"", "\\\"") + "\"}")
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
                if (i > 0) json.append(',');
                json.append(lines.get(i).toJson());
            }
            json.append("],\"nextOffset\":").append(currentSize).append("}");
            return Response.ok(json.toString()).build();

        } catch (IOException e) {
            logger.error("Error reading log file {}: {}", logPath, e.getMessage(), e);
            return Response.serverError().entity("{\"error\":\"Could not read log file\"}").build();
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
            StringBuilder raw = new StringBuilder();
            String line;
            int rawCount = 0;
            while ((line = reader.readLine()) != null && rawCount < n * 4) {
                raw.insert(0, line + "\n");
                rawCount++;
            }
            List<LogLine> parsed = LogLineParser.parse(raw.toString());
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
        if (!Files.exists(logPath)) return Collections.emptyList();
        long fileSize = Files.size(logPath);
        if (offset >= fileSize) return Collections.emptyList();
        try (FileInputStream fis = new FileInputStream(logPath.toFile())) {
            long skipped = fis.skip(offset);
            if (skipped < offset) return Collections.emptyList();
            byte[] bytes = fis.readAllBytes();
            return LogLineParser.parse(new String(bytes, StandardCharsets.UTF_8));
        }
    }
}
