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
package io.goobi.viewer.connector.oai.model.formats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;

import com.ctc.wstx.shaded.msv_core.verifier.ErrorInfo.ElementErrorInfo;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.Utils;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.solr.SolrConstants;

/**
 * LIDO
 */
public class LIDOFormat extends Format {

    private static final Logger logger = LogManager.getLogger(LIDOFormat.class);

    static final Namespace LIDO_NS =
            Namespace.getNamespace(Metadata.LIDO.getMetadataNamespacePrefix(), Metadata.LIDO.getMetadataNamespaceUri());

    private static final String LIDO_FILTER_QUERY = " +(+" + SolrConstants.SOURCEDOCFORMAT + ":LIDO " + "-" + SolrConstants.DATEDELETED + ":*)";

    private List<String> setSpecFields =
            DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.LIDO.getMetadataPrefix());

    /** {@inheritDoc} */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField,
            String filterQuerySuffix) throws IOException, SolrServerException {
        List<String> fieldList = new ArrayList<>(Arrays.asList(IDENTIFIER_FIELDS));
        fieldList.addAll(Arrays.asList(DATE_FIELDS));
        fieldList.addAll(setSpecFields);
        QueryResponse qr =
                solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false, LIDO_FILTER_QUERY, filterQuerySuffix,
                        fieldList, null);
        if (qr.getResults().isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }

        return generateLidoRecords(qr.getResults(), qr.getResults().getNumFound(), firstRawRow, numRows, handler, "ListRecords", setSpecFields,
                filterQuerySuffix);
    }

    /** {@inheritDoc} */
    @Override
    public Element createGetRecord(RequestHandler handler, String filterQuerySuffix) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        List<String> fieldList = new ArrayList<>(Arrays.asList(IDENTIFIER_FIELDS));
        fieldList.addAll(Arrays.asList(DATE_FIELDS));
        fieldList.addAll(setSpecFields);
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), fieldList, filterQuerySuffix);
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            return generateLidoRecords(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord", setSpecFields, filterQuerySuffix);
        } catch (IOException | SolrServerException e) {
            return new ErrorCode().getIdDoesNotExist();
        }
    }

    /**
     * Creates LIDO records
     * 
     * @param records
     * @param totalHits
     * @param firstRow
     * @param numRows
     * @param handler
     * @param recordType "GetRecord" or "ListRecords"
     * @param setSpecFields
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return {@link ElementErrorInfo}
     * @throws IOException
     * @throws JDOMException
     * @throws SolrServerException
     * @throws HTTPException
     */
    private static Element generateLidoRecords(List<SolrDocument> records, long totalHits, int firstRow, final int numRows, RequestHandler handler,
            String recordType, List<String> setSpecFields, String filterQuerySuffix) throws SolrServerException {
        Element xmlListRecords = new Element(recordType, OAI_NS);

        for (SolrDocument doc : records) {
            String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
            if (pi == null) {
                pi = (String) doc.getFieldValue(SolrConstants.PI);
            }
            if (pi == null) {
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                continue;
            }
            String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl()).append(pi).toString();
            String xml = null;
            try {
                xml = NetTools.getWebContentGET(url);
            } catch (HTTPException | IOException e) {
                logger.error("Could not retriee LIDO: {}", url);
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                continue;
            }

            if (StringUtils.isEmpty(xml)) {
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                continue;
            }

            Element eleRecord = generateLidoRecord(xml, doc, handler, setSpecFields, filterQuerySuffix);
            if (eleRecord != null) {
                xmlListRecords.addContent(eleRecord);
            } else {
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
            }
        }

        // Create resumption token
        int useNumRows = numRows;
        if (records.size() < useNumRows) {
            useNumRows = records.size();
        }
        if (totalHits > firstRow + useNumRows) {
            Element resumption = createResumptionTokenAndElement(totalHits, firstRow + useNumRows, firstRow, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /**
     * 
     * @param xml
     * @param doc
     * @param handler
     * @param setSpecFields
     * @param filterQuerySuffix
     * @return {@link ElementErrorInfo}
     * @throws SolrServerException
     * @should generate element correctly
     * @should return null if xml empty
     */
    static Element generateLidoRecord(String xml, SolrDocument doc, RequestHandler handler, List<String> setSpecFields, String filterQuerySuffix)
            throws SolrServerException {
        logger.trace("generateLidoRecord");
        if (StringUtils.isEmpty(xml)) {
            return null;
        }

        try {
            org.jdom2.Document xmlDoc = XmlTools.getDocumentFromString(xml, null);
            Element xmlRoot = xmlDoc.getRootElement();
            Element newLido = new Element(Metadata.LIDO.getMetadataPrefix(), LIDO_NS);
            newLido.addNamespaceDeclaration(XSI_NS);
            newLido.setAttribute(
                    new Attribute("schemaLocation", "http://www.lido-schema.org http://www.lido-schema.org/schema/v1.0/lido-v1.0.xsd", XSI_NS));
            newLido.addContent(xmlRoot.cloneContent());

            Element eleRecord = new Element("record", OAI_NS);
            Element header = getHeader(doc, null, handler, null, setSpecFields, filterQuerySuffix);
            eleRecord.addContent(header);
            Element metadata = new Element("metadata", OAI_NS);
            metadata.addContent(newLido);
            eleRecord.addContent(metadata);
            return eleRecord;
        } catch (IOException | JDOMException e) {
            logger.error("{}:\n{}", e.getMessage(), xml);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getTotalHits(Map<String, String> params, String versionDiscriminatorField, String filterQuerySuffix)
            throws IOException, SolrServerException {
        return solr.getTotalHitNumber(params, false, LIDO_FILTER_QUERY, null, filterQuerySuffix);
    }

}
