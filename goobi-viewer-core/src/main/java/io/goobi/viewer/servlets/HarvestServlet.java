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
package io.goobi.viewer.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.misc.Harvestable;
import jakarta.mail.MessagingException;

/**
 * Servlet for harvesting crowdsourcing data and overview pages.
 */
public class HarvestServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = -3607375020549274741L;

    private static final Logger logger = LogManager.getLogger(HarvestServlet.class);

    private static final String ERROR_DB = "Database error";

    /**
     * <p>
     * Constructor for HarvestServlet.
     * </p>
     *
     * @see HttpServlet#HttpServlet()
     */
    public HarvestServlet() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = null;
        String identifier = null;
        String status = null;
        String message = null;
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        int first = 0;
        int pageSize = 100000;

        if (request.getParameterMap().size() > 0) {
            // Regular URLs
            Set<String> keys = request.getParameterMap().keySet();
            for (String s : keys) {
                String[] values = request.getParameterMap().get(s);
                if (values[0] != null) {
                    switch (s) {
                        case "action":
                            action = values[0];
                            break;
                        case "identifier":
                            identifier = values[0];
                            identifier = Paths.get(identifier).getFileName().toString(); // Make sure filename doesn't inject a path traversal
                            break;
                        case "status":
                            status = values[0];
                            break;
                        case "message":
                            message = values[0];
                            break;
                        case "from":
                            fromDate = DateTools.parseDateTimeFromString(values[0], true);
                            if (fromDate == null) {
                                try {
                                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal 'from' attribute value: " + values[0]);
                                } catch (IOException e) {
                                    logger.error(e.getMessage());
                                }
                                return;
                            }
                            break;
                        case "until":
                            toDate = DateTools.parseDateTimeFromString(values[0], true);
                            if (toDate == null) {
                                try {
                                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal 'until' attribute value: " + values[0]);
                                } catch (IOException e) {
                                    logger.error(e.getMessage());
                                }
                                return;
                            }
                            break;
                        case "first":
                            try {
                                first = Integer.parseInt(values[0]);
                            } catch (NumberFormatException e) {
                                try {
                                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal 'first' attribute value: " + values[0]);
                                } catch (IOException e1) {
                                    logger.error(e1.getMessage());
                                }
                                return;
                            }
                            break;
                        case "pageSize":
                            try {
                                pageSize = Integer.parseInt(values[0]);
                            } catch (NumberFormatException e) {
                                try {
                                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal 'pageSize' attribute value: " + values[0]);
                                } catch (IOException e1) {
                                    logger.error(e1.getMessage());
                                }
                                return;
                            }
                            break;
                        default:
                            try {
                                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal actiion: " + s);
                            } catch (IOException e) {
                                logger.error(e.getMessage());
                            }
                            break;
                    }
                }
            }
        }
        if (action == null) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: action");
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }
        if (!action.startsWith("getlist")) {
            if (identifier == null) {
                try {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: identifier");
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
                return;
            } else if (!PIValidator.validatePi(identifier)) {
                logger.warn("Identifier is invalid: {}", identifier);
                try {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "identifier value invalid");
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
                return;
            }
        }

        // TODO Check access conditions, if an actual document with a PI is involved
        boolean access = true;

        if (access) {
            switch (action) {
                case "getlist_overviewpage":
                    // Get a JSON list of all identifiers  and timestamps of records that have an overview page update in the given time frame
                    try {
                        // EXAMPLE: ?action=getlist_overviewpage&from=2015-06-26&until=2016-01-01&first=0&pageSize=100
                        long count = DataManager.getInstance().getDao().getCMSPageWithRelatedPiCount(fromDate, toDate);
                        List<CMSPage> cmsPages = DataManager.getInstance()
                                .getDao()
                                .getCMSPagesWithRelatedPi(first, pageSize, fromDate, toDate);
                        JSONArray jsonArray = convertToJSON(count, cmsPages);
                        response.setContentType("application/json");
                        response.getWriter().write(jsonArray.toString());
                    } catch (IOException | DAOException e) {
                        logger.error(e.getMessage(), e);
                        try {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }
                    }
                    return;
                case "snoop_overviewpage":
                    // Checks whether there are cms page updates with in the given time frame (returns 200 or 404, respectively)
                    try {
                        // ?action=snoop_overviewpage&identifier=PPN62692460X&from=2015-06-26&until=2016-01-01
                        if (!DataManager.getInstance().getDao().isCMSPagesForRecordHaveUpdates(identifier, null, fromDate, toDate)) {
                            try {
                                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                            } catch (IOException e) {
                                logger.error(e.getMessage());
                            }
                        }
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        try {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_DB);
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }
                    }
                    return;
                case "get_overviewpage":
                    Path tempFolder = Paths.get(DataManager.getInstance().getConfiguration().getTempFolder());
                    try {
                        if (!FileTools.checkPathExistance(tempFolder, true)) {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Temp folder could not be created: " + DataManager.getInstance().getConfiguration().getTempFolder());
                            return;
                        }
                    } catch (IOException e) {
                        try {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Temp folder could not be checked: " + tempFolder.toAbsolutePath().toString());
                            return;
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }

                        // Thread ID as the temp folder path so that it doesn't collide with other users' calls
                        Path localTempFolder =
                                Paths.get(DataManager.getInstance().getConfiguration().getTempFolder(),
                                        String.valueOf(Thread.currentThread().getId()));
                        try {
                            // ?action=get_overviewpage&identifier=PPN62692460X&from=2015-06-26&until=2016-01-01
                            List<CMSPage> pages = DataManager.getInstance().getDao().getCMSPagesForRecord(identifier, null);
                            if (pages.isEmpty()) {
                                try {
                                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "CMS pages not found");
                                } catch (IOException e1) {
                                    logger.error(e1.getMessage());
                                }
                                return;
                            }
                            try {
                                Files.createDirectory(localTempFolder);
                            } catch (IOException e1) {
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                        "Temp folder could not be created: " + localTempFolder.toAbsolutePath().toString());
                                return;
                            }
                            String fileName =
                                    identifier + "_cmspage_" + (fromDate != null ? DateTools.getMillisFromLocalDateTime(fromDate, true) : "-") + "-"
                                            + (toDate != null ? DateTools.getMillisFromLocalDateTime(toDate, true) : "-") + ".zip";
                            fileName = FilenameUtils.getName(fileName); // Make sure identifier doesn't inject a path traversal
                            Path zipFile = Paths.get(localTempFolder.toAbsolutePath().toString(), fileName);
                            List<File> tempFiles = new ArrayList<>(pages.size() * 2);
                            try {
                                for (CMSPage page : pages) {
                                    tempFiles.addAll(page.exportTexts(localTempFolder.toAbsolutePath().toString(), fileName));
                                }
                            } catch (IOException e1) {
                                logger.error(e1.getMessage(), e1);
                                try {
                                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                                } catch (IOException e2) {
                                    logger.error(e2.getMessage());
                                }
                                return;
                            }

                            if (tempFiles.isEmpty()) {
                                try {
                                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "No content found");
                                } catch (IOException e1) {
                                    logger.error(e1.getMessage());
                                }
                                return;
                            }

                            // Compress to a ZIP
                            try {
                                FileTools.compressZipFile(tempFiles, zipFile.toFile(), 9);
                                if (Files.isRegularFile(zipFile)) {
                                    String now = LocalDateTime.now().format(DateTools.formatterISO8601BasicDateTime);
                                    response.setContentType("application/zip");
                                    response.setHeader("Content-Disposition",
                                            new StringBuilder("attachment;filename=").append(now + "_" + fileName).toString());

                                    response.setHeader("Content-Length", String.valueOf(Files.size(zipFile)));
                                    response.flushBuffer();
                                    OutputStream os = response.getOutputStream();
                                    try (FileInputStream fis = new FileInputStream(zipFile.toFile())) {
                                        byte[] buffer = new byte[1024];
                                        int bytesRead = 0;
                                        while ((bytesRead = fis.read(buffer)) != -1) {
                                            os.write(buffer, 0, bytesRead);
                                        }
                                    }
                                }
                            } catch (IOException e1) {
                                logger.error(e1.getMessage(), e1);
                                try {
                                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                                } catch (IOException e2) {
                                    logger.error(e2.getMessage());
                                }
                            }

                        } catch (DAOException e1) {
                            logger.error(e1.getMessage(), e1);
                            try {
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_DB);
                            } catch (IOException e2) {
                                logger.error(e2.getMessage());
                            }
                        } finally {
                            if (localTempFolder != null && Files.isDirectory(localTempFolder)) {
                                try {
                                    FileUtils.deleteDirectory(localTempFolder.toFile());
                                } catch (IOException e1) {
                                    logger.error(e1.getMessage());
                                }
                            }
                        }
                    }
                    return;
                case "dl_update":
                    // http://localhost:8080/viewer/harvest?&action=dl_update&identifier=7062b2225caf97a5e80f91f647f66b95&status=READY
                    if (StringUtils.isEmpty(identifier)) {
                        try {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "identifier required");
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }
                        return;
                    }
                    if (StringUtils.isEmpty(status)) {
                        try {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "status required");
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }
                        return;
                    }
                    try {
                        // Find job in the DB
                        DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByIdentifier(identifier);
                        if (job == null) {
                            try {
                                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job not found");
                            } catch (IOException e) {
                                logger.error(e.getMessage());
                            }
                            return;
                        }
                        JobStatus oldStatus = job.getStatus();
                        JobStatus djStatus = JobStatus.getByName(status);
                        if (djStatus == null) {
                            try {
                                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown status: " + status);
                            } catch (IOException e) {
                                logger.error(e.getMessage());
                            }
                            return;
                        }
                        logger.trace("Update for {} job {}: Changing status from {} to {}", job.getType(), job.getIdentifier(), oldStatus, djStatus);
                        //only do something if job status has actually changed
                        if (!djStatus.equals(oldStatus)) {
                            // Update and save job
                            synchronized (job) {
                                try {
                                    job.setStatus(djStatus);
                                    if (StringUtils.isNotBlank(message)) {
                                        job.setMessage(message);
                                    }
                                    job.setLastRequested(LocalDateTime.now());
                                    if (JobStatus.ERROR.equals(djStatus) || JobStatus.READY.equals(djStatus)) {
                                        // Send out the word
                                        try {
                                            job.notifyObservers(djStatus, message);
                                            job.resetObservers();
                                        } catch (UnsupportedEncodingException e) {
                                            logger.error(e.getMessage());
                                        }
                                    }
                                } catch (MessagingException e) {
                                    logger.error(e.getMessage(), e);
                                } finally {
                                    try {
                                        if (!DataManager.getInstance().getDao().updateDownloadJob(job)) {
                                            try {
                                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                            } catch (IOException e) {
                                                logger.error(e.getMessage());
                                            }
                                        } else {
                                            logger.trace("Downloadjob {} updated in database with status {}", job, job.getStatus());
                                        }
                                    } catch (DAOException e) {
                                        logger.error(e.getMessage(), e);
                                        try {
                                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_DB);
                                        } catch (IOException e1) {
                                            logger.error(e1.getMessage());
                                        }
                                    }
                                }
                            }
                        }
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        try {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_DB);
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }
                    }
                    return;
                // Redirect crowdsourcing requests to
                case "getlist_crowdsourcing":
                case "snoop_cs":
                case "get_cs":
                    String contextPath = request.getContextPath();
                    if (contextPath == null || contextPath.equals("/")) {
                        contextPath = "";
                    }
                    String forward = contextPath + "/csharvest?" + request.getQueryString();
                    logger.trace("Redirecting to {}", forward);
                    try {
                        response.sendRedirect(forward);
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                    return;
                default:
                    try {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                    } catch (IOException e1) {
                        logger.error(e1.getMessage());
                    }
                    return;
            }
        }
        logger.debug("Access condition for download not met for '{}'.", identifier);
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * <p>
     * convertToJSON.
     * </p>
     *
     * @param totalCount a long.
     * @param objects a {@link java.util.List} object.
     * @return a {@link org.json.JSONArray} object.
     */
    protected static JSONArray convertToJSON(long totalCount, List<? extends Harvestable> objects) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(totalCount);
        for (Harvestable o : objects) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", o.getPi());
            if (o.getDateUpdated() != null) {
                long timestamp = DateTools.getMillisFromLocalDateTime(o.getDateUpdated(), false);
                if (timestamp < 0) {
                    timestamp = 0;
                }
                jsonObj.put("du", timestamp);
            } else {
                jsonObj.put("du", 0L);
                logger.debug("{} has no dateUpdated", o.getPi());
            }
            jsonArray.put(jsonObj);
        }

        return jsonArray;
    }
}
