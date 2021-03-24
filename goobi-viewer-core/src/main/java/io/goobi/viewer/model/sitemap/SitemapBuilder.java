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
package io.goobi.viewer.model.sitemap;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.model.SitemapRequestParameters;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * Resource for sitemap generation.
 */
public class SitemapBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SitemapBuilder.class);


    private static Thread workerThread = null;

    private final HttpServletRequest servletRequest;

    public SitemapBuilder(HttpServletRequest request) {
        this.servletRequest = request;
    }

    public void updateSitemap(SitemapRequestParameters params) throws AccessDeniedException, IllegalRequestException, InterruptedException, JSONException, PresentationException {

        JSONObject ret = new JSONObject();

        if (params == null) {
            throw new IllegalRequestException("Invalid JSON request object");
//            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
//            ret.put("message", "Invalid JSON request object");
//            return ret.toString();
        }

        Sitemap sitemap = new Sitemap();
        String outputPath = params.getOutputPath();
        if (outputPath == null) {
            outputPath = servletRequest.getServletContext().getRealPath("/");
        }
        final String passOutputPath = outputPath;

        if (workerThread == null || !workerThread.isAlive()) {
            workerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        List<File> sitemapFiles = sitemap.generate(servletRequest, passOutputPath);
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
                        logger.debug("PresentationException thrown here: {}", e.getMessage());
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
            workerThread.join();
            if(!Integer.valueOf(HttpServletResponse.SC_OK).equals(ret.getInt("status"))) {
                throw new PresentationException(ret.getString("message"));
            }
        } else {
            throw new AccessDeniedException("Sitemap generation currently in progress");
        }

    }
}
