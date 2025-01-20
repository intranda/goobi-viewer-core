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
package io.goobi.viewer.model.sitemap;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.model.SitemapRequestParameters;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * Resource for sitemap generation.
 */
public class SitemapBuilder {

    private static final Logger logger = LogManager.getLogger(SitemapBuilder.class);

    private static Thread workerThread = null;

    private final HttpServletRequest servletRequest;

    public SitemapBuilder(HttpServletRequest request) {
        this.servletRequest = request;
    }

    public void updateSitemap(SitemapRequestParameters params)
            throws AccessDeniedException, IllegalRequestException, JSONException, PresentationException {

        if (params == null) {
            throw new IllegalRequestException("Invalid JSON request object");
        }

        String outputPath = params.getOutputPath();
        if (outputPath == null) {
            outputPath = servletRequest.getServletContext().getRealPath("/");
        }
        String rootUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(this.servletRequest);
        updateSitemap(outputPath, rootUrl);
    }

    public void updateSitemap(String outputPath, String viewerRootUrl) throws AccessDeniedException, JSONException, PresentationException {

        JSONObject ret = new JSONObject();

        Sitemap sitemap = new Sitemap();

        if (workerThread == null || !workerThread.isAlive()) {
            workerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        List<File> sitemapFiles = sitemap.generate(viewerRootUrl, outputPath);
                        if (sitemapFiles != null) {
                            ret.put("status", HttpServletResponse.SC_OK);
                            ret.put("message", sitemapFiles.size() + " sitemap files created");
                            JSONArray fileArray = new JSONArray();
                            for (File file : sitemapFiles) {
                                JSONObject fileObj = new JSONObject();
                                fileObj.put("filename", file.getName());
                                fileArray.put(fileObj);
                            }
                            ret.put("files", fileArray);
                        } else {
                            ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            ret.put("message", "Could not generate sitemap, please check logs");
                        }
                    } catch (PresentationException e) {
                        logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
                        ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        ret.put("message", e.getMessage());
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        ret.put("message", e.getMessage());
                    } catch (DAOException e) {
                        logger.debug("DAOException thrown here: {}", e.getMessage());
                        ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        ret.put("message", e.getMessage());
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                        ret.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        ret.put("message", e.getMessage());
                    }
                }
            });

            workerThread.start();
            try {
                workerThread.join();
            } catch (InterruptedException e) {
                workerThread.interrupt();
                throw new PresentationException("Processing interrupted");
            }
            if (!Integer.valueOf(HttpServletResponse.SC_OK).equals(ret.getInt("status"))) {
                throw new PresentationException(ret.getString("message"));
            }
        } else {
            throw new AccessDeniedException("Sitemap generation currently in progress");
        }

    }
}
