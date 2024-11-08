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

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetAction;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.servlets.oembed.OEmbedRecord;
import io.goobi.viewer.servlets.oembed.OEmbedResponse;
import io.goobi.viewer.servlets.oembed.PhotoOEmbedResponse;
import io.goobi.viewer.servlets.oembed.RichOEmbedResponse;

/**
 * Servlet for original content file download.
 */
public class OEmbedServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1507603814345643281L;

    private static final Logger logger = LogManager.getLogger(OEmbedServlet.class);

    /**
     * <p>
     * Constructor for OEmbedServlet.
     * </p>
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
        Integer maxWidth = null;
        Integer maxHeight = null;
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
                            try {
                                maxWidth = Integer.parseInt(values[0]);
                            } catch (NumberFormatException e) {
                                logger.warn("'maxWidth' paraneter is not an integer: {}", values[0]);
                            }
                            break;
                        case "maxHeight":
                            try {
                                maxHeight = Integer.parseInt(values[0]);
                            } catch (NumberFormatException e) {
                                logger.warn("'maxHeight' paraneter is not an integer: {}", values[0]);
                            }
                            break;
                        case "format":
                            format = values[0];
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        if (url == null) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: url");
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }

        OEmbedRecord rec = null;
        try {
            rec = parseUrl(url);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not parse URL");
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
            return;
        } catch (PresentationException | IndexUnreachableException e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not parse URL");
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
            return;
        }
        if (rec == null) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not load record described by the URL");
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
            return;
        }

        // Check access conditions, if an actual document with a PI is involved
        boolean access = true;
        if (!access) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
            return;
        }

        try {
            switch (format) {
                case "json":
                    response.setContentType("application/json");
                    break;
                case "xml":
                    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "XML is not yet supported");
                    return;
                default:
                    return;
            }

            OEmbedResponse oembedResponse;
            if (rec.isRichResponse()) {
                oembedResponse = new RichOEmbedResponse(rec, maxWidth, maxHeight);
            } else {
                oembedResponse = new PhotoOEmbedResponse(rec);
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_NULL);
            String ret = mapper.writeValueAsString(oembedResponse);
            response.getWriter().write(ret);
        } catch (SocketException e) {
            logger.trace("Client {} has abborted the connection: {}", request.getRemoteAddr(), e.getMessage());
        } catch (IOException e) {
            if (GetAction.isClientAbort(e)) {
                logger.trace("Client {} has abborted the connection: {}", request.getRemoteAddr(), e.getMessage());
            } else {
                logger.error(e.getMessage(), e);
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                } catch (IOException e1) {
                    logger.error(e1.getMessage());
                }
            }
        } catch (ViewerConfigurationException e) {
            logger.error(e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        }
    }

    /**
     * 
     * @param origUrl
     * @return {@link OEmbedRecord}
     * @throws URISyntaxException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should parse url with page number correctly
     * @should parse url without page number correctly
     * @should return null if url contains no pi
     */
    static OEmbedRecord parseUrl(String origUrl) throws URISyntaxException, PresentationException, IndexUnreachableException {
        if (origUrl == null) {
            return null;
        }

        URI uri = new URI(origUrl);
        logger.trace(uri.getPath());
        String url = uri.getPath().substring(1);
        url = url.replace("viewer/", "");

        String[] urlSplit = url.split("/");
        if (logger.isTraceEnabled()) {
            logger.trace(Arrays.toString(urlSplit));
        }

        if (urlSplit.length > 0 && "embed".equals(urlSplit[0])) {
            return new OEmbedRecord(origUrl);
        }
        if (urlSplit.length < 2) {
            return null;
        }
        String pi = urlSplit[1];
        int page = urlSplit.length > 2 ? Integer.parseInt(urlSplit[2]) : 1;
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(pi);
        if (iddoc == null) {
            return null;
        }

        OEmbedRecord ret = new OEmbedRecord();
        StructElement se = new StructElement(iddoc);
        se.setPi(pi);
        ret.setStructElement(se);
        if (se.isAnchor() || se.isGroup()) {
            StructElement seChild = se.getFirstVolume(new ArrayList<>(ThumbnailHandler.REQUIRED_SOLR_FIELDS));
            PhysicalElement pe = AbstractPageLoader.loadPage(seChild, page);
            ret.setPhysicalElement(pe);
        } else {
            PhysicalElement pe = AbstractPageLoader.loadPage(se, page);
            ret.setPhysicalElement(pe);
        }

        return ret;
    }
}
