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

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.digiverso.presentation.servlets.oembed.RichOEmbedResponse;

/**
 * Servlet for original content file download.
 */
public class OEmbedServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1507603814345643281L;

    private static final Logger logger = LoggerFactory.getLogger(OEmbedServlet.class);

    /**
     * @see HttpServlet#HttpServlet()
     */
    public OEmbedServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = null;
        String maxWidth = null;
        String maxHeight = null;
        String format = "json";

        if (request.getParameterMap().size() > 0) {
            Set<String> keys = request.getParameterMap().keySet();
            for (String s : keys) {
                String[] values = request.getParameterMap().get(s);
                if (values[0] != null) {
                    switch (s) {
                        case "url":
                            url = values[0];
                            break;
                        case "maxWidth":
                            maxWidth = values[0];
                            break;
                        case "maxHeight":
                            maxHeight = values[0];
                            break;
                        case "format":
                            format = values[0];
                            break;
                    }
                }
            }
        }
        if (url == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: url");
            return;
        }

        // Check access conditions, if an actual document with a PI is involved
        boolean access = true;
        //        try {
        //            access = !AccessConditionUtils.checkContentFileAccessPermission(pi, request, Collections.singletonList(path))
        //                    .containsValue(Boolean.FALSE);
        if (!access) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        try {
            String ret = "TODO";
            switch (format) {
                case "json":
                    response.setContentType("application/json");
                    break;
                case "xml":
                    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "XML is not yet supported");
                    return;
            }

            RichOEmbedResponse oembed = new RichOEmbedResponse();
            oembed.setType("rich");
            oembed.setHtml("<div></div>");

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_NULL);
            ret = mapper.writeValueAsString(oembed);
            response.getWriter().write(ret);
        } catch (ClientAbortException | SocketException e) {
            logger.warn("Client {} has abborted the connection: {}", request.getRemoteAddr(), e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        //        } catch (IndexUnreachableException e) {
        //            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        //            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        //            return;
        //        } catch (DAOException e) {
        //            logger.debug("DAOException thrown here: {}", e.getMessage());
        //            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        //            return;
        //        } catch (PresentationException e) {
        //            logger.debug("PresentationException thrown here: {}", e.getMessage());
        //            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        //            return;
        //        }

    }
}
