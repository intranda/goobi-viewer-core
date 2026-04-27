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

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.SolrSearchTools;
import io.goobi.viewer.connector.utils.Utils;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Format for TEI and CMDI records.
 */
public class TEIFormat extends Format {

    private static final Logger logger = LogManager.getLogger(TEIFormat.class);

    static final Namespace CMDI = Namespace.getNamespace("cmd", "http://www.clarin.eu/cmd/1");
    static final Namespace COMPONENTS = Namespace.getNamespace("cmdp", "http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1380106710826");

    /** {@inheritDoc} */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField,
            String filterQuerySuffix) throws SolrServerException {
        // &stats=true&stats.field=LANGUAGE
        QueryResponse qr;
        long totalVirtualHits;
        long totalRawHits;
        List<String> fieldList = new ArrayList<>(Arrays.asList(IDENTIFIER_FIELDS));
        fieldList.addAll(Arrays.asList(DATE_FIELDS));

        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            // One OAI record for each record version
            qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false,
                    " AND " + versionDiscriminatorField + ":*", filterQuerySuffix, fieldList,
                    Collections.singletonList(versionDiscriminatorField));
            totalVirtualHits = SolrSearchTools.getFieldCount(qr, versionDiscriminatorField);
            totalRawHits = qr.getResults().getNumFound();
        } else {
            // One OAI record for each record proper
            qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false, null, filterQuerySuffix, fieldList,
                    null);
            totalRawHits = qr.getResults().getNumFound();
            totalVirtualHits = totalRawHits;
        }
        if (qr.getResults().isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        try {
            return generateTeiCmdi(qr.getResults(), totalVirtualHits, totalRawHits, firstVirtualRow, firstRawRow, numRows, handler,
                    "ListRecords", versionDiscriminatorField, null, filterQuerySuffix);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        } catch (HTTPException e) {
            return new ErrorCode().getIdDoesNotExist();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Element createGetRecord(RequestHandler handler, String filterQuerySuffix) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }

        String versionDiscriminatorField =
                DataManager.getInstance()
                        .getConfiguration()
                        .getVersionDisriminatorFieldForMetadataFormat(handler.getMetadataPrefix().getMetadataPrefix());
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            String[] identifierSplit = Utils.splitIdentifierAndLanguageCode(handler.getIdentifier(), 3);
            try {
                SolrDocument doc = solr.getListRecord(identifierSplit[0], null, filterQuerySuffix);
                if (doc == null) {
                    return new ErrorCode().getIdDoesNotExist();
                }
                return generateTeiCmdi(Collections.singletonList(doc), 1L, 1L, 0, 0, 1, handler, "GetRecord", versionDiscriminatorField,
                        identifierSplit[1], filterQuerySuffix);
            } catch (HTTPException | IOException | SolrServerException e) {
                return new ErrorCode().getIdDoesNotExist();
            } catch (JDOMException e) {
                return new ErrorCode().getCannotDisseminateFormat();
            }
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), null, filterQuerySuffix);
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            return generateTeiCmdi(Collections.singletonList(doc), 1L, 1L, 0, 0, 1, handler, "GetRecord",
                    DataManager.getInstance()
                            .getConfiguration()
                            .getVersionDisriminatorFieldForMetadataFormat(handler.getMetadataPrefix().getMetadataPrefix()),
                    null, filterQuerySuffix);
        } catch (HTTPException | IOException | SolrServerException e) {
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        }
    }

    /**
     * Creates TEI or CMDI records.
     * 
     * @param records
     * @param totalVirtualHits
     * @param totalRawHits
     * @param firstVirtualRow
     * @param firstRawRow
     * @param numRows
     * @param handler
     * @param recordType "GetRecord" or "ListRecords"
     * @param versionDiscriminatorField If not null, each value of this field will be created as an individual record
     * @param requestedVersion If not null, only the record with the exact value will be added
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return {@link ElementErrorInfo}
     * @throws IOException
     * @throws JDOMException
     * @throws SolrServerException
     * @throws HTTPException
     */
    private static Element generateTeiCmdi(List<SolrDocument> records, long totalVirtualHits, long totalRawHits, int firstVirtualRow, int firstRawRow,
            final int numRows, RequestHandler handler, String recordType, String versionDiscriminatorField, String requestedVersion,
            String filterQuerySuffix) throws JDOMException, IOException, SolrServerException, HTTPException {
        Element xmlListRecords = new Element(recordType, OAI_NS);

        Namespace namespace = Namespace.getNamespace(handler.getMetadataPrefix().getMetadataNamespacePrefix(),
                handler.getMetadataPrefix().getMetadataNamespaceUri());

        List<String> setSpecFields =
                DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(handler.getMetadataPrefix().getMetadataPrefix());
        int virtualHitCount = 0;
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            List<String> versions = Collections.singletonList(requestedVersion);
            for (SolrDocument doc : records) {
                if (requestedVersion == null) {
                    versions = SolrSearchTools.getMetadataValues(doc, versionDiscriminatorField);
                }
                for (String version : versions) {
                    virtualHitCount++; // Count hit even if the XML file is ultimately unavailable
                    String url = new StringBuilder(DataManager.getInstance().getConfiguration().getRestApiUrl())
                            .append("records/")
                            .append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                            .append('/')
                            .append(handler.getMetadataPrefix().getMetadataPrefix())
                            .append('/')
                            .append(version)
                            .append('/')
                            .toString();
                    logger.trace("api url: {}", url);
                    String xml = NetTools.getWebContentGET(url);
                    if (StringUtils.isEmpty(xml)) {
                        // Old API fallback
                        url = new StringBuilder(DataManager.getInstance().getConfiguration().getRestApiUrl())
                                .append("content/")
                                .append(handler.getMetadataPrefix().getMetadataPrefix())
                                .append('/')
                                .append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                                .append('/')
                                .append(version)
                                .append('/')
                                .toString();
                        logger.trace("old url: {}", url);
                        xml = NetTools.getWebContentGET(url);
                    }
                    if (StringUtils.isEmpty(xml)) {
                        xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                        continue;
                    }

                    org.jdom2.Document xmlDoc = XmlTools.getDocumentFromString(xml, null);
                    Element teiRoot = xmlDoc.getRootElement();
                    Element newDoc;
                    switch (handler.getMetadataPrefix()) {
                        case TEI:
                            newDoc = new Element("tei", namespace);
                            newDoc.addNamespaceDeclaration(XSI_NS);
                            newDoc.setAttribute(new Attribute("schemaLocation", handler.getMetadataPrefix().getSchema(), XSI_NS));
                            break;
                        case CMDI:
                            newDoc = new Element("CMD", CMDI);
                            newDoc.addNamespaceDeclaration(XSI_NS);
                            newDoc.addNamespaceDeclaration(COMPONENTS);
                            newDoc.setAttribute("CMDVersion", "1.2");
                            newDoc.setAttribute(new Attribute("schemaLocation",
                                    "http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1380106710826 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1380106710826/xsd",
                                    XSI_NS));
                            break;
                        default:
                            xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                            continue;
                    }

                    newDoc.addContent(teiRoot.cloneContent());

                    String iso3code = version;
                    // Make sure to add the ISO-3 language code
                    if (iso3code.length() == 2) {
                        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(version);
                        if (lang != null) {
                            iso3code = lang.getIsoCode();
                        }
                    }
                    Element rec = new Element("record", OAI_NS);
                    Element header = getHeader(doc, null, handler, iso3code, setSpecFields, filterQuerySuffix);
                    rec.addContent(header);
                    Element metadata = new Element("metadata", OAI_NS);
                    metadata.addContent(newDoc);
                    rec.addContent(metadata);
                    xmlListRecords.addContent(rec);
                }
            }
        } else {
            logger.error("TEI/CMDI record output without languages is currently not supported.");
            return new ErrorCode().getCannotDisseminateFormat();
        }

        // Create resumption token
        int useNumRows = numRows;
        if (records.size() < useNumRows) {
            useNumRows = records.size();
        }
        if (totalRawHits > firstRawRow + useNumRows) {
            Element resumption = createResumptionTokenAndElement(totalVirtualHits, totalRawHits, firstVirtualRow + virtualHitCount,
                    firstRawRow + useNumRows, firstVirtualRow, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /**
     * Modified header generation where identifiers also contain the language code.
     * 
     * @param doc
     * @param topstructDoc
     * @param handler
     * @param requestedVersion
     * @param setSpecFields
     * @param filterQuerySuffix
     * @return {@link Element}
     */
    protected static Element getHeader(SolrDocument doc, SolrDocument topstructDoc, RequestHandler handler, String requestedVersion,
            List<String> setSpecFields, String filterQuerySuffix)
            throws SolrServerException, IOException {
        Element header = new Element("header", OAI_NS);
        // identifier
        Element identifier = new Element("identifier", OAI_NS);
        identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier")
                + (String) doc.getFieldValue(SolrConstants.PI) + '_' + requestedVersion);
        header.addContent(identifier);
        // datestamp
        Element datestamp = new Element("datestamp", OAI_NS);
        long untilTimestamp = RequestHandler.getUntilTimestamp(handler.getUntil());
        long timestampModified = SolrSearchTools.getLatestValidDateUpdated(topstructDoc != null ? topstructDoc : doc, untilTimestamp);
        datestamp.setText(Utils.parseDate(timestampModified));
        if (StringUtils.isEmpty(datestamp.getText()) && doc.getFieldValue(SolrConstants.ISANCHOR) != null) {
            datestamp.setText(
                    Utils.parseDate(DataManager.getInstance().getSearchIndex().getLatestVolumeTimestamp(doc, untilTimestamp, filterQuerySuffix)));
        }
        header.addContent(datestamp);
        // setSpec
        if (setSpecFields != null && !setSpecFields.isEmpty()) {
            for (String setSpecField : setSpecFields) {
                if (doc.containsKey(setSpecField)) {
                    for (Object fieldValue : doc.getFieldValues(setSpecField)) {
                        // TODO translation
                        Element setSpec = new Element("setSpec", OAI_NS);
                        setSpec.setText((String) fieldValue);
                        header.addContent(setSpec);
                    }
                }
            }
        }

        // status="deleted"
        if (doc.getFieldValues(SolrConstants.DATEDELETED) != null) {
            header.setAttribute("status", "deleted");
            datestamp.setText(Utils.parseDate(doc.getFieldValue(SolrConstants.DATEDELETED)));
        }

        return header;
    }

    /** {@inheritDoc} */
    @Override
    public long getTotalHits(Map<String, String> params, String versionDiscriminatorField, String filterQuerySuffix)
            throws IOException, SolrServerException {
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            // Query Solr index for the count of the discriminator field
            QueryResponse qr = solr.search(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), 0, 0, false,
                    " AND " + versionDiscriminatorField + ":*", filterQuerySuffix, null, Collections.singletonList(versionDiscriminatorField));
            return SolrSearchTools.getFieldCount(qr, versionDiscriminatorField);
        }

        return solr.getTotalHitNumber(params, false, null, null, filterQuerySuffix);
    }
}
