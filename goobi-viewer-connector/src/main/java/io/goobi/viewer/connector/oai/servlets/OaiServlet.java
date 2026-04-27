/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.oai.servlets;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.XMLOutputter;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.oai.model.formats.Format;
import io.goobi.viewer.connector.utils.SolrSearchTools;
import io.goobi.viewer.connector.utils.Utils;

/**
 * <p>
 * OaiServlet class.
 * </p>
 *
 */
public class OaiServlet extends HttpServlet {

    private static final long serialVersionUID = -2357047964682340928L;

    private static final Logger logger = LogManager.getLogger(OaiServlet.class);

    /** {@inheritDoc} */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");

        String queryString = (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        if (logger.isDebugEnabled()) {
            logger.debug("REQUEST URL: {}{}", request.getRequestURL(), queryString);
        }

        String filterQuerySuffix = "";
        filterQuerySuffix = SolrSearchTools.getAllSuffixes(request);
        // logger.trace("filterQuerySuffix: {}",filterQuerySuffix);

        Document doc = new Document();
        ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type='text/xsl' href='./oai2.xsl'");

        doc.addContent(pi);
        // generate root element
        Element root = Format.getOaiPmhElement("OAI-PMH");

        Element responseDate = new Element("responseDate", Format.OAI_NS);

        responseDate.setText(Utils.getCurrentUTCTime(LocalDateTime.now()));
        root.addContent(responseDate);

        RequestHandler handler = new RequestHandler(request);

        // handle request
        if (handler.getVerb() == null) {
            Element requestType = new Element("request", Format.OAI_NS);
            requestType.setAttribute("verb", "missing");
            if (DataManager.getInstance().getConfiguration().isBaseUrlUseInRequestElement()) {
                requestType.setText(DataManager.getInstance().getConfiguration().getBaseURL());
            } else {
                requestType.setText(request.getRequestURL().toString().replace("/M2M/", "/viewer/"));
            }
            root.addContent(requestType);
            root.addContent(new ErrorCode().getBadVerb());
        } else if (!checkDatestamps(handler.getFrom(), handler.getUntil())) {
            // Check for invalid from/until parameters
            logger.trace("Invalid timestamps");
            root.addContent(new ErrorCode().getBadArgument());
        } else {
            Element requestType = new Element("request", Format.OAI_NS);
            requestType.setAttribute("verb", handler.getVerb().getTitle());
            if (handler.getMetadataPrefix() != null) {
                requestType.setAttribute("metadataPrefix", handler.getMetadataPrefix().getMetadataPrefix());
            }
            if (StringUtils.isNotEmpty(handler.getIdentifier())) {
                requestType.setAttribute("identifier", handler.getIdentifier());
            }
            if (StringUtils.isNotEmpty(handler.getFrom())) {
                requestType.setAttribute("from", handler.getFrom());
            }
            if (StringUtils.isNotEmpty(handler.getUntil())) {
                requestType.setAttribute("until", handler.getUntil());
            }
            if (StringUtils.isNotEmpty(handler.getSet())) {
                requestType.setAttribute("set", handler.getSet());
            }
            if (DataManager.getInstance().getConfiguration().isBaseUrlUseInRequestElement()) {
                requestType.setText(DataManager.getInstance().getConfiguration().getBaseURL());
            } else {
                requestType.setText(request.getRequestURL().toString().replace("/M2M/", "/viewer/"));
            }
            root.addContent(requestType);
            //  resumptionToken
            if (request.getParameter("resumptionToken") != null) {
                String resumptionToken = request.getParameterValues("resumptionToken")[0];
                requestType.setAttribute("resumptionToken", resumptionToken);
                root.addContent(Format.handleToken(resumptionToken, filterQuerySuffix));
                Format.removeExpiredTokens();
            } else {
                switch (handler.getVerb()) {
                    case IDENTIFY:
                        try {
                            root.addContent(Format.getIdentifyXML(filterQuerySuffix));
                        } catch (IOException | SolrServerException e) {
                            logger.error(e.getMessage(), e);
                            try {
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                            } catch (IOException e1) {
                                logger.error(e1.getMessage());
                            }
                            return;
                        }
                        break;
                    case LISTIDENTIFIERS:
                        logger.debug("ListIdentifiers");
                        if (handler.getMetadataPrefix() == null) {
                            root.addContent(new ErrorCode().getBadArgument());
                        } else if (!DataManager.getInstance()
                                .getConfiguration()
                                .isMetadataFormatEnabled(handler.getMetadataPrefix().getMetadataPrefix())) {
                            // Deny access to disabled formats
                            root.addContent(new ErrorCode().getCannotDisseminateFormat());
                        } else {
                            try {
                                int hitsPerToken =
                                        DataManager.getInstance()
                                                .getConfiguration()
                                                .getHitsPerTokenForMetadataFormat(handler.getMetadataPrefix().getMetadataPrefix());
                                String versionDiscriminatorField = DataManager.getInstance()
                                        .getConfiguration()
                                        .getVersionDisriminatorFieldForMetadataFormat(handler.getMetadataPrefix().getMetadataPrefix());
                                Format format = Format.getFormatByMetadataPrefix(handler.getMetadataPrefix());
                                if (format != null) {
                                    root.addContent(
                                            format.createListIdentifiers(handler, 0, 0, hitsPerToken, versionDiscriminatorField, filterQuerySuffix));
                                } else {
                                    root.addContent(new ErrorCode().getBadArgument());
                                }
                            } catch (IOException | SolrServerException e) {
                                logger.error(e.getMessage(), e);
                                try {
                                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                                } catch (IOException e1) {
                                    logger.error(e1.getMessage());
                                }
                                return;
                            }
                        }
                        break;
                    case LISTRECORDS:
                        if (handler.getMetadataPrefix() == null) {
                            root.addContent(new ErrorCode().getBadArgument());
                        } else if (!DataManager.getInstance()
                                .getConfiguration()
                                .isMetadataFormatEnabled(handler.getMetadataPrefix().getMetadataPrefix())) {
                            // Deny access to disabled formats
                            root.addContent(new ErrorCode().getCannotDisseminateFormat());
                        } else {
                            if (handler.getUntil() == null) {
                                String until = Utils.convertDate(System.currentTimeMillis());
                                handler.setUntil(until);
                                logger.debug("No 'until' parameter, setting 'now' ({})", until);
                            }
                            try {
                                int hitsPerToken =
                                        DataManager.getInstance()
                                                .getConfiguration()
                                                .getHitsPerTokenForMetadataFormat(handler.getMetadataPrefix().getMetadataPrefix());
                                String versionDiscriminatorField = DataManager.getInstance()
                                        .getConfiguration()
                                        .getVersionDisriminatorFieldForMetadataFormat(handler.getMetadataPrefix().getMetadataPrefix());
                                logger.trace(handler.getMetadataPrefix().getMetadataPrefix());
                                Format format = Format.getFormatByMetadataPrefix(handler.getMetadataPrefix());
                                if (format != null) {
                                    root.addContent(
                                            format.createListRecords(handler, 0, 0, hitsPerToken, versionDiscriminatorField, filterQuerySuffix));
                                } else {
                                    root.addContent(new ErrorCode().getBadArgument());
                                }
                            } catch (IOException | SolrServerException e) {
                                logger.error(e.getMessage(), e);
                                try {
                                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                                } catch (IOException e1) {
                                    logger.error(e1.getMessage());
                                }
                                return;
                            }
                        }
                        break;
                    case GETRECORD:
                        if (handler.getMetadataPrefix() == null) {
                            root.addContent(new ErrorCode().getBadArgument());
                        } else if (!DataManager.getInstance()
                                .getConfiguration()
                                .isMetadataFormatEnabled(handler.getMetadataPrefix().getMetadataPrefix())) {
                            // Deny access to disabled formats
                            root.addContent(new ErrorCode().getCannotDisseminateFormat());
                        } else {
                            Format format = Format.getFormatByMetadataPrefix(handler.getMetadataPrefix());
                            if (format != null) {
                                root.addContent(format.createGetRecord(handler, filterQuerySuffix));
                            } else {
                                root.addContent(new ErrorCode().getBadArgument());
                            }
                        }
                        break;
                    case LISTMETADATAFORMATS:
                        root.addContent(Format.createMetadataFormats());
                        break;
                    case LISTSETS:
                        try {
                            root.addContent(Format.createListSets(DataManager.getInstance().getConfiguration().getDefaultLocale()));
                        } catch (IOException | SolrServerException e) {
                            logger.error(e.getMessage(), e);
                            try {
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                            } catch (IOException e1) {
                                logger.error(e1.getMessage());
                            }
                            return;
                        }
                        break;
                    default:
                        root.addContent(new ErrorCode().getBadArgument());
                }
            }
        }
        doc.setRootElement(root);
        org.jdom2.output.Format format = org.jdom2.output.Format.getPrettyFormat();
        format.setEncoding("utf-8");
        XMLOutputter xmlOut = new XMLOutputter(format);
        try {
            if (handler.getMetadataPrefix() != null && handler.getMetadataPrefix().equals(Metadata.EPICUR)) {
                String ueblerhack = xmlOut.outputString(doc);
                ueblerhack = ueblerhack.replace("<epicur", "<epicur xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
                response.setCharacterEncoding("utf-8");
                ServletOutputStream out = response.getOutputStream();
                out.print(ueblerhack);
            } else {
                xmlOut.output(doc, response.getOutputStream());
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        }
    }

    /**
     * <p>
     * checkDatestamps.
     * </p>
     *
     * @param from a {@link java.lang.String} object.
     * @param until a {@link java.lang.String} object.
     * @return false if from after until; true otherwise
     * @should return false if from is not well formed
     * @should return false if until is not well formed
     * @should return false if from after until
     * @should return true if from and until correct
     * @should return false if from and until different types
     * @should return true if only one datestamp given
     */
    public static boolean checkDatestamps(String from, String until) {
        boolean fromJustDate = false;
        boolean untilJustDate = false;
        LocalDateTime ldtFrom = null;
        LocalDateTime ldtUntil = null;
        if (from != null) {
            if (from.contains("T")) {
                // Date/time
                try {
                    ldtFrom = LocalDateTime.parse(from, Utils.FORMATTER_ISO8601_DATETIME_WITH_OFFSET);
                } catch (DateTimeParseException e) {
                    logger.warn(e.getMessage());
                    return false;
                }
            } else {
                // Just date
                try {
                    ldtFrom = LocalDate.parse(from, Utils.FORMATTER_ISO8601_DATE).atStartOfDay();
                    fromJustDate = true;
                } catch (DateTimeParseException e) {
                    logger.warn(e.getMessage());
                    return false;
                }
            }
        }
        if (until != null) {
            if (until.contains("T")) {
                // Date/time
                try {
                    ldtUntil = LocalDateTime.parse(until, Utils.FORMATTER_ISO8601_DATETIME_WITH_OFFSET);
                } catch (DateTimeParseException e) {
                    logger.warn(e.getMessage());
                    return false;
                }
            } else {
                // Just date
                try {
                    ldtUntil = LocalDate.parse(until, Utils.FORMATTER_ISO8601_DATE).atStartOfDay();
                    untilJustDate = true;
                } catch (DateTimeParseException e) {
                    logger.warn(e.getMessage());
                    return false;
                }
            }
        }
        // Different types not allowed
        if (from != null && until != null && fromJustDate != untilJustDate) {
            return false;
        }
        if (ldtFrom != null && ldtUntil != null) {
            try {
                return !ldtFrom.isAfter(ldtUntil);
            } catch (DateTimeParseException e) {
                logger.warn(e.getMessage());
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            doGet(request, response);
        } catch (IOException | ServletException e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        }
    }
}
