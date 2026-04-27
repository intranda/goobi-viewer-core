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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.ctc.wstx.shaded.msv_core.verifier.ErrorInfo.ElementErrorInfo;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.SolrSearchTools;
import io.goobi.viewer.connector.utils.Utils;
import io.goobi.viewer.connector.utils.XmlConstants;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.solr.SolrConstants;

/**
 * ESE
 */
public class EuropeanaFormat extends OAIDCFormat {

    private static final Logger logger = LogManager.getLogger(EuropeanaFormat.class);

    private static Namespace nsDc = Namespace.getNamespace(Metadata.DC.getMetadataNamespacePrefix(), Metadata.DC.getMetadataNamespaceUri());
    private static Namespace nsDcTerms = Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
    private static Namespace nsEuropeana = Namespace.getNamespace(Metadata.ESE.getMetadataNamespacePrefix(), Metadata.ESE.getMetadataNamespaceUri());

    private List<String> setSpecFields =
            DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.ESE.name().toLowerCase());

    /** {@inheritDoc} */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField,
            String filterQuerySuffix) throws SolrServerException, IOException {
        QueryResponse qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false,
                SolrSearchTools.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes()),
                filterQuerySuffix, null, null);
        if (qr.getResults().isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        return generateESE(qr.getResults(), qr.getResults().getNumFound(), firstRawRow, numRows, handler, "ListRecords", filterQuerySuffix);
    }

    /** {@inheritDoc} */
    @Override
    public Element createGetRecord(RequestHandler handler, String filterQuerySuffix) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), null, filterQuerySuffix);
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            return generateESE(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord", filterQuerySuffix);
        } catch (IOException | SolrServerException e) {
            return new ErrorCode().getNoMetadataFormats();
        }
    }

    /**
     * Mandatory elements: europeana:provider europeana:dataProvider europeana:rights europeana:type europeana:isShownBy and/or europeana:isShownAt
     * 
     * @param records
     * @param totalHits
     * @param firstRow
     * @param numRows
     * @param handler
     * @param recordType
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return {@link ElementErrorInfo}
     * @throws SolrServerException
     * @throws IOException
     */
    private Element generateESE(List<SolrDocument> records, long totalHits, int firstRow, final int numRows, RequestHandler handler,
            String recordType, String filterQuerySuffix) throws SolrServerException, IOException {

        Element xmlListRecords = new Element(recordType, OAI_NS);
        for (SolrDocument doc : records) {
            Element eleRecord = generateSingleESERecord(doc, handler, filterQuerySuffix);
            xmlListRecords.addContent(eleRecord);
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
     * @param doc
     * @param handler
     * @param filterQuerySuffix
     * @return {@link Element}
     * @throws SolrServerException
     * @throws IOException
     * @should generate element correctly
     */
    Element generateSingleESERecord(SolrDocument doc, RequestHandler handler, String filterQuerySuffix) throws SolrServerException, IOException {
        // logger.trace("record: {}", doc.getFieldValue(SolrConstants.PI));
        boolean isWork = doc.getFieldValue(SolrConstants.ISWORK) != null && (boolean) doc.getFieldValue(SolrConstants.ISWORK);
        boolean isAnchor = doc.getFieldValue(SolrConstants.ISANCHOR) != null && (boolean) doc.getFieldValue(SolrConstants.ISANCHOR);
        SolrDocument topstructDoc = null;
        if (isWork || isAnchor) {
            topstructDoc = doc;
        } else {
            // If child element metadata fields are empty, get certain values from topstruct
            String iddocTopstruct = (String) doc.getFieldValue(SolrConstants.IDDOC_TOPSTRUCT);
            SolrDocumentList docList = solr.search("+" + SolrConstants.IDDOC + ":" + iddocTopstruct, filterQuerySuffix);
            if (docList != null && !docList.isEmpty()) {
                topstructDoc = docList.get(0);
            }
        }
        SolrDocument anchorDoc = null;
        if (!isAnchor) {
            SolrDocument childDoc = topstructDoc != null ? topstructDoc : doc;
            String iddocAnchor = (String) childDoc.getFieldValue(SolrConstants.IDDOC_PARENT);
            if (iddocAnchor != null) {
                SolrDocumentList docList = solr.search("+" + SolrConstants.IDDOC + ":" + iddocAnchor, filterQuerySuffix);
                if (docList != null && !docList.isEmpty()) {
                    anchorDoc = docList.get(0);
                }
            }
        }

        Element eleRecord = new Element("record", OAI_NS);
        Element header = getHeader(doc, topstructDoc, handler, null, setSpecFields, filterQuerySuffix);
        eleRecord.addContent(header);

        String identifier = null;
        String urn = null;
        String type = null;

        // create the metadata element, special for dc
        Element eleMetadata = new Element("metadata", OAI_NS);
        eleMetadata.addNamespaceDeclaration(nsEuropeana);
        eleMetadata.addNamespaceDeclaration(nsDc);
        eleMetadata.addNamespaceDeclaration(nsDcTerms);

        Element eleEuropeanaRecord = new Element("record", nsEuropeana);

        // <dc:identifier>
        if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.URN))) {
            Element eleDcIdentifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, nsDc);
            urn = (String) doc.getFieldValue(SolrConstants.URN);
            eleDcIdentifier.setText(DataManager.getInstance().getConfiguration().getUrnResolverUrl() + urn);
            eleEuropeanaRecord.addContent(eleDcIdentifier);
        } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI))) {
            Element eleDcIdentifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, nsDc);
            identifier = (String) doc.getFieldValue(SolrConstants.PI);
            eleDcIdentifier.setText(DataManager.getInstance().getConfiguration().getPiResolverUrl() + identifier);
            eleEuropeanaRecord.addContent(eleDcIdentifier);
        } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))) {
            Element eleDcIdentifier = new Element(XmlConstants.ELE_NAME_IDENTIFIER, nsDc);
            identifier = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
            eleDcIdentifier.setText(DataManager.getInstance().getConfiguration().getPiResolverUrl() + identifier);
            eleEuropeanaRecord.addContent(eleDcIdentifier);
        }
        // <dc:language>
        Element eleDcLanguage = new Element("language", nsDc);
        String language = "";
        if (doc.getFieldValues("MD_LANGUAGE") != null) {
            language = (String) doc.getFieldValues("MD_LANGUAGE").iterator().next();
            if (!StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(language)) {
                eleDcLanguage.setText(language);
                eleEuropeanaRecord.addContent(eleDcLanguage);
            }
        }

        // MANDATORY: <dc:title>
        String title = null;
        if (doc.getFieldValues(SolrConstants.TITLE) != null) {
            title = (String) doc.getFieldValues(SolrConstants.TITLE).iterator().next();
        }
        if (isWork && doc.getFieldValue(SolrConstants.IDDOC_PARENT) != null) {
            // If this is a volume, add anchor title in front
            String iddocParent = (String) doc.getFieldValue(SolrConstants.IDDOC_PARENT);
            String anchorTitle = anchorTitles.get(iddocParent);
            if (anchorTitle == null) {
                anchorTitle = getAnchorTitle(iddocParent, filterQuerySuffix);
                if (anchorTitle != null) {
                    title = anchorTitle + "; " + title;
                    anchorTitles.put(iddocParent, anchorTitle);
                }
            }
        }
        if (!StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(title)) {
            Element eleDcTitle = new Element("title", nsDc);
            eleDcTitle.setText(title);
            eleEuropeanaRecord.addContent(eleDcTitle);
        }

        // <dc:description>
        String desc = null;
        if (doc.getFieldValues("MD_INFORMATION") != null) {
            desc = (String) doc.getFieldValues("MD_INFORMATION").iterator().next();

        } else if (doc.getFieldValues(MD_DATECREATED) != null) {
            desc = (String) doc.getFieldValues(MD_DATECREATED).iterator().next();
        }
        if (desc != null && !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(desc)) {
            Element eleDcDescription = new Element("description", nsDc);
            eleDcDescription.setText(desc);
            eleEuropeanaRecord.addContent(eleDcDescription);
        }

        // <dc:date>
        String date = null;
        if (doc.getFieldValues(MD_YEARPUBLISH) != null) {
            date = (String) doc.getFieldValues(MD_YEARPUBLISH).iterator().next();
        } else if (doc.getFieldValues(MD_DATECREATED) != null) {
            date = (String) doc.getFieldValues(MD_DATECREATED).iterator().next();
        } else if (topstructDoc != null && topstructDoc.getFieldValues(MD_YEARPUBLISH) != null) {
            date = (String) topstructDoc.getFieldValues(MD_YEARPUBLISH).iterator().next();
        } else if (topstructDoc != null && topstructDoc.getFieldValues(MD_DATECREATED) != null) {
            date = (String) topstructDoc.getFieldValues(MD_DATECREATED).iterator().next();
        }

        if (date != null && !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(date)) {
            Element eleDcDate = new Element("date", nsDc);
            eleDcDate.setText(date);
            eleEuropeanaRecord.addContent(eleDcDate);
        }

        // <dc:creator>
        if (doc.getFieldValues(MD_CREATOR) != null) {
            for (Object fieldValue : doc.getFieldValues(MD_CREATOR)) {
                String creator = (String) fieldValue;
                if (StringUtils.isNotBlank(creator) && !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(creator)) {
                    Element eleDcCreator = new Element("creator", nsDc);
                    eleDcCreator.setText(creator);
                    eleEuropeanaRecord.addContent(eleDcCreator);
                }
            }
        }
        // <dc:created>
        if (doc.getFieldValues(MD_DATECREATED) != null) {
            String created = (String) doc.getFieldValues(MD_DATECREATED).iterator().next();
            if (!StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(created)
                    && !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(created)) {
                Element eleDcCreated = new Element("created", nsDc);
                eleDcCreated.setText(created);
                eleEuropeanaRecord.addContent(eleDcCreated);
            }
        }
        // <dc:issued>
        if (doc.getFieldValues("MD_DATEISSUED") != null) {
            String created = (String) doc.getFieldValues("MD_DATEISSUED").iterator().next();
            if (!StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(created)) {
                Element eleDcCreated = new Element("issued", nsDc);
                eleDcCreated.setText(created);
                eleEuropeanaRecord.addContent(eleDcCreated);
            }
        }
        // creating <dc:subject>
        if (doc.getFieldValues(SolrConstants.DC) != null) {
            for (Object fieldValue : doc.getFieldValues(SolrConstants.DC)) {
                Element eleDcType = new Element("subject", nsDc);
                String subject = (String) fieldValue;
                if (StringUtils.isNotEmpty(subject) && !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(subject)) {
                    eleDcType.setText(subject);
                    eleEuropeanaRecord.addContent(eleDcType);
                }
            }
        }

        // <dc:publisher>
        String publisher = null;
        if (doc.getFieldValues(MD_PUBLISHER) != null) {
            publisher = (String) doc.getFieldValues(MD_PUBLISHER).iterator().next();
        } else if (topstructDoc != null && topstructDoc.getFieldValues(MD_PUBLISHER) != null) {
            publisher = (String) topstructDoc.getFieldValues(MD_PUBLISHER).iterator().next();
        }
        if (publisher != null && !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(publisher)) {
            Element eleDcPublisher = new Element("publisher", nsDc);
            eleDcPublisher.setText(publisher);
            eleEuropeanaRecord.addContent(eleDcPublisher);
        }

        // <dc:type>
        Element eleDcType = new Element("type", nsDc);
        if (doc.getFieldValue(SolrConstants.DOCSTRCT) != null) {
            type = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
            if (type != null && !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(type)) {
                eleDcType.setText(type);
                eleEuropeanaRecord.addContent(eleDcType);
            }
        }

        // <dc:format>
        // <dc:format> second one appears always
        Element eleDcFormat = new Element("format", nsDc);
        eleDcFormat.setText("image/jpeg");
        eleEuropeanaRecord.addContent(eleDcFormat);

        eleDcFormat = new Element("format", nsDc);
        eleDcFormat.setText("application/pdf");
        eleEuropeanaRecord.addContent(eleDcFormat);

        // <dc:source>
        eleEuropeanaRecord.addContent(generateDcSource(doc, topstructDoc, anchorDoc, nsDc));

        // ESE elements have a mandatory order

        // MANDATORY: <europeana:provider>
        Element eleEuropeanaProvider = new Element("provider", nsEuropeana);
        String provider = DataManager.getInstance().getConfiguration().getEseDefaultProvider();
        String field = DataManager.getInstance().getConfiguration().getEseProviderField();
        if (doc.getFieldValues(field) != null) {
            String val = (String) doc.getFieldValues(field).iterator().next();
            if (!StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(val)) {
                provider = val;
            }
        }
        eleEuropeanaProvider.setText(provider);
        eleEuropeanaRecord.addContent(eleEuropeanaProvider);

        // MANDATORY: <europeana:type>
        Element eleEuropeanaType = new Element("type", nsEuropeana);
        String europeanaType = "TEXT";
        if (type != null && !StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(type)) {
            // Retrieve coded ESE type, if available
            Map<String, String> eseTypes = DataManager.getInstance().getConfiguration().getEseTypes();
            if (eseTypes.get(type) != null) {
                europeanaType = eseTypes.get(type);
            }
        }
        eleEuropeanaType.setText(europeanaType);
        eleEuropeanaRecord.addContent(eleEuropeanaType);

        // MANDATORY: <europeana:rights>
        Element eleEuropeanaRights = new Element("rights", nsEuropeana);
        String rights = DataManager.getInstance().getConfiguration().getEseDefaultRightsUrl();
        String rightsField = DataManager.getInstance().getConfiguration().getEseRightsField();
        if (doc.getFieldValues(rightsField) != null) {
            String val = (String) doc.getFieldValues(rightsField).iterator().next();
            if (!StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(val)) {
                rights = val;
            }

        }
        eleEuropeanaRights.setText(rights);
        eleEuropeanaRecord.addContent(eleEuropeanaRights);

        // MANDATORY: <europeana:dataProvider>
        Element eleEuropeanaDataProvider = new Element("dataProvider", nsEuropeana);
        String dataProvider = DataManager.getInstance().getConfiguration().getEseDefaultProvider();
        String dataProviderField = DataManager.getInstance().getConfiguration().getEseDataProviderField();
        if (doc.getFieldValues(dataProviderField) != null) {
            String val = (String) doc.getFieldValues(dataProviderField).iterator().next();
            if (!StringConstants.ACCESSCONDITION_METADATA_ACCESS_RESTRICTED.equals(val)) {
                dataProvider = val;
            }
        }
        eleEuropeanaDataProvider.setText(dataProvider);
        eleEuropeanaRecord.addContent(eleEuropeanaDataProvider);

        // MANDATORY: <europeana:isShownAt> (if <europeana:isShownBy> is not present)
        Element eleEuropeanaIsShownAt = new Element("isShownAt", nsEuropeana);
        String isShownAt = "";
        if (urn != null) {
            isShownAt = DataManager.getInstance().getConfiguration().getUrnResolverUrl() + urn;
        } else if (identifier != null) {
            isShownAt = DataManager.getInstance().getConfiguration().getPiResolverUrl() + identifier;
        }
        eleEuropeanaIsShownAt.setText(isShownAt);
        eleEuropeanaRecord.addContent(eleEuropeanaIsShownAt);

        // MANDATORY: <europeana:isShownBy> (if <europeana:isShownAt> is not present)
        String thumbnail = (String) doc.getFieldValue(SolrConstants.THUMBNAIL);
        String useIdentifier = identifier;
        if (StringUtils.isEmpty(useIdentifier)) {
            useIdentifier = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        }
        if (StringUtils.isNotEmpty(useIdentifier) && StringUtils.isNotEmpty(thumbnail)) {
            Element eleEuropeanaIsShownBy = new Element("isShownBy", nsEuropeana);
            String isShownBy =
                    io.goobi.viewer.controller.DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "records/" + useIdentifier
                            + "/files/images/" + thumbnail + "/full/max/0/default.jpg";
            eleEuropeanaIsShownBy.setText(isShownBy);
            eleEuropeanaRecord.addContent(eleEuropeanaIsShownBy);
        }

        eleMetadata.addContent(eleEuropeanaRecord);
        eleRecord.addContent(eleMetadata);

        return eleRecord;
    }
}
