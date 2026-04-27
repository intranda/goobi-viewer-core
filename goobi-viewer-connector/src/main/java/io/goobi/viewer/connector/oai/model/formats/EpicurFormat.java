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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Element;
import org.jdom2.Namespace;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.enums.Verb;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.SolrSearchIndex;
import io.goobi.viewer.connector.utils.SolrSearchTools;
import io.goobi.viewer.connector.utils.Utils;
import io.goobi.viewer.connector.utils.XmlConstants;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Xepicur
 */
public class EpicurFormat extends Format {

    private static final Logger logger = LogManager.getLogger(EpicurFormat.class);

    private static final Namespace EPICUR =
            Namespace.getNamespace(Metadata.EPICUR.getMetadataNamespacePrefix(), Metadata.EPICUR.getMetadataNamespaceUri());

    private static final String[] FIELDS =
            { SolrConstants.DATECREATED, SolrConstants.DATEUPDATED, SolrConstants.DATEDELETED, SolrConstants.PI, SolrConstants.PI_TOPSTRUCT,
                    SolrConstants.URN };

    private static final String STATUS_URL_UPDATE_GENERAL = "url_update_general";
    private static final String STATUS_URN_NEW = "urn_new";

    private List<String> setSpecFields =
            DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.EPICUR.getMetadataPrefix());

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, final int numRows,
            String versionDiscriminatorField,
            String filterQuerySuffix) throws SolrServerException, IOException {
        logger.trace("createListRecords");

        String urnPrefixBlacklistSuffix =
                SolrSearchTools.getUrnPrefixBlacklistSuffix(DataManager.getInstance().getConfiguration().getUrnPrefixBlacklist());
        String additionalQuery = urnPrefixBlacklistSuffix
                + SolrSearchTools.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes());
        List<String> fieldList = new ArrayList<>(Arrays.asList(FIELDS));
        fieldList.addAll(setSpecFields);

        int useNumRows = numRows;
        QueryResponse qr =
                solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, useNumRows, true, additionalQuery, filterQuerySuffix,
                        fieldList, null);
        SolrDocumentList records = qr.getResults();
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        Element xmlListRecords = new Element("ListRecords", OAI_NS);
        if (records.size() < useNumRows) {
            useNumRows = records.size();
        }
        int pagecount = 0;
        for (SolrDocument doc : records) {
            long dateUpdated = SolrSearchTools.getLatestValidDateUpdated(doc, RequestHandler.getUntilTimestamp(handler.getUntil()));
            Long dateDeleted = (Long) doc.getFieldValue(SolrConstants.DATEDELETED);
            if (doc.getFieldValue(SolrConstants.URN) != null) {
                Element eleRecord = new Element(XmlConstants.ELE_NAME_RECORD, OAI_NS);
                Element header = generateEpicurHeader(doc, dateUpdated, setSpecFields);
                eleRecord.addContent(header);
                Element metadata = new Element(XmlConstants.ELE_NAME_METADATA, OAI_NS);
                eleRecord.addContent(metadata);
                boolean topstruct = doc.containsKey(SolrConstants.PI);
                metadata.addContent(generateEpicurElement((String) doc.getFieldValue(SolrConstants.URN),
                        (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateUpdated, dateDeleted, topstruct));
                xmlListRecords.addContent(eleRecord);
            }

            if (dateDeleted == null) {
                // Page elements for existing record
                StringBuilder sbPageQuery = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':')
                        .append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                        .append(" AND ")
                        .append(SolrConstants.DOCTYPE)
                        .append(":PAGE")
                        .append(" AND ")
                        .append(SolrConstants.IMAGEURN)
                        .append(":*");
                sbPageQuery.append(urnPrefixBlacklistSuffix);
                QueryResponse qrInner = solr.search(sbPageQuery.toString(), 0, SolrSearchIndex.MAX_HITS,
                        Collections.singletonList(SolrConstants.ORDER), Collections.singletonList(SolrConstants.IMAGEURN), null);
                if (qrInner != null && !qrInner.getResults().isEmpty()) {
                    for (SolrDocument pageDoc : qrInner.getResults()) {
                        String imgUrn = (String) pageDoc.getFieldValue(SolrConstants.IMAGEURN);
                        Element pagerecord = new Element(XmlConstants.ELE_NAME_RECORD, OAI_NS);
                        Element pageheader = generateEpicurPageHeader(doc, imgUrn, dateUpdated, setSpecFields);
                        pagerecord.addContent(pageheader);
                        Element pagemetadata = new Element(XmlConstants.ELE_NAME_METADATA, OAI_NS);
                        pagerecord.addContent(pagemetadata);
                        pagemetadata.addContent(generateEpicurPageElement(imgUrn, (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateUpdated,
                                (Long) doc.getFieldValue(SolrConstants.DATEDELETED)));
                        xmlListRecords.addContent(pagerecord);
                        pagecount++;
                    }
                    logger.trace("Found {} page records for {}", qrInner.getResults().size(), doc.getFieldValue(SolrConstants.PI_TOPSTRUCT));
                }
            } else {
                // Page elements for deleted record (only deleted record docs will have IMAGEURN_OAI!)
                Collection<Object> pageUrnValues = doc.getFieldValues(SolrConstants.IMAGEURN_OAI);
                if (pageUrnValues != null) {
                    for (Object obj : pageUrnValues) {
                        String imgUrn = (String) obj;
                        Element pagerecord = new Element(XmlConstants.ELE_NAME_RECORD, OAI_NS);
                        Element pageheader = generateEpicurPageHeader(doc, imgUrn, dateUpdated, setSpecFields);
                        pagerecord.addContent(pageheader);
                        Element pagemetadata = new Element(XmlConstants.ELE_NAME_METADATA, OAI_NS);
                        pagerecord.addContent(pagemetadata);
                        pagemetadata.addContent(
                                generateEpicurPageElement(imgUrn, (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateUpdated, dateDeleted));
                        xmlListRecords.addContent(pagerecord);
                        pagecount++;
                    }
                }
            }
        }
        logger.debug("Found {} page records total", pagecount);

        // Create resumption token
        if (records.getNumFound() > firstRawRow + useNumRows) {
            Element resumption = createResumptionTokenAndElement(records.getNumFound(), firstRawRow + useNumRows, firstRawRow, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /** {@inheritDoc} */
    @Override
    public Element createGetRecord(RequestHandler handler, String filterQuerySuffix) {
        logger.trace("createGetRecord");
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        List<String> fieldList = new ArrayList<>(Arrays.asList(FIELDS));
        fieldList.addAll(setSpecFields);
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), fieldList, filterQuerySuffix);
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            Element getRecord = new Element("GetRecord", OAI_NS);
            long dateupdated = SolrSearchTools.getLatestValidDateUpdated(doc, RequestHandler.getUntilTimestamp(handler.getUntil()));
            String urn = doc.getFieldValue(SolrConstants.URN) != null ? (String) doc.getFieldValue(SolrConstants.URN) : handler.getIdentifier();
            Element header = generateEpicurPageHeader(doc, urn, dateupdated, setSpecFields);
            Element eleRecord = new Element(XmlConstants.ELE_NAME_RECORD, OAI_NS);
            eleRecord.addContent(header);
            Element metadata = new Element(XmlConstants.ELE_NAME_METADATA, OAI_NS);
            eleRecord.addContent(metadata);
            metadata.addContent(generateEpicurPageElement(urn, (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateupdated,
                    (Long) doc.getFieldValue(SolrConstants.DATEDELETED)));
            getRecord.addContent(eleRecord);

            return getRecord;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (SolrServerException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        }
    }

    /**
     * generates header for epicur format
     * 
     * @param doc
     * @param dateUpdated
     * @param setSpecFields
     * @return {@link Element}
     */
    private static Element generateEpicurHeader(SolrDocument doc, long dateUpdated, List<String> setSpecFields) {
        Element header = new Element("header", OAI_NS);
        Element identifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, OAI_NS);
        identifier.setText(
                DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier")
                        + (String) doc.getFieldValue(SolrConstants.URN));
        header.addContent(identifier);

        Element datestamp = new Element("datestamp", OAI_NS);
        datestamp.setText(Utils.parseDate(dateUpdated));
        header.addContent(datestamp);
        // setSpec
        if (!setSpecFields.isEmpty()) {
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
        }

        return header;
    }

    /**
     * 
     * @param urn
     * @param dateCreated
     * @param dateUpdated
     * @param dateDeleted
     * @param topstruct
     * @return {@link Element}
     */
    private static Element generateEpicurElement(String urn, Long dateCreated, Long dateUpdated, Long dateDeleted, boolean topstruct) {
        Namespace xmlns = Namespace.getNamespace("urn:nbn:de:1111-2004033116");

        Element epicur = new Element("epicur", xmlns);
        epicur.addNamespaceDeclaration(XSI_NS);
        epicur.addNamespaceDeclaration(EPICUR);
        epicur.setAttribute("schemaLocation", "urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd",
                XSI_NS);

        // xsi:schemaLocation="urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd"
        // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        // xmlns:epicur="urn:nbn:de:1111-2004033116"
        // xmlns="urn:nbn:de:1111-2004033116"

        String status;
        if (dateDeleted != null) {
            // "url_delete" is no longer allowed
            status = STATUS_URL_UPDATE_GENERAL;
        } else {
            if (dateCreated != null && dateUpdated != null) {
                if (dateUpdated > dateCreated) {
                    status = STATUS_URL_UPDATE_GENERAL;
                } else {
                    status = STATUS_URN_NEW;
                }
            } else {
                status = STATUS_URN_NEW;
            }
        }

        epicur.addContent(generateAdministrativeData(status, xmlns));

        Element eleRecord = new Element(XmlConstants.ELE_NAME_RECORD, xmlns);
        Element schemaIdentifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, xmlns);
        schemaIdentifier.setAttribute(XmlConstants.ATT_NAME_SCHEME, "urn:nbn:de");
        schemaIdentifier.setText(urn);
        eleRecord.addContent(schemaIdentifier);

        Element resource = new Element("resource", xmlns);
        // add no resource element if the record is deleted
        if (dateDeleted == null) {
            eleRecord.addContent(resource);
        }

        Element identifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, xmlns);
        identifier.setAttribute("origin", "original");
        identifier.setAttribute("role", "primary");
        identifier.setAttribute(XmlConstants.ATT_NAME_SCHEME, "url");
        if (topstruct) {
            identifier.setAttribute("type", "frontpage");
        }

        identifier.setText(DataManager.getInstance().getConfiguration().getUrnResolverUrl() + urn);
        resource.addContent(identifier);
        Element format = new Element("format", xmlns);
        format.setAttribute(XmlConstants.ATT_NAME_SCHEME, "imt");
        format.setText("text/html");
        resource.addContent(format);
        epicur.addContent(eleRecord);

        return epicur;
    }

    /**
     * 
     * @param doc
     * @param urn
     * @param dateUpdated
     * @param setSpecFields
     * @return {@link Element}
     */
    private static Element generateEpicurPageHeader(SolrDocument doc, String urn, long dateUpdated, List<String> setSpecFields) {
        Element header = new Element("header", OAI_NS);

        Element identifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, OAI_NS);
        identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier") + urn);
        header.addContent(identifier);

        Element datestamp = new Element("datestamp", OAI_NS);
        datestamp.setText(Utils.parseDate(dateUpdated));
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
        }

        return header;
    }

    /**
     * 
     * @param urn
     * @param dateCreated
     * @param dateUpdated
     * @param dateDeleted
     * @return {@link Element}
     */
    private static Element generateEpicurPageElement(String urn, Long dateCreated, Long dateUpdated, Long dateDeleted) {
        Namespace xmlns = Namespace.getNamespace("urn:nbn:de:1111-2004033116");

        Element epicur = new Element("epicur", xmlns);
        epicur.addNamespaceDeclaration(XSI_NS);
        epicur.addNamespaceDeclaration(EPICUR);
        epicur.setAttribute("schemaLocation", "urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd",
                XSI_NS);
        String status = STATUS_URN_NEW;

        // xsi:schemaLocation="urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd"
        // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        // xmlns:epicur="urn:nbn:de:1111-2004033116"
        // xmlns="urn:nbn:de:1111-2004033116"

        // /*
        // TODO add this after dnb can handle status updates

        if (dateDeleted != null) {
            status = "url_delete";
        } else if (dateCreated != null && dateUpdated != null) {
            if (dateUpdated > dateCreated) {
                status = STATUS_URL_UPDATE_GENERAL;
            } else {
                status = STATUS_URN_NEW;
            }
        }

        // */

        epicur.addContent(generateAdministrativeData(status, xmlns));

        Element eleRecord = new Element(XmlConstants.ELE_NAME_RECORD, xmlns);

        Element schemaIdentifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, xmlns);
        schemaIdentifier.setAttribute(XmlConstants.ATT_NAME_SCHEME, "urn:nbn:de");
        schemaIdentifier.setText(urn);
        eleRecord.addContent(schemaIdentifier);

        Element resource = new Element("resource", xmlns);
        eleRecord.addContent(resource);

        Element identifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, xmlns);
        identifier.setAttribute("origin", "original");
        identifier.setAttribute("role", "primary");
        identifier.setAttribute(XmlConstants.ATT_NAME_SCHEME, "url");
        //        identifier.setAttribute("type", "frontpage");

        identifier.setText(DataManager.getInstance().getConfiguration().getUrnResolverUrl() + urn);
        resource.addContent(identifier);
        Element format = new Element("format", xmlns);
        format.setAttribute(XmlConstants.ATT_NAME_SCHEME, "imt");
        format.setText("text/html");
        resource.addContent(format);
        epicur.addContent(eleRecord);

        return epicur;
    }

    /**
     * Generates administrative_data section in epicur.
     * 
     * @param status
     * @param xmlns
     * @return {@link Element}
     */
    private static Element generateAdministrativeData(String status, Namespace xmlns) {
        Element delivery = new Element("delivery", xmlns);

        Element eleUpdateStatus = new Element("update_status", xmlns);
        eleUpdateStatus.setAttribute("type", status);
        delivery.addContent(eleUpdateStatus);

        Element transfer = new Element("transfer", xmlns);
        transfer.setAttribute("type", "oai");
        delivery.addContent(transfer);

        Element eleAdministrativeData = new Element("administrative_data", xmlns);
        eleAdministrativeData.addContent(delivery);
        return eleAdministrativeData;
    }

    /** {@inheritDoc} */
    @Override
    public long getTotalHits(Map<String, String> params, String versionDiscriminatorField, String filterQuerySuffix)
            throws IOException, SolrServerException {
        // Hit count may differ for epicur
        String additionalQuery = SolrSearchTools.getUrnPrefixBlacklistSuffix(DataManager.getInstance().getConfiguration().getUrnPrefixBlacklist());
        if (!Verb.LISTIDENTIFIERS.getTitle().equals(params.get("verb"))) {
            additionalQuery +=
                    SolrSearchTools.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes());
        }
        // Query Solr index for the total hits number
        return solr.getTotalHitNumber(params, true, additionalQuery, null, filterQuerySuffix);
    }

}
