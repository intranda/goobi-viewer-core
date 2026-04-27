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
package io.goobi.viewer.connector.sru;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.XSLTransformException;
import org.jdom2.transform.XSLTransformer;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.exceptions.MissingArgumentException;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.utils.SolrSearchIndex;
import io.goobi.viewer.connector.utils.SolrSearchTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * SruServlet class.
 * </p>
 *
 */
public class SruServlet extends HttpServlet {

    private static final long serialVersionUID = -6396567784411891113L;
    private static final Logger logger = LogManager.getLogger(SruServlet.class);

    static final Namespace SRU_NAMESPACE = Namespace.getNamespace("srw", "http://www.loc.gov/zing/srw/");
    static final Namespace EXPLAIN_NAMESPACE = Namespace.getNamespace("ns", "http://explain.z3950.org/dtd/2.0/");
    static final Namespace DIAG_NAMESPACE = Namespace.getNamespace("diag", "http://www.loc.gov/zing/srw/diagnostic/");

    private static final Namespace METS_NAMESPACE = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
    private static final Namespace XSI_NAMESPACE = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    private static final Namespace MODS_NAMESPACE = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
    private static final Namespace DV_NAMESPACE = Namespace.getNamespace("dv", "http://dfg-viewer.de/");
    private static final Namespace XLINK_NAMESPACE = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
    private static final Namespace MARC_NAMEPSACE = Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim");
    private static final Namespace DC_NAMEPSACE = Namespace.getNamespace("dc", "info:srw/schema/1/dc-schema");
    private static final Namespace LIDO_NAMESPACE = Namespace.getNamespace("lido", "http://www.lido-schema.org");

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");

        SruRequestParameter parameter = null;
        try {
            parameter = new SruRequestParameter(request);
        } catch (MissingArgumentException e) {
            try {
                if (e.getMessage().contains(SruRequestParameter.PARAM_VERSION)) {
                    missingArgument(parameter, response, SruRequestParameter.PARAM_VERSION);
                } else if (e.getMessage().contains(SruRequestParameter.PARAM_OPERATION)) {
                    missingArgument(parameter, response, SruRequestParameter.PARAM_OPERATION);
                } else {
                    missingArgument(parameter, response, "");
                }
            } catch (IOException e1) {
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
                } catch (IOException e2) {
                    logger.error(e2.getMessage());
                }
            }
            logger.debug(e.getMessage());
            return;
        }

        Document doc = new Document();
        if (parameter.getStylesheet() != null && !parameter.getStylesheet().isEmpty()) {
            ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type='text/xsl' href='" + parameter.getStylesheet() + "'");
            doc.addContent(pi);
            // logger.trace("Added stylesheet '{}' to SRU output.", parameter.getStylesheet()); //NOSONAR Debug
        }

        switch (parameter.getOperation()) {
            case SEARCHRETRIEVE:
                logger.trace("operation is searchRetrieve");
                if (parameter.getQuery() == null || parameter.getQuery().isEmpty()) {
                    try {
                        missingArgument(parameter, response, "query");
                    } catch (IOException e) {
                        try {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }
                    }
                    logger.info("cannot process request {}, parameter 'query' is missing.", request.getQueryString());
                    return;
                }

                if (parameter.getRecordSchema() == null) {
                    try {
                        wrongSchema(parameter, response, request.getParameter("recordSchema"));
                    } catch (IOException e) {
                        try {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }
                    }
                    return;
                }
                try {
                    String filterQuerySuffix = SolrSearchTools.getAllSuffixes(request);
                    Element searchRetrieve = generateSearchRetrieve(parameter, DataManager.getInstance().getSearchIndex(), filterQuerySuffix);
                    doc.setRootElement(searchRetrieve);
                } catch (IOException | SolrServerException e) {
                    logger.error(e.getMessage());
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    } catch (IOException e1) {
                        logger.error(e1.getMessage());
                    }
                    return;
                }
                break;
            case EXPLAIN:
                logger.trace("operation is explain");
                Element rootElement = getExplain(request, parameter);
                doc.setRootElement(rootElement);
                break;
            case SCAN:
                logger.trace("operation is scan");
                if (parameter.getScanClause() == null || parameter.getScanClause().isEmpty()) {
                    try {
                        missingArgument(parameter, response, "scanClause");
                    } catch (IOException e) {
                        try {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        } catch (IOException e1) {
                            logger.error(e1.getMessage());
                        }
                    }
                    logger.info("Cannot process request {}, parameter 'scanClause' is missing.", request.getQueryString());
                    return;
                }
                // http://aleph20.ub.hu-berlin.de:5661/hub01?version=1.1&operation=scan&scanClause=BV040552415&maximumRecords=1
                // http://services.dnb.de/sru/authorities?operation=scan&version=1.1&scanClause=Maximilian
                // http://s2w.visuallibrary.net/dps/sru/?operation=scan&version=1.1&scanClause=Augsburg
                // http://sru.gbv.de/opac-de-27?version=1.2&operation=scan&scanClause=Augsburg
                try {
                    unsupportedOperation(parameter, response, "scan");
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    } catch (IOException e1) {
                        logger.error(e1.getMessage());
                    }
                }
                return;
            case UNSUPPORTETPARAMETER:
            default:
                try {
                    unsupportedOperation(parameter, response, request.getParameter(SruRequestParameter.PARAM_OPERATION));
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    } catch (IOException e1) {
                        logger.error(e1.getMessage());
                    }
                }
                return;
        }

        Format format = Format.getPrettyFormat();
        format.setEncoding(StandardCharsets.UTF_8.name().toLowerCase());
        XMLOutputter xmlOut = new XMLOutputter(format);
        try {
            xmlOut.output(doc, response.getOutputStream());
        } catch (IOException e) {
            logger.error(e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        }

    }

    /**
     * 
     * @param parameter
     * @param solr
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return {@link Element}
     * @throws SolrServerException
     * @throws IOException
     * @should create element correctly
     */
    static Element generateSearchRetrieve(SruRequestParameter parameter, SolrSearchIndex solr, String filterQuerySuffix)
            throws SolrServerException, IOException {
        if (parameter == null) {
            throw new IllegalArgumentException("parameter may not be null");
        }

        Element root = new Element("searchRetrieveResponse", SRU_NAMESPACE);
        Element version = new Element(SruRequestParameter.PARAM_VERSION, SRU_NAMESPACE);
        version.setText(parameter.getVersion());
        root.addContent(version);

        String query = generateSearchQuery(parameter.getQuery(), parameter.getRecordSchema(), filterQuerySuffix);
        QueryResponse queryResponse =
                solr.search(query, parameter.getStartRecord() - 1, parameter.getStartRecord() - 1 + parameter.getMaximumRecords(), null, null, null);
        SolrDocumentList solrDocuments = queryResponse.getResults();
        Element numberOfRecords = new Element("numberOfRecords", SRU_NAMESPACE);
        if (solrDocuments == null || solrDocuments.isEmpty()) {
            numberOfRecords.setText("0");
            logger.debug("Searched for {}, found {} documents.", query, 0);
        } else {
            numberOfRecords.setText(String.valueOf(solrDocuments.size()));
            logger.debug("Searched for {}, found {} documents.", query, solrDocuments.size());
        }
        root.addContent(numberOfRecords);

        Element records = new Element("records", SRU_NAMESPACE);
        root.addContent(records);

        generateRecords(records, solrDocuments, parameter, solr, filterQuerySuffix);
        generateEchoedSearchRetrieveRequest(root, parameter);

        return root;
    }

    /**
     * Creates wrong schema error XML and writes it into response.
     * 
     * @param parameter
     * @param response
     * @param schema
     * @throws IOException
     */
    private static void wrongSchema(SruRequestParameter parameter, HttpServletResponse response, String schema) throws IOException {
        Document doc = createWrongSchemaDocument(parameter != null ? parameter.getVersion() : "?", schema);
        Format format = Format.getPrettyFormat();
        format.setEncoding(StandardCharsets.UTF_8.name().toLowerCase());
        XMLOutputter xmlOut = new XMLOutputter(format);
        xmlOut.output(doc, response.getOutputStream());
    }

    /**
     * Creates wrong schema error {@link Document}
     * 
     * @param version
     * @param schema
     * @return {@link Document}
     * @should create document correctly
     */
    static Document createWrongSchemaDocument(String version, String schema) {
        return createErrorResponseDocument(version, "info:srw/diagnostic/1/66", "Unknown schema for retrieval", schema);
    }

    /**
     * @param parameter SRU request parameter object
     * @param response
     * @param missing Name of the missing parameter
     * @throws IOException
     */
    private static void missingArgument(SruRequestParameter parameter, HttpServletResponse response, String missing) throws IOException {
        Document doc = createMissingArgumentDocument(parameter != null ? parameter.getVersion() : "?", missing);
        Format format = Format.getPrettyFormat();
        format.setEncoding(StandardCharsets.UTF_8.name().toLowerCase());
        XMLOutputter xmlOut = new XMLOutputter(format);
        xmlOut.output(doc, response.getOutputStream());
    }

    /**
     * 
     * Creates missing argument error {@link Document}
     * 
     * @param version
     * @param missing
     * @return {@link Document}
     * @should create document correctly
     */
    static Document createMissingArgumentDocument(String version, String missing) {
        return createErrorResponseDocument(version, "info:srw/diagnostic/1/7",
                "Mandatory parameter not supplied", missing);
    }

    /**
     * @param parameter
     * @param response
     * @param operation
     * @throws IOException
     */
    static void unsupportedOperation(SruRequestParameter parameter, HttpServletResponse response, String operation) throws IOException {
        Document doc = createUnsupportedOperationDocument(parameter != null ? parameter.getVersion() : "?", operation);
        Format format = Format.getPrettyFormat();
        format.setEncoding(StandardCharsets.UTF_8.name().toLowerCase());
        XMLOutputter xmlOut = new XMLOutputter(format);
        xmlOut.output(doc, response.getOutputStream());
    }

    /**
     * 
     * @param version
     * @param operation
     * @return {@link Document}
     * @should create document correctly
     */
    static Document createUnsupportedOperationDocument(String version, String operation) {
        return createErrorResponseDocument(version, "info:srw/diagnostic/1/4", "Unsupported operation", operation);
    }

    /**
     * Creates an error XML {@link Document} with given content.
     * 
     * @param version SRU protocol version
     * @param uriText Text for the uri element
     * @param detailsText Text for the details element
     * @param arg Problematic argument
     * @return {@link Document}
     */
    static Document createErrorResponseDocument(String version, String uriText, String detailsText, String arg) {
        Element searchRetrieveResponse = new Element("searchRetrieveResponse", SRU_NAMESPACE);
        Element eleVersion = new Element(SruRequestParameter.PARAM_VERSION, SRU_NAMESPACE);
        eleVersion.setText(version);
        searchRetrieveResponse.addContent(eleVersion);
        Element diagnostic = new Element("diagnostic", SRU_NAMESPACE);
        searchRetrieveResponse.addContent(diagnostic);

        Element uri = new Element("uri", DIAG_NAMESPACE);
        uri.setText(uriText);
        diagnostic.addContent(uri);

        Element details = new Element("details", DIAG_NAMESPACE);
        details.setText(detailsText);
        diagnostic.addContent(details);

        Element message = new Element("message", DIAG_NAMESPACE);
        message.setText(detailsText + " / " + arg);
        diagnostic.addContent(message);

        Document ret = new Document();
        ret.setRootElement(searchRetrieveResponse);

        return ret;
    }

    /**
     * @param sruQuery
     * @param recordSchema
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return Generated query
     * @should throw {@link IllegalArgumentException} if sruQuery null
     * @should create query correctly
     */
    static String generateSearchQuery(String sruQuery, Metadata recordSchema, String filterQuerySuffix) {
        if (sruQuery == null) {
            throw new IllegalArgumentException("sruQuery may not be null");
        }

        // map dc queries to solr queries
        StringBuilder sbValue = new StringBuilder();
        String initValue = sruQuery;
        for (Matcher m = Pattern.compile("dc.\\w+").matcher(initValue); m.find();) {
            String searchParameter = m.toMatchResult().group();
            SearchField sf = SearchField.getFieldByDcName(searchParameter);
            if (sf != null) {
                initValue = initValue.replaceAll(searchParameter + "\\s*=", sf.getSolrName() + ":");
            }
        }
        // map iv queries to solr queries TODO This is probably unused
        //        for (Matcher m = Pattern.compile("iv.(\\w+)").matcher(initValue); m.find();) {
        //            String searchParameter = m.toMatchResult().group(1);
        //            SearchField sf = SearchField.getFieldByInternalName(searchParameter);
        //            if (sf != null) {
        //                initValue = initValue.replaceAll(searchParameter + "\\s*=", sf.getSolrName() + ":");
        //            }
        //        }
        // map cql fields to solr queries
        for (Matcher m = Pattern.compile("(\\w{1,20})\\s?=").matcher(initValue); m.find();) {
            String searchParameter = m.toMatchResult().group();
            String param = searchParameter.replace("=", "").trim();
            SearchField sf = SearchField.getFieldByCqlName(param);
            if (sf != null) {
                initValue = initValue.replaceAll(searchParameter, sf.getSolrName() + ":");
            }
        }
        sbValue.append(initValue);

        if (recordSchema != null) {
            switch (recordSchema) {
                case LIDO:
                    sbValue.append(" AND ").append(SolrConstants.SOURCEDOCFORMAT).append(":LIDO");
                    break;
                case MARCXML:
                case MODS:
                case METS:
                    sbValue.append(" AND ").append(SolrConstants.SOURCEDOCFORMAT).append(":METS");
                    break;
                default:
                    break;
            }
        }
        sbValue.append(" AND (")
                .append(SolrConstants.ISWORK)
                .append(":true OR ")
                .append(SolrConstants.ISANCHOR)
                .append(":true)")
                .append(filterQuerySuffix != null ? filterQuerySuffix : "");
        logger.trace(sbValue);
        return sbValue.toString();
    }

    /**
     * @param root
     * @param parameter
     */
    static void generateEchoedSearchRetrieveRequest(Element root, SruRequestParameter parameter) {
        Element echoedSearchRetrieveRequest = new Element("echoedSearchRetrieveRequest", SRU_NAMESPACE);
        root.addContent(echoedSearchRetrieveRequest);

        Element version = new Element(SruRequestParameter.PARAM_VERSION, SRU_NAMESPACE);
        version.setText(parameter.getVersion());
        echoedSearchRetrieveRequest.addContent(version);

        Element recordSchema = new Element("recordSchema", SRU_NAMESPACE);
        recordSchema.setText(parameter.getRecordSchema().getMetadataPrefix());
        echoedSearchRetrieveRequest.addContent(recordSchema);

        Element query = new Element("query", SRU_NAMESPACE);
        query.setText(parameter.getQuery());
        echoedSearchRetrieveRequest.addContent(query);

        Element recordPacking = new Element("recordPacking", SRU_NAMESPACE);
        recordPacking.setText(parameter.getRecordPacking());
        echoedSearchRetrieveRequest.addContent(recordPacking);

        Element startRecord = new Element("startRecord", SRU_NAMESPACE);
        startRecord.setText(String.valueOf(parameter.getStartRecord()));
        echoedSearchRetrieveRequest.addContent(startRecord);

        Element maximumRecords = new Element("maximumRecords", SRU_NAMESPACE);
        maximumRecords.setText(String.valueOf(parameter.getMaximumRecords()));
        echoedSearchRetrieveRequest.addContent(maximumRecords);

    }

    /**
     * @param records
     * @param solrDocuments
     * @param parameter
     * @param solr
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @throws SolrServerException
     * @throws IOException
     */
    private static void generateRecords(Element records, List<SolrDocument> solrDocuments, SruRequestParameter parameter,
            SolrSearchIndex solr, String filterQuerySuffix) throws SolrServerException, IOException {
        if (solrDocuments == null || solrDocuments.isEmpty()) {
            return;
        }

        for (SolrDocument document : solrDocuments) {
            Element rec = new Element("record", SRU_NAMESPACE);
            records.addContent(rec);

            Element recordSchema = new Element("recordSchema", SRU_NAMESPACE);
            recordSchema.setText(parameter.getRecordSchema().getMetadataPrefix());
            rec.addContent(recordSchema);

            Element recordPacking = new Element("recordPacking", SRU_NAMESPACE);
            recordPacking.setText(parameter.getRecordPacking());
            rec.addContent(recordPacking);

            Element recordData = new Element("recordData", SRU_NAMESPACE);
            rec.addContent(recordData);

            if ("1.2".equals(parameter.getVersion())) {
                Element recordIdentifier = new Element("recordIdentifier", SRU_NAMESPACE);
                recordIdentifier.setText(SolrTools.getSingleFieldStringValue(document, SolrConstants.PI_TOPSTRUCT));
                rec.addContent(recordIdentifier);
            }

            switch (parameter.getRecordSchema()) {
                case SOLR:
                    generateSolrRecord(document, recordData);
                    break;
                case METS:
                    generateMetsRecord(document, recordData);
                    break;
                case MODS:
                    generateModsRecord(document, recordData);
                    break;
                case MARCXML:
                    generateMarcxmlRecord(document, recordData);
                    break;
                case DC:
                    generateDcRecord(document, recordData, solr, filterQuerySuffix);
                    break;
                case LIDO:
                    generateLidoRecord(document, recordData);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @param doc
     * @param recordData
     */
    private static void generateLidoRecord(SolrDocument doc, Element recordData) {
        String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl())
                .append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                .toString();
        try {
            String xml = NetTools.getWebContentGET(url);
            if (StringUtils.isEmpty(xml)) {
                return;
            }
            org.jdom2.Document xmlDoc = XmlTools.getDocumentFromString(xml, null);
            Element xmlRoot = xmlDoc.getRootElement();
            Element newLido = new Element(Metadata.LIDO.getMetadataPrefix(), LIDO_NAMESPACE);
            newLido.addNamespaceDeclaration(XSI_NAMESPACE);
            newLido.setAttribute(new Attribute("schemaLocation", "http://www.lido-schema.org http://www.lido-schema.org/schema/v1.0/lido-v1.0.xsd",
                    XSI_NAMESPACE));

            newLido.addContent(xmlRoot.cloneContent());
            recordData.addContent(newLido);
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        } catch (HTTPException e) {
            logger.error("{}: {}", e.getCode(), e.getMessage());
        }
    }

    /**
     * @param doc
     * @param recordData
     * @param solr
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @throws SolrServerException
     * @throws IOException
     */
    private static void generateDcRecord(SolrDocument doc, Element recordData, SolrSearchIndex solr, String filterQuerySuffix)
            throws SolrServerException, IOException {
        Element dc = new Element("record", DC_NAMEPSACE);

        String title = null;
        String creators = null;
        String publisher = null;
        String placepublish = null;
        String yearpublish = null;

        // creating Element <dc:title />
        Element eleDcTitle = new Element("title", DC_NAMEPSACE);
        if (doc.getFieldValues(SolrConstants.TITLE) != null) {
            title = (String) doc.getFieldValues("MD_TITLE").iterator().next();
        } else {
            title = "";
        }
        if (doc.getFieldValue("IDDOC_PARENT") != null) {
            // If this is a volume, add anchor title in front
            String anchorTitle = getAnchorTitle(doc, solr, filterQuerySuffix);
            if (anchorTitle != null) {
                title = anchorTitle + "; " + title;
            }
        }
        eleDcTitle.setText(title);
        dc.addContent(eleDcTitle);

        // creating Element <dc:creator />
        if (doc.getFieldValues(io.goobi.viewer.connector.oai.model.formats.Format.MD_CREATOR) != null) {
            StringBuilder sb = new StringBuilder();
            for (Object fieldValue : doc.getFieldValues(io.goobi.viewer.connector.oai.model.formats.Format.MD_CREATOR)) {
                String value = (String) fieldValue;
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(value);
                Element eleDcCreator = new Element("creator", DC_NAMEPSACE);
                eleDcCreator.setText(value);
                dc.addContent(eleDcCreator);
            }
            creators = sb.toString();
        }

        // creating <dc:subject />
        // if (doc.get("CREATOR") != null) {
        // in lucene there are two space between the strings -> every one must be an new xml subject element
        if (doc.getFieldValues(SolrConstants.DC) != null) {
            for (Object fieldValue : doc.getFieldValues(SolrConstants.DC)) {
                Element eleDcType = new Element("subject", DC_NAMEPSACE);
                if (((String) fieldValue).equals("")) {
                    eleDcType.setText((String) fieldValue);
                    dc.addContent(eleDcType);
                }
            }
        }

        // creating <dc:publisher .../>
        Element eleDcPublisher = new Element("publisher", DC_NAMEPSACE);
        if (doc.getFieldValues("MD_PUBLISHER") != null) {
            publisher = (String) doc.getFieldValues("MD_PUBLISHER").iterator().next();
            eleDcPublisher.setText(publisher);
        }
        dc.addContent(eleDcPublisher);

        if (doc.getFieldValues("MD_PLACEPUBLISH") != null) {
            placepublish = (String) doc.getFieldValues("MD_PLACEPUBLISH").iterator().next();
        }

        // creating <dc:date />
        Element eleDcYearPublish = new Element("date", DC_NAMEPSACE);
        if (doc.getFieldValues("MD_YEARPUBLISH") != null) {
            yearpublish = (String) doc.getFieldValues("MD_YEARPUBLISH").iterator().next();
            eleDcYearPublish.setText(yearpublish);
        }
        dc.addContent(eleDcYearPublish);

        // creting <dc:type />
        Element eleDcType = new Element("type", DC_NAMEPSACE);
        if (doc.getFieldValue(SolrConstants.DOCSTRCT) != null) {
            eleDcType.setText((String) doc.getFieldValue(SolrConstants.DOCSTRCT));
        }
        dc.addContent(eleDcType);
        // always create a standard type with the default dc:type
        eleDcType = new Element("type", DC_NAMEPSACE);
        eleDcType.setText("Text");
        dc.addContent(eleDcType);

        // creating <dc:format />
        // creating <dc:format /> second one appears always
        Element eleDcFormat = new Element("format", DC_NAMEPSACE);
        eleDcFormat.setText("image/jpeg");
        dc.addContent(eleDcFormat);

        eleDcFormat = new Element("format", DC_NAMEPSACE);
        eleDcFormat.setText("application/pdf");
        dc.addContent(eleDcFormat);

        // create <dc:identifier />
        if (doc.getFieldValue(SolrConstants.URN) != null && ((String) doc.getFieldValue(SolrConstants.URN)).length() > 0) {
            Element eleDcIdentifier = new Element("identifier", DC_NAMEPSACE);
            eleDcIdentifier.setText(DataManager.getInstance().getConfiguration().getUrnResolverUrl() + (String) doc.getFieldValue(SolrConstants.URN));
            dc.addContent(eleDcIdentifier);
        }

        // create <dc:source />
        Element eleDcSource = new Element("source", DC_NAMEPSACE);
        if (creators == null) {
            creators = "-";
        }
        if (title == null) {
            title = "-";
        }
        if (yearpublish == null) {
            yearpublish = "-";
        }
        if (placepublish == null) {
            placepublish = "-";
        }
        if (publisher == null) {
            publisher = "-";
        }
        String sourceString = creators + ": " + title + ", " + placepublish + ": " + publisher + " " + yearpublish + ".";
        eleDcSource.setText(sourceString);
        dc.addContent(eleDcSource);

        // create <dc:rights />
        Element eleDcRights = new Element("rights", DC_NAMEPSACE);
        eleDcRights.setText("Open Access");
        dc.addContent(eleDcRights);
        recordData.addContent(dc);
    }

    /**
     * 
     * @param doc
     * @param solr
     * @param filterQuerySuffix
     * @return {@link String}
     * @throws SolrServerException
     * @throws IOException
     */
    private static String getAnchorTitle(SolrDocument doc, SolrSearchIndex solr, String filterQuerySuffix) throws SolrServerException, IOException {
        String iddocParent = (String) doc.getFieldValue(SolrConstants.IDDOC_PARENT);
        SolrDocumentList hits = solr.search(SolrConstants.IDDOC + ":" + iddocParent, filterQuerySuffix);
        if (hits != null && !hits.isEmpty()) {
            return (String) hits.get(0).getFirstValue("MD_TITLE");
        }

        return null;
    }

    /**
     * @param document
     * @param recordData
     */
    private static void generateMarcxmlRecord(SolrDocument document, Element recordData) {
        String pi = (String) document.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl()).append(pi).toString();
        try {
            String xml = NetTools.getWebContentGET(url);
            if (StringUtils.isEmpty(xml)) {
                return;
            }
            org.jdom2.Document xmlDoc = XmlTools.getDocumentFromString(xml, null);
            Element xmlRoot = xmlDoc.getRootElement();
            Element newmods = new Element("mods", MODS_NAMESPACE);
            newmods.addNamespaceDeclaration(XSI_NAMESPACE);
            newmods.addNamespaceDeclaration(MODS_NAMESPACE);
            newmods.addNamespaceDeclaration(XLINK_NAMESPACE);
            newmods.setAttribute("schemaLocation", "http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-3.xsd", XSI_NAMESPACE);

            List<Element> dmdList = xmlRoot.getChildren("dmdSec", METS_NAMESPACE);
            if (dmdList != null && !dmdList.isEmpty()) {
                Element firstDmdSec = dmdList.get(0);
                Element mdWrap = firstDmdSec.getChild("mdWrap", METS_NAMESPACE);
                Element xmlData = mdWrap.getChild("xmlData", METS_NAMESPACE);
                Element modsElement = xmlData.getChild("mods", MODS_NAMESPACE);

                newmods.addContent(modsElement.cloneContent());

                newmods.addNamespaceDeclaration(MARC_NAMEPSACE);
                org.jdom2.Document marcDoc = new org.jdom2.Document();

                marcDoc.setRootElement(newmods);

                String xslt = DataManager.getInstance().getConfiguration().getMods2MarcXsl();

                try {
                    XSLTransformer transformer = new XSLTransformer(xslt);
                    org.jdom2.Document docTrans = transformer.transform(marcDoc);
                    Element root = docTrans.getRootElement();

                    root.setAttribute("schemaLocation", "http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd",
                            XSI_NAMESPACE);
                    recordData.addContent(root.cloneContent());
                } catch (XSLTransformException e) {
                    logger.warn(e.getMessage());
                }

                // Add PURL for this record as 856$u
                StringBuilder sbUrl = new StringBuilder(DataManager.getInstance().getConfiguration().getPiResolverUrl()).append(pi);
                Element eleUrl = new Element("datafield", MARC_NAMEPSACE);
                eleUrl.setAttribute("tag", "856");
                eleUrl.setAttribute("ind1", "4");
                eleUrl.setAttribute("ind2", " ");
                recordData.addContent(eleUrl);
                Element eleUrlSubfield = new Element("subfield", MARC_NAMEPSACE);
                eleUrlSubfield.setAttribute("code", "u");
                eleUrlSubfield.setText(sbUrl.toString());
                eleUrl.addContent(eleUrlSubfield);
            }
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        } catch (HTTPException e) {
            logger.error("{}: {}", e.getCode(), e.getMessage());
        }
    }

    /**
     * @param doc
     * @param recordData
     */
    private static void generateModsRecord(SolrDocument doc, Element recordData) {
        String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl())
                .append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                .toString();
        try {
            String xml = NetTools.getWebContentGET(url);
            if (StringUtils.isEmpty(xml)) {
                return;
            }
            org.jdom2.Document xmlDoc = XmlTools.getDocumentFromString(xml, null);
            Element xmlRoot = xmlDoc.getRootElement();
            Element newMods = new Element("mods", MODS_NAMESPACE);
            newMods.addNamespaceDeclaration(XSI_NAMESPACE);
            newMods.addNamespaceDeclaration(MODS_NAMESPACE);
            newMods.addNamespaceDeclaration(XLINK_NAMESPACE);
            newMods.setAttribute("schemaLocation", "http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-3.xsd", XSI_NAMESPACE);

            List<Element> dmdList = xmlRoot.getChildren("dmdSec", METS_NAMESPACE);
            if (dmdList != null && !dmdList.isEmpty()) {
                Element firstDmdSec = dmdList.get(0);
                Element mdWrap = firstDmdSec.getChild("mdWrap", METS_NAMESPACE);
                Element xmlData = mdWrap.getChild("xmlData", METS_NAMESPACE);
                Element modsElement = xmlData.getChild("mods", MODS_NAMESPACE);
                newMods.addContent(modsElement.cloneContent());
                recordData.addContent(newMods);
            }
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        } catch (HTTPException e) {
            logger.error("{}: {}", e.getCode(), e.getMessage());
        }
    }

    /**
     * @param doc
     * @param recordData
     */
    private static void generateMetsRecord(SolrDocument doc, Element recordData) {
        String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl())
                .append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                .toString();
        logger.trace("generateMetsRecord");
        try {
            String xml = NetTools.getWebContentGET(url);
            if (StringUtils.isEmpty(xml)) {
                return;
            }
            org.jdom2.Document xmlDoc = XmlTools.getDocumentFromString(xml, null);
            Element xmlRoot = xmlDoc.getRootElement();
            Element newMets = new Element(Metadata.METS.getMetadataPrefix(), METS_NAMESPACE);
            newMets.addNamespaceDeclaration(XSI_NAMESPACE);
            newMets.addNamespaceDeclaration(XSI_NAMESPACE);
            newMets.addNamespaceDeclaration(MODS_NAMESPACE);
            newMets.addNamespaceDeclaration(DV_NAMESPACE);
            newMets.addNamespaceDeclaration(XLINK_NAMESPACE);
            newMets.setAttribute("schemaLocation",
                    "http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-3.xsd http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/version17/mets.v1-7.xsd",
                    XSI_NAMESPACE);

            newMets.addContent(xmlRoot.cloneContent());
            recordData.addContent(newMets);
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        } catch (HTTPException e) {
            logger.error("{}: {}", e.getCode(), e.getMessage());
        }
    }

    /**
     * 
     * @param document
     * @param recordData
     * @should add correct element types
     */
    static void generateSolrRecord(SolrDocument document, Element recordData) {
        Element doc = new Element("doc");
        recordData.addContent(doc);

        for (String fieldname : document.getFieldNames()) {
            Object fieldvalue = document.getFieldValue(fieldname);
            if (fieldvalue instanceof String s) {
                Element str = new Element("str");
                str.setAttribute("name", fieldname);
                str.setText(s);
                doc.addContent(str);
            } else if (fieldvalue instanceof Long l) {
                Element longElement = new Element("long");
                longElement.setAttribute("name", fieldname);
                longElement.setText(l.toString());
                doc.addContent(longElement);
            } else if (fieldvalue instanceof Integer i) {
                Element intElement = new Element("int");
                intElement.setAttribute("name", fieldname);
                intElement.setText(i.toString());
                doc.addContent(intElement);
            } else if (fieldvalue instanceof Boolean bool) {
                Element intElement = new Element("bool");
                intElement.setAttribute("name", fieldname);
                intElement.setText(bool.toString());
                doc.addContent(intElement);
            } else if (fieldvalue instanceof Collection col) {
                Element arr = new Element("arr");
                arr.setAttribute("name", fieldname);
                doc.addContent(arr);

                for (Object o : col) {
                    if (o instanceof String s) {
                        Element str = new Element("str");
                        str.setText(s);
                        arr.addContent(str);
                    } else if (o instanceof Long l) {
                        Element longElement = new Element("long");
                        longElement.setText(l.toString());
                        arr.addContent(longElement);
                    } else if (fieldvalue instanceof Integer i) {
                        Element intElement = new Element("int");
                        intElement.setText(i.toString());
                        doc.addContent(intElement);
                    } else if (fieldvalue instanceof Boolean bool) {
                        Element intElement = new Element("bool");
                        intElement.setText(bool.toString());
                        doc.addContent(intElement);
                    }
                }
            }
        }
    }

    /**
     * 
     * @param request
     * @param parameter
     * @return {@link Element}
     */
    private static Element getExplain(HttpServletRequest request, SruRequestParameter parameter) {
        Element root = new Element("explainResponse", SRU_NAMESPACE);

        Element version = new Element(SruRequestParameter.PARAM_VERSION, SRU_NAMESPACE);
        version.setText(parameter.getVersion());
        root.addContent(version);

        Element eleRecord = new Element("record", SRU_NAMESPACE);
        root.addContent(eleRecord);

        Element recordSchema = new Element("recordSchema", SRU_NAMESPACE);
        recordSchema.setText(EXPLAIN_NAMESPACE.getURI());
        eleRecord.addContent(recordSchema);

        Element recordData = new Element("recordData", SRU_NAMESPACE);
        eleRecord.addContent(recordData);

        Element explain = new Element("explain", EXPLAIN_NAMESPACE);
        explain.setAttribute("id", "intranda GmbH");

        recordData.addContent(explain);

        Element serverInfo = new Element("serverInfo", EXPLAIN_NAMESPACE);
        serverInfo.setAttribute("protocol", "SRU");
        serverInfo.setAttribute(SruRequestParameter.PARAM_VERSION, "1.2");
        serverInfo.setAttribute("transport", "http");

        explain.addContent(serverInfo);

        Element host = new Element("host", EXPLAIN_NAMESPACE);
        host.setText(request.getRequestURL().toString());
        serverInfo.addContent(host);

        Element port = new Element("port", EXPLAIN_NAMESPACE);
        port.setText(String.valueOf(request.getLocalPort()));
        serverInfo.addContent(port);

        Element database = new Element("database", EXPLAIN_NAMESPACE);
        database.setText("viewer/sru");
        serverInfo.addContent(database);

        Element databaseInfo = new Element("databaseInfo", EXPLAIN_NAMESPACE);
        explain.addContent(databaseInfo);

        Element title = new Element("title", EXPLAIN_NAMESPACE);
        title.setAttribute("primary", "true");
        title.setText("intranda viewer");
        databaseInfo.addContent(title);

        Element author = new Element("author", EXPLAIN_NAMESPACE);
        author.setText("intranda GmbH");
        databaseInfo.addContent(author);

        Element contact = new Element("contact", EXPLAIN_NAMESPACE);
        contact.setText(DataManager.getInstance().getConfiguration().getIdentifyTags().get("adminEmail"));
        databaseInfo.addContent(contact);

        Element implementation = new Element("implementation", EXPLAIN_NAMESPACE);
        implementation.setAttribute(SruRequestParameter.PARAM_VERSION, "1.2");
        databaseInfo.addContent(implementation);

        Element indexInfo = new Element("indexInfo", EXPLAIN_NAMESPACE);
        explain.addContent(indexInfo);

        Element cqlSet = new Element("set", EXPLAIN_NAMESPACE);
        cqlSet.setAttribute("name", "cql");
        cqlSet.setAttribute("identifier", "info:srw/cql-context-set/1/cql-v1.1");
        Element cqlTitle = new Element("title", EXPLAIN_NAMESPACE);
        cqlTitle.setText("CQL Standard Set");
        cqlSet.addContent(cqlTitle);

        indexInfo.addContent(cqlSet);

        Element dcSet = new Element("set", EXPLAIN_NAMESPACE);
        dcSet.setAttribute("name", "dc");
        dcSet.setAttribute("identifier", "info:srw/cql-context-set/1/dc-v1.1");
        Element dcTitle = new Element("title", EXPLAIN_NAMESPACE);
        dcTitle.setText("Dublin Core Set");
        dcSet.addContent(dcTitle);
        indexInfo.addContent(dcSet);

        Element intrandaSet = new Element("set", EXPLAIN_NAMESPACE);
        intrandaSet.setAttribute("name", "iv");
        intrandaSet.setAttribute("identifier", "http://intranda.com/iv-v1.2");
        Element intrandaTitle = new Element("title", EXPLAIN_NAMESPACE);
        intrandaTitle.setText("intranda viewer Set");
        intrandaSet.addContent(intrandaTitle);
        indexInfo.addContent(intrandaSet);

        for (SearchField field : SearchField.values()) {
            Element index = createIndex(field);
            indexInfo.addContent(index);
        }

        explain.addContent(
                createSchemaInfoElement("MODS", "mods", "info:srw/schema/1/mods-v3.3", "http://www.loc.gov/standards/mods/v3/mods-3-3.xsd"));
        explain.addContent(
                createSchemaInfoElement("Dublin Core", "dc", "info:srw/schema/1/dc-v1.1",
                        "http://www.loc.gov/standards/sru/resources/dc-schema.xsd"));
        explain.addContent(
                createSchemaInfoElement("MARC21-XML", "marcxml", "info:srw/schema/1/marcxml-v1.1",
                        "http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd"));
        explain.addContent(
                createSchemaInfoElement("SOLR", "solr", "info:srw/schema/1/solr-v1.1", ""));
        explain.addContent(
                createSchemaInfoElement("METS/MODS", "mets", "http://www.loc.gov/METS/", "http://www.loc.gov/standards/mets/version18/mets.xsd"));
        explain.addContent(
                createSchemaInfoElement("LIDO", "lido", "http://www.lido-schema.org", "http://www.lido-schema.org/schema/v1.0/lido-v1.0.xsd"));

        Element configInfo = new Element("configInfo", EXPLAIN_NAMESPACE);
        explain.addContent(configInfo);

        Element numOfRecords = new Element("default", EXPLAIN_NAMESPACE);
        numOfRecords.setAttribute("type", "numberOfRecords");
        numOfRecords.setText("100");
        configInfo.addContent(numOfRecords);

        Element retrieveSchema = new Element("default", EXPLAIN_NAMESPACE);
        retrieveSchema.setAttribute("type", "retrieveSchema");
        retrieveSchema.setText("mods");
        configInfo.addContent(retrieveSchema);

        Element operator = new Element("default", EXPLAIN_NAMESPACE);
        operator.setAttribute("type", "booleanOperator");
        operator.setText("and");
        configInfo.addContent(operator);

        configInfo.addContent(createSupportsElement("and"));
        configInfo.addContent(createSupportsElement("or"));
        configInfo.addContent(createSupportsElement("not"));
        configInfo.addContent(createSupportsElement("*"));
        configInfo.addContent(createSupportsElement("="));
        configInfo.addContent(createSupportsElement("&lt;"));
        configInfo.addContent(createSupportsElement("&gt;"));
        configInfo.addContent(createSupportsElement("exact"));
        configInfo.addContent(createSupportsElement("within"));

        return root;
    }

    /**
     * 
     * @param displayTitle
     * @param name
     * @param identifier
     * @param location
     * @return {@link Element}
     * @should create element correctly
     */
    static Element createSchemaInfoElement(String displayTitle, String name, String identifier, String location) {
        Element ret = new Element("schemaInfo", EXPLAIN_NAMESPACE);

        Element schema = new Element("schema", EXPLAIN_NAMESPACE);
        schema.setAttribute("retrieve", "true");
        schema.setAttribute("sort", "false");
        schema.setAttribute("identifier", identifier);
        schema.setAttribute("location", location);
        schema.setAttribute("name", name);

        Element schemaTitle = new Element("title", EXPLAIN_NAMESPACE);
        schemaTitle.setAttribute("primary", "true");
        schemaTitle.setText(displayTitle);
        schema.addContent(schemaTitle);
        ret.addContent(schema);

        return ret;
    }

    /**
     * 
     * @param value
     * @return {@link Element}
     * @should create element correctly
     */
    static Element createSupportsElement(String value) {
        Element ret = new Element("supports", EXPLAIN_NAMESPACE);
        ret.setAttribute("type", "relation");
        ret.setText(value);

        return ret;
    }

    /**
     * 
     * @param field
     * @return {@link Element}
     */
    private static Element createIndex(SearchField field) {
        Element index = new Element("index", EXPLAIN_NAMESPACE);
        if (field.isSeachable()) {
            index.setAttribute("search", "true");
        } else {
            index.setAttribute("search", "false");
        }

        if (field.isScanable()) {
            index.setAttribute("scan", "true");
        } else {
            index.setAttribute("scan", "false");
        }

        if (field.isSortable()) {
            index.setAttribute("sort", "true");
        } else {
            index.setAttribute("sort", "false");
        }

        index.setAttribute("id", field.getInternalName());
        Element titleElement = new Element("title", EXPLAIN_NAMESPACE);
        titleElement.setAttribute("primary", "true");
        titleElement.setText(field.toString());
        index.addContent(titleElement);

        Element map = new Element("map", EXPLAIN_NAMESPACE);
        index.addContent(map);
        if (field.getCqlName() != null) {
            Element namecql = new Element("name", EXPLAIN_NAMESPACE);
            namecql.setAttribute("set", "cql");
            namecql.setText(field.getCqlName());
            map.addContent(namecql);
        }
        if (field.getDcName() != null) {
            Element namedc = new Element("name", EXPLAIN_NAMESPACE);
            namedc.setAttribute("set", "dc");
            namedc.setText(field.getDcName());
            map.addContent(namedc);
        }
        if (field.getSolrName() != null) {
            Element nameiv = new Element("name", EXPLAIN_NAMESPACE);
            nameiv.setAttribute("set", "iv");
            nameiv.setText(field.getInternalName());
            map.addContent(nameiv);
        }
        return index;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    /** {@inheritDoc} */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
