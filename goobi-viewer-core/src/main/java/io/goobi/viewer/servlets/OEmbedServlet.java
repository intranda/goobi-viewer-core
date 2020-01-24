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
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
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

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.servlets.oembed.OEmbedRecord;
import io.goobi.viewer.servlets.oembed.OEmbedResponse;
import io.goobi.viewer.servlets.oembed.PhotoOEmbedResponse;

/**
 * Servlet for original content file download.
 */
public class OEmbedServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1507603814345643281L;

    private static final Logger logger = LoggerFactory.getLogger(OEmbedServlet.class);

    /**
     * <p>Constructor for OEmbedServlet.</p>
     *
     * @see HttpServlet#HttpServlet()
     */
    public OEmbedServlet() {
        super();
    }

    /** {@inheritDoc} */
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

        OEmbedRecord record = null;
        try {
            record = parseUrl(url);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not parse URL");
            return;
        } catch (PresentationException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not parse URL");
            return;
        } catch (IndexUnreachableException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not parse URL");
            return;
        }
        if (record == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not load record described by the URL");
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

            // OEmbedResponse oembedResponse = new RichOEmbedResponse(record);
            OEmbedResponse oembedResponse = new PhotoOEmbedResponse(record);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_NULL);
            ret = mapper.writeValueAsString(oembedResponse);
            response.getWriter().write(ret);
        } catch (ClientAbortException | SocketException e) {
            logger.warn("Client {} has abborted the connection: {}", request.getRemoteAddr(), e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (ViewerConfigurationException e) {
            logger.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    static OEmbedRecord parseUrl(String url) throws URISyntaxException, PresentationException, IndexUnreachableException {
        if (url == null) {
            return null;
        }

        URI uri = new URI(url);
        logger.trace(uri.getPath());
        url = uri.getPath().substring(1);
        url = url.replace("viewer/", "");

        String[] urlSplit = url.split("/");
        logger.trace(Arrays.toString(urlSplit));
        String pi = urlSplit[1];
        int page = Integer.valueOf(urlSplit[2]);
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(pi);
        if (iddoc == 0) {
            return null;
        }

        OEmbedRecord ret = new OEmbedRecord();
        StructElement se = new StructElement(iddoc);
        ret.setStructElement(se);
        PhysicalElement pe = AbstractPageLoader.loadPage(se, page);
        ret.setPhysicalElement(pe);

        return ret;
    }
}
