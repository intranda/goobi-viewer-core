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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.Sitemap;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;
import de.intranda.viewer.cache.JobManager;
import de.intranda.viewer.cache.JobManager.Status;
import de.unigoettingen.sub.commons.util.CacheUtils;

/**
 * Servlet for deleting cache elements. Should not be accessible to unauthorized persons. This is a temporary solutions which will probably be
 * replaced with some kind of GUI later.
 */
public class ToolServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = -2888790425901398519L;

    private static JobManager cacheFiller = new JobManager();

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(ToolServlet.class);

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = null;
        String identifier = null;
        boolean fromContentCache = false;
        boolean fromThumbnailCache = false;
        boolean fromPdfCache = false;
        boolean firstPageOnly = false;
        String outputPath = null;

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
                        case "fromContent":
                            fromContentCache = Boolean.valueOf(values[0]);
                            break;
                        case "fromThumbs":
                            fromThumbnailCache = Boolean.valueOf(values[0]);
                            break;
                        case "fromPdfs":
                            fromPdfCache = Boolean.valueOf(values[0]);
                            break;
                        case "firstPageOnly":
                            firstPageOnly = Boolean.valueOf(values[0]);
                            break;
                        case "outputPath":
                            outputPath = values[0];
                            break;
                        default: // nothing
                    }
                }
            }
        }

        // Check access conditions, if an actual document with a PI is involved
        if (action != null) {
            switch (action) {
                case "emptyCache":
                    int deleted = CacheUtils.deleteFromCache(identifier, fromContentCache, fromThumbnailCache, fromPdfCache);
                    response.getWriter().write(deleted + " cache elements belonging to '" + identifier + "' deleted.");
                    break;
                case "updateSitemap":
                    Sitemap sitemap = new Sitemap();
                    if (outputPath == null) {
                        outputPath = getServletContext().getRealPath("/");
                    }
                    List<File> sitemapFiles = null;
                    try {
                        sitemapFiles = sitemap.generate(ServletUtils.getServletPathWithHostAsUrlFromRequest(request), outputPath, firstPageOnly);
                    } catch (PresentationException e) {
                        logger.debug("PresentationException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                    if (sitemapFiles != null) {
                        response.getWriter().write("Sitemap files created:\n");
                        for (File file : sitemapFiles) {
                            response.getWriter().write("- " + file.getName() + "\n");
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                    break;
                case "fillCache":
                    String answer = performCacheFillerAction(request.getParameterMap());
                    String returnString = answer.trim().replaceAll("\\n", "<br>").replaceAll("\\t", "&#160;&#160;&#160;&#160;");

                    response.setContentType("text/html"); {
                    ServletOutputStream output = response.getOutputStream();
                    output.write(returnString.getBytes(Charset.forName("utf-8")));
                }
                    break;
                case "checkSolrSchemaName":
                    String[] result = SolrSearchIndex.checkSolrSchemaName();
                    int status = Integer.valueOf(result[0]);
                    if (status == 200) {
                        response.setStatus(200);
                        response.getOutputStream().write("OK".getBytes(Charset.forName("utf-8")));
                    } else {
                        response.sendError(status, result[1]);
                    }
                    break;
                case "getVersion":
                    response.setContentType("text/html"); {
                    ServletOutputStream output = response.getOutputStream();
                    output.write(Helper.getVersion().getBytes(Charset.forName("utf-8")));
                }
                    break;
                case "migrateOverviewPages": {
                    int migratedToDB = 0;
                    int deletedXmlFiles = 0;
                    response.setContentType("text/html");
                    ServletOutputStream output = response.getOutputStream();
                    try {
                        String[] fields = { SolrConstants.OVERVIEWPAGE, SolrConstants.PI, SolrConstants.SOURCEDOCFORMAT };
                        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(SolrConstants.ISWORK + ":true", Arrays.asList(
                                fields));
                        for (SolrDocument doc : docs) {
                            String overviewPageField = (String) doc.getFieldValue(SolrConstants.OVERVIEWPAGE);
                            if (overviewPageField == null) {
                                continue;
                            }
                            String pi = (String) doc.getFieldValue(SolrConstants.PI);
                            String msg = "Found record '" + pi + "' with an overview page in Solr... ";
                            output.write(msg.getBytes(Charset.forName("utf-8")));
                            OverviewPage overviewPage = DataManager.getInstance().getDao().getOverviewPageForRecord(pi, null, null);
                            if (overviewPage == null) {
                                overviewPage = new OverviewPage();
                                if (overviewPage.migrateToDB(overviewPageField, pi)) {
                                    migratedToDB++;
                                    msg = "migrated to the database<br />";
                                    output.write(msg.getBytes(Charset.forName("utf-8")));
                                    //                                    msg = deleteOverviewPageFile(pi);
                                    //                                    if (msg != null) {
                                    //                                        output.write(msg.getBytes(Charset.forName("utf-8")));
                                    //                                    }
                                    Helper.reIndexRecord(pi, (String) doc.getFieldValue(SolrConstants.SOURCEDOCFORMAT), overviewPage);
                                }
                            } else {
                                // Record already in DB but also in Solr
                                msg = "already in the database<br />";
                                output.write((msg).getBytes(Charset.forName("utf-8")));
                                //                                msg = deleteOverviewPageFile(pi);
                                //                                if (msg != null) {
                                //                                    output.write(msg.getBytes(Charset.forName("utf-8")));
                                //                                }
                                Helper.reIndexRecord(pi, (String) doc.getFieldValue(SolrConstants.SOURCEDOCFORMAT), overviewPage);
                            }
                        }
                        String msg = "Migrated " + migratedToDB + " overview pages to the database. " + deletedXmlFiles
                                + " overview page configurations were removed from the file system and the records are currently being re-indexed.";
                        output.write((msg + "<br />").getBytes(Charset.forName("utf-8")));
                    } catch (PresentationException e) {
                        logger.trace("PresentationException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (DAOException e) {
                        logger.debug("DAOException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                }
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                    break;
            }
        }
    }

    /**
     * @param parameterMap
     */
    private static String performCacheFillerAction(Map<String, String[]> parameterMap) {
        if (parameterMap.containsKey("stop")) {
            if (cacheFiller.getCacheFillerStatus() == Status.DORMANT) {
                return "Cachefiller is not running.";
            } else if (stopCacheFiller()) {
                return "Cachefiller stopped successfully";
            } else {
                return "Failed to stop Cachefiller in time. Last status: " + cacheFiller.getCacheFillerStatus();
            }
        } else if (parameterMap.containsKey("status")) {
            StringBuilder sbAnswer = new StringBuilder("Current Cachefiller status is ").append(cacheFiller.getCacheFillerStatus()).append("\n\n")
                    .append(cacheFiller.getDetailedGeneratorStatus());
            return sbAnswer.toString();
        } else if (parameterMap.containsKey("resetTrace")) {
            File traceFile = cacheFiller.getConfiguredTraceFile();
            if (traceFile != null && traceFile.isFile()) {
                if (traceFile.delete()) {
                    return "Tracefile " + traceFile.getAbsolutePath() + " deleted successfully";
                }
                return "Failed to delete tracefile " + traceFile.getAbsolutePath();
            }
            return "No tracefile found to delete";
        } else if (parameterMap.containsKey("start")) {
            if (cacheFiller.getCacheFillerStatus() != JobManager.Status.DORMANT) {
                String error = "CacheFiller is already running. To start another run, first end current run by using parameter 'stop'";
                logger.error(error);
                return error;
            }
            return startCacheFiller(parameterMap);
        } else {
            StringBuilder info = new StringBuilder(600);
            info.append("intrandaCacheFiller: use the following parameters to control the cacheFiller:\n");
            info.append(
                    "\nstart\t\t\t\tStarts the cacheFiller. Accepts additional parameters, if deviation from default parameters specified in \"config_cacheFiller\" is desired. Sets status to INITIALIZING and then to RUNNING after initialization");
            info.append(
                    "\nstop\t\t\t\tStops all cacheFiller threads. Sets status to TERMINATING and then to DORMANT once all threads have concluded");
            info.append("\nstatus\t\t\t\tRequests the current cacheFiller status and status of all running threads. Initial status is DORMANT");
            info.append("\nresetTrace\t\tDeletes any saved progress");
            return info.toString();
        }

    }

    /**
     * @param parameterMap
     */
    private static String startCacheFiller(Map<String, String[]> parameterMap) {
        parseParameters(parameterMap);
        cacheFiller.fillCache();
        String answer = "Cachefiller started with generators:\n\n";
        answer += cacheFiller.getDetailedGeneratorStatus();
        return answer;

    }

    private static void parseParameters(Map<String, String[]> parameterMap) {
        for (String parameter : parameterMap.keySet()) {

            String[] values = parameterMap.get(parameter);

            switch (parameter) {
                case "csec":
                    cacheFiller.setCsec(getBooleanValue(values));
                    break;
                case "reverse":
                    cacheFiller.setReverse(getBooleanValue(values));
                    break;
                case "overwrite":
                    cacheFiller.setOverwriteCache(getBooleanValue(values));
                    break;
                case "thumbnails":
                    cacheFiller.setGenerateThumbnails(getBooleanValue(values));
                    break;
                case "largeImages":
                    cacheFiller.setGenerateLargeImages(getBooleanValue(values));
                    break;
                case "fullscreen":
                    cacheFiller.setGenerateFullscreenImages(getBooleanValue(values));
                    break;
                case "previews":
                    cacheFiller.setGeneratePreviews(getBooleanValue(values));
                    break;
                case "pdfs":
                    cacheFiller.setGeneratePdfs(getBooleanValue(values));
                    break;
                case "startDate":
                    try {
                        cacheFiller.setStartDate(parseDate(values[0]));
                    } catch (ParseException e) {
                        logger.error("Unable to parse date " + values[0]);
                        cacheFiller.setStartDate(null);
                    }
                    break;
                case "traceProgress":
                    cacheFiller.setTrace(getBooleanValue(values));
                    break;
            }
        }
    }

    private static Date parseDate(String dateString) throws ParseException {
        if (dateString == null) {
            throw new IllegalArgumentException("dateString may not be null");
        }
        dateString = dateString.replaceAll("\\D", "");
        Date date = null;
        switch (dateString.length()) {
            case 8:
                date = DateTools.formatterISO8601BasicDate.parseDateTime(dateString).toDate();
                break;
            case 4:
                date = DateTools.formatterISO8601BasicDateNoYear.parseDateTime(dateString).toDate();
                Calendar cal = new GregorianCalendar();
                int year = cal.get(Calendar.YEAR);
                cal.setTime(date);
                cal.set(Calendar.YEAR, year);
                date = cal.getTime();
                break;
            default:
                throw new ParseException("Wrong date format", 0);
        }

        return date;
    }

    /**
     * @param values
     * @return
     */
    private static boolean getBooleanValue(String[] values) {
        if (values == null || values.length == 0 || values[0].trim().isEmpty()) {
            return true;
        }

        return Boolean.valueOf(values[0]);
    }

    /**
     * @return
     */
    private static boolean stopCacheFiller() {
        long maxWaitInSeconds = 10;
        cacheFiller.interrupt();
        long start = System.currentTimeMillis();
        boolean stopped = false;
        while (System.currentTimeMillis() - start < maxWaitInSeconds * 1000) {
            if (cacheFiller.getCacheFillerStatus() == Status.DORMANT) {
                stopped = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
        return stopped;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
