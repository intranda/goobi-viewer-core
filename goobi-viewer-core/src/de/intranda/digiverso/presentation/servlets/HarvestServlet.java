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
package de.intranda.digiverso.presentation.servlets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.faces.validators.PIValidator;
import de.intranda.digiverso.presentation.model.download.DownloadJob;
import de.intranda.digiverso.presentation.model.download.DownloadJob.JobStatus;
import de.intranda.digiverso.presentation.model.misc.Harvestable;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;

/**
 * Servlet for harvesting crowdsourcing data and overview pages.
 */
public class HarvestServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = -3607375020549274741L;

    private static final Logger logger = LoggerFactory.getLogger(HarvestServlet.class);

    // private HttpClient httpClient;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public HarvestServlet() {
        super();
        // httpClient = new HttpClient();
        // httpClient.setHttpConnectionManager(new MultiThreadedHttpConnectionManager());
        // httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = null;
        String identifier = null;
        String status = null;
        String message = null;
        Date fromDate = null;
        Date toDate = null;
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
                            break;
                        case "status":
                            status = values[0];
                            break;
                        case "message":
                            message = values[0];
                            break;
                        case "from":
                            DateTime fromDateTime = DateTools.parseDateTimeFromString(values[0], true);
                            if (fromDateTime == null) {
                                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal 'from' attribute value: " + values[0]);
                                return;
                            }
                            fromDate = fromDateTime.toDate();
                            break;
                        case "until":
                            DateTime toDateTime = DateTools.parseDateTimeFromString(values[0], true);
                            if (toDateTime == null) {
                                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal 'until' attribute value: " + values[0]);
                                return;
                            }
                            toDate = toDateTime.toDate();
                            break;
                        case "first":
                            try {
                                first = Integer.parseInt(values[0]);
                            } catch (NumberFormatException e) {
                                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal 'first' attribute value: " + values[0]);
                                return;
                            }
                            break;
                        case "pageSize":
                            try {
                                pageSize = Integer.parseInt(values[0]);
                            } catch (NumberFormatException e) {
                                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal 'pageSize' attribute value: " + values[0]);
                                return;
                            }
                            break;
                    }
                }
            }
        }
        if (action == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: action");
            return;
        }
        if (!action.startsWith("getlist")) {
            if (identifier == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: identifier");
                return;
            } else if (!PIValidator.validatePi(identifier)) {
                logger.warn("Identifier is invalid: {}", identifier);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "identifier value invalid");
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
                        long count = DataManager.getInstance().getDao().getOverviewPageCount(fromDate, toDate);
                        List<OverviewPage> overviewPages = DataManager.getInstance().getDao().getOverviewPages(first, pageSize, fromDate, toDate);
                        JSONArray jsonArray = convertToJSON(count, overviewPages);
                        response.setContentType("application/json");
                        response.getWriter().write(jsonArray.toString());
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                    return;
                case "snoop_overviewpage":
                    // Checks whether there are overview page updates with in the given time frame (returns 200 or 404, respectively)
                    try {
                        // ?action=snoop_overviewpage&identifier=PPN62692460X&from=2015-06-26&until=2016-01-01
                        if (!DataManager.getInstance().getDao().isOverviewPageHasUpdates(identifier, fromDate, toDate)) {
                            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        }
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
                    }
                    return;
                case "get_overviewpage": {
                    Path tempFolder = Paths.get(DataManager.getInstance().getConfiguration().getTempFolder());
                    if (!FileTools.checkPathExistance(tempFolder, true)) {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Temp folder could not be created: " + DataManager.getInstance().getConfiguration().getTempFolder());
                        return;
                    }
                    Path tempFile = null;
                    try {
                        // ?action=get_overviewpage&identifier=PPN62692460X&from=2015-06-26&until=2016-01-01
                        OverviewPage overviewPage = DataManager.getInstance().getDao().getOverviewPageForRecord(identifier, fromDate, toDate);
                        if (overviewPage == null) {
                            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Overview page not found");
                            return;
                        }
                        String now = DateTools.formatterFilename.print(System.currentTimeMillis());
                        String fileNamePrefix = String.valueOf(Thread.currentThread().getId()) + "_"; // Thread ID as prefix so that it doesn't collide with other users' calls
                        String fileName = identifier + "_overviewpage_" + (fromDate != null ? fromDate.getTime() : "-") + "-"
                                + (toDate != null ? toDate.getTime() : "-") + ".xml";
                        tempFile = FileTools
                                .getFileFromString(overviewPage.getExportFormat(),
                                        DataManager.getInstance().getConfiguration().getTempFolder() + fileNamePrefix + fileName, "UTF-8", false)
                                .toPath();
                        if (Files.isRegularFile(tempFile)) {
                            response.setContentType("application/xml");
                            response.setHeader("Content-Disposition",
                                    new StringBuilder("attachment;filename=").append(now + "_" + fileName).toString());
                            response.setHeader("Content-Length", String.valueOf(Files.size(tempFile)));
                            response.flushBuffer();
                            OutputStream os = response.getOutputStream();
                            try (FileInputStream fis = new FileInputStream(tempFile.toFile())) {
                                byte[] buffer = new byte[1024];
                                int bytesRead = 0;
                                while ((bytesRead = fis.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                }
                            }
                            // os.flush();
                        }
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
                    } finally {
                        if (tempFile != null && Files.isRegularFile(tempFile)) {
                            Files.delete(tempFile);
                        }
                    }
                }
                    return;
                case "dl_update":
                    // http://localhost:8080/viewer/harvest?&action=dl_update&identifier=7062b2225caf97a5e80f91f647f66b95&status=READY
                    if (StringUtils.isEmpty(identifier)) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "identifier required");
                        return;
                    }
                    if (StringUtils.isEmpty(status)) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "status required");
                        return;
                    }
                    try {
                        // Find job in the DB
                        DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByIdentifier(identifier);
                        if (job == null) {
                            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job not found");
                            return;
                        }
                        JobStatus oldStatus = job.getStatus();
                        JobStatus djStatus = JobStatus.getByName(status);
                        if (djStatus == null) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown status: " + status);
                            return;
                        }
                        logger.trace("Update for " + job.getType() + " job " + job.getIdentifier() + ": Status changed from " + oldStatus + " to "
                                + djStatus);
                        //only do something if job status has actually changed
                        if (!djStatus.equals(oldStatus)) {
                            // Update and save job
                            synchronized (job) {
                                try {
                                    job.setStatus(djStatus);
                                    if (StringUtils.isNotBlank(message)) {
                                        job.setMessage(message);
                                    }
                                    job.setLastRequested(new Date());
                                    if (JobStatus.ERROR.equals(djStatus) || JobStatus.READY.equals(djStatus)) {
                                        // Send out the word
                                        job.notifyObservers(djStatus, message);
                                        job.resetObservers();
                                    }
                                } catch (MessagingException e) {
                                    logger.error(e.getMessage(), e);
                                    // response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error contacting observers");
                                } finally {
                                    if (!DataManager.getInstance().getDao().updateDownloadJob(job)) {
                                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                        return;
                                    }
                                    logger.trace("Downloadjob {} updated in database with status {}", job, job.getStatus());
                                }
                            }
                        }
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
                        return;
                    }
                    return;
                // Redirect crowdsourcing requests to 
                case "getlist_crowdsourcing":
                case "snoop_cs":
                case "get_cs": {
                    String forward = "/csharvest?" + request.getQueryString();
                    logger.trace("Redirecting to {}", forward);
                    response.sendRedirect(forward);
                }
                    return;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                    return;
            }
        }
        logger.debug("Access condition for download not met for '{}'.", identifier);
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     *
     * @param totalCount
     * @param identifiers
     * @return
     */
    @SuppressWarnings("unchecked")
    protected static JSONArray convertToJSON(long totalCount, List<? extends Harvestable> objects) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(totalCount);
        for (Harvestable o : objects) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", o.getPi());
            if (o.getDateUpdated() != null) {
                long timestamp = o.getDateUpdated().getTime();
                if (timestamp < 0) {
                    timestamp = 0;
                }
                jsonObj.put("du", timestamp);
            } else {
                jsonObj.put("du", 0);
                logger.debug("{} has no dateUpdated", o.getPi());
            }
            jsonArray.add(jsonObj);
        }

        return jsonArray;
    }
}
