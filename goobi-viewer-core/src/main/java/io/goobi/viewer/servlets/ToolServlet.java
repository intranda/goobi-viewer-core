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
package io.goobi.viewer.servlets;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.util.CacheUtils;
import io.goobi.viewer.Version;
import io.goobi.viewer.controller.SolrSearchIndex;

/**
 * Servlet for deleting cache elements. Should not be accessible to unauthorized persons. This is a temporary solutions which will probably be
 * replaced with some kind of GUI later.
 */
public class ToolServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = -2888790425901398519L;

    //    private static JobManager cacheFiller = new JobManager();

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(ToolServlet.class);

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = null;
        String identifier = null;
        boolean fromContentCache = false;
        boolean fromThumbnailCache = false;
        boolean fromPdfCache = false;

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
                case "fillCache":
                    //                    String answer = performCacheFillerAction(request.getParameterMap());
                    //                    String returnString = answer.trim().replaceAll("\\n", "<br>").replaceAll("\\t", "&#160;&#160;&#160;&#160;");
                    //
                    //                    response.setContentType("text/html"); {
                    //                    ServletOutputStream output = response.getOutputStream();
                    //                    output.write(returnString.getBytes(Charset.forName("utf-8")));
                    //                }
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
                    response.setContentType("application/json"); {
                    ServletOutputStream output = response.getOutputStream();
                    output.write(Version.asJSON().getBytes(Charset.forName("utf-8")));
                }
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                    break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
