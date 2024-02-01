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
package io.goobi.viewer.model.viewer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * StructElement essentials with a reduced memory footprint.
 */
public class StructElementStub implements Comparable<StructElementStub>, Serializable {

    private static final long serialVersionUID = -5448163073874698918L;

    private static final Logger logger = LogManager.getLogger(StructElementStub.class);

    /** IDDOC of the Solr document representing this structure element. */
    protected final long luceneId;
    /** Identifier of top level structure element of the tree to which this structure element belongs. */
    protected String pi;
    /** Logical structure element ID from METS. */
    protected String logid;
    /** Type of this structure element. */
    protected String docStructType = "";
    /** Solr document type. */
    protected DocType docType = null;
    /** True if this element is a top level structure element. */
    protected boolean work = false;
    /** True if this element is an anchor element. */
    protected boolean anchor = false;
    /** True if this element is a volume element. */
    protected boolean volume = false;
    /** True if this element represents a CMS page. */
    protected boolean cmsPage = false;
    /** Number of contained volumes (anchors only) */
    protected long numVolumes = 0;
    /** Volume label of this element (only for records that are part of a multi-volume record). */
    protected String volumeNo = null;
    /** Volume number of this element (only for records that are part of a multi-volume record). */
    protected String volumeNoSort = null;
    /** Image number. */
    protected int imageNumber = 0;
    /** Identifier of the partner institution to which this record belongs (top-level structure elements only). */
    protected String partnerId = null;
    /** Format of document format from which this record was indexed. */
    protected String sourceDocFormat = SolrConstants.SOURCEDOCFORMAT_METS;
    /** Content of the LABEL or MD_TITLE fields. Used to display the record label in the browser's title bar. */
    protected String label = null;
    /** Name of the data repository for this record. */
    protected String dataRepository;
    /** Map containing all field values from the Solr document. */
    protected Map<String, List<String>> metadataFields = new HashMap<>();

    /**
     * <p>
     * Constructor for StructElementStub.
     * </p>
     */
    public StructElementStub() {
        this.luceneId = 1L;
    }

    /**
     * <p>
     * Constructor for StructElementStub.
     * </p>
     *
     * @param luceneId a long.
     */
    public StructElementStub(long luceneId) {
        this.luceneId = luceneId;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public int compareTo(StructElementStub o) {
        if (luceneId > o.luceneId) {
            return 1;
        } else if (luceneId < o.luceneId) {
            return -1;
        }
        return 0;
    }

    /**
     * <p>
     * getDisplayLabel.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayLabel() {
        String localLabel = getMetadataValue(SolrConstants.LABEL);
        if (StringUtils.isEmpty(localLabel)) {
            localLabel = getMetadataValue(SolrConstants.TITLE);
            if (StringUtils.isEmpty(localLabel)) {
                localLabel = getDocStructType();
            }
            if (StringUtils.isEmpty(localLabel)) {
                localLabel = "doctype_" + docType.name();
            }
        }

        return localLabel;
    }

    /**
     * <p>
     * isWork.
     * </p>
     *
     * @return the work
     */
    public boolean isWork() {
        return work;
    }

    /**
     * <p>
     * Setter for the field <code>work</code>.
     * </p>
     *
     * @param work the work to set
     */
    public void setWork(boolean work) {
        this.work = work;
    }

    /**
     * <p>
     * isAnchor.
     * </p>
     *
     * @return the anchor
     */
    public boolean isAnchor() {
        return anchor;
    }

    /**
     * <p>
     * Setter for the field <code>anchor</code>.
     * </p>
     *
     * @param anchor the anchor to set
     */
    public void setAnchor(boolean anchor) {
        this.anchor = anchor;
    }

    /**
     * <p>
     * isVolume.
     * </p>
     *
     * @return the volume
     */
    public boolean isVolume() {
        return volume;
    }

    /**
     * <p>
     * Setter for the field <code>volume</code>.
     * </p>
     *
     * @param volume the volume to set
     */
    public void setVolume(boolean volume) {
        this.volume = volume;
    }

    /**
     * @return the cmsPage
     */
    public boolean isCmsPage() {
        return cmsPage;
    }

    /**
     * @param cmsPage the cmsPage to set
     */
    public void setCmsPage(boolean cmsPage) {
        this.cmsPage = cmsPage;
    }

    /**
     * <p>
     * Getter for the field <code>numVolumes</code>.
     * </p>
     *
     * @return the numVolumes
     */
    public long getNumVolumes() {
        return numVolumes;
    }

    /**
     * <p>
     * isGroup.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isGroup() {
        return DocType.GROUP.equals(docType);
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>
     * Setter for the field <code>pi</code>.
     * </p>
     *
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * <p>
     * Getter for the field <code>luceneId</code>.
     * </p>
     *
     * @return a long.
     */
    public long getLuceneId() {
        return luceneId;
    }

    /**
     * <p>
     * Getter for the field <code>logid</code>.
     * </p>
     *
     * @return the logid
     */
    public String getLogid() {
        return logid;
    }

    /**
     * <p>
     * Setter for the field <code>logid</code>.
     * </p>
     *
     * @param logid the logid to set
     */
    public void setLogid(String logid) {
        this.logid = logid;
    }

    /**
     * <p>
     * Getter for the field <code>docStructType</code>.
     * </p>
     *
     * @return the docStructType
     */
    public String getDocStructType() {
        return docStructType;
    }

    /**
     * <p>
     * Setter for the field <code>docStructType</code>.
     * </p>
     *
     * @param docStructType the docStructType to set
     */
    public void setDocStructType(String docStructType) {
        this.docStructType = docStructType;
    }

    /**
     * @return the docType
     */
    public DocType getDocType() {
        return docType;
    }

    /**
     * @param docType the docType to set
     */
    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    /**
     * <p>
     * Getter for the field <code>imageNumber</code>.
     * </p>
     *
     * @return the imageNumber
     */
    public int getImageNumber() {
        return imageNumber;
    }

    /**
     * <p>
     * Setter for the field <code>imageNumber</code>.
     * </p>
     *
     * @param imageNumber the imageNumber to set
     */
    public void setImageNumber(int imageNumber) {
        this.imageNumber = imageNumber;
    }

    /**
     * <p>
     * Getter for the field <code>volumeNo</code>.
     * </p>
     *
     * @return the volumeNo
     */
    public String getVolumeNo() {
        return volumeNo;
    }

    /**
     * <p>
     * Setter for the field <code>volumeNo</code>.
     * </p>
     *
     * @param volumeNo the volumeNo to set
     */
    public void setVolumeNo(String volumeNo) {
        this.volumeNo = volumeNo;
    }

    /**
     * <p>
     * Getter for the field <code>volumeNoSort</code>.
     * </p>
     *
     * @return the volumeNoSort
     */
    public String getVolumeNoSort() {
        return volumeNoSort;
    }

    /**
     * <p>
     * Setter for the field <code>volumeNoSort</code>.
     * </p>
     *
     * @param volumeNoSort the volumeNoSort to set
     */
    public void setVolumeNoSort(String volumeNoSort) {
        this.volumeNoSort = volumeNoSort;
    }

    /**
     * <p>
     * getUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUrl() {
        if (anchor) {
            return getUrl(PageType.viewToc);
        }
        return getUrl(PageType.viewObject);
    }

    /**
     * Returns a URL to this element (but for the metadata view).
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public String getMetadataUrl() throws PresentationException {
        return getUrl(PageType.viewMetadata);
    }

    /**
     * <p>
     * getUrl.
     * </p>
     *
     * @param pageTypeName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getUrl(String pageTypeName) {
        PageType pageType = PageType.getByName(pageTypeName);
        if (pageType != null) {
            return getUrl(pageType);
        }
        return getUrl();
    }

    /**
     * <p>
     * getUrl.
     * </p>
     *
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return a {@link java.lang.String} object.
     */
    public String getUrl(final PageType pageType) {
        // Only viewToc and viewMetadata are allowed for anchors
        PageType usePageType = pageType;
        if (anchor && usePageType != PageType.viewMetadata) {
            usePageType = PageType.viewToc;
        }

        StringBuilder sbUrl = new StringBuilder();
        boolean topstruct = isWork() || isAnchor() || isGroup();
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                .append('/')
                .append(DataManager.getInstance().getUrlBuilder().buildPageUrl(pi, imageNumber, logid, usePageType, topstruct));

        return sbUrl.toString();
    }

    /**
     * <p>
     * isLidoRecord.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isLidoRecord() {
        return SolrConstants.SOURCEDOCFORMAT_LIDO.equals(sourceDocFormat);
    }

    /**
     *
     * @return true if record has images; false otherwise
     */
    public boolean isHasImages() {
        String imageAvailable = getMetadataValue(SolrConstants.BOOL_IMAGEAVAILABLE);
        return imageAvailable != null && Boolean.valueOf(imageAvailable);
    }

    /**
     * Returns the number of pages (for the entire record, not a particular docstruct).
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getNumPages() throws IndexUnreachableException {

        try {
            String query = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':')
                    .append(getPi())
                    .append(" AND ")
                    .append(SolrConstants.DOCTYPE)
                    .append(':')
                    .append(DocType.PAGE.name())
                    .toString();
            SolrDocumentList result =
                    DataManager.getInstance().getSearchIndex().search(query, 0, null, Collections.singletonList(SolrConstants.ORDER));
            return (int) result.getNumFound();
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
        }

        return 0;
    }

    /**
     * <p>
     * Getter for the field <code>partnerId</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPartnerId() {
        return partnerId;
    }

    /**
     * <p>
     * Setter for the field <code>partnerId</code>.
     * </p>
     *
     * @param partnerId a {@link java.lang.String} object.
     */
    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    /**
     * <p>
     * Getter for the field <code>sourceDocFormat</code>.
     * </p>
     *
     * @return the sourceDocFormat
     */
    public String getSourceDocFormat() {
        return sourceDocFormat;
    }

    /**
     * <p>
     * Setter for the field <code>sourceDocFormat</code>.
     * </p>
     *
     * @param sourceDocFormat the sourceDocFormat to set
     */
    public void setSourceDocFormat(String sourceDocFormat) {
        // logger.trace("setSourceDocFormat: {}", sourceDocFormat);
        this.sourceDocFormat = sourceDocFormat;
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    public String getLabel() {
        return getLabel(null);
    }

    /**
     * Returns a language specific version of MD_TITLE, if available. If not available or if locale is null, the regular label is returned.
     *
     * @param language a {@link java.lang.String} object.
     * @return Locale-specific version of MD_TITLE if requested and found; label otherwise
     * @should return locale specific title if so requested
     * @should return label if no locale specific title found
     * @should return label if locale is null
     */
    public String getLabel(String language) {
        if (language != null) {
            String mdField = SolrConstants.TITLE + SolrConstants.MIDFIX_LANG + language.toUpperCase();
            String localLabel = getMetadataValue(mdField);
            if (StringUtils.isNotEmpty(localLabel)) {
                return localLabel;
            }
        }

        return label;
    }

    /**
     * <p>
     * Setter for the field <code>label</code>.
     * </p>
     *
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * <p>
     * Getter for the field <code>dataRepository</code>.
     * </p>
     *
     * @return the dataRepository
     */
    public String getDataRepository() {
        return dataRepository;
    }

    /**
     * <p>
     * Setter for the field <code>dataRepository</code>.
     * </p>
     *
     * @param dataRepository the dataRepository to set
     */
    public void setDataRepository(String dataRepository) {
        this.dataRepository = dataRepository;
    }

    /**
     * <p>
     * Getter for the field <code>metadataFields</code>.
     * </p>
     *
     * @return the metadataFields
     */
    public Map<String, List<String>> getMetadataFields() {
        return metadataFields;
    }

    /**
     * <p>
     * Setter for the field <code>metadataFields</code>.
     * </p>
     *
     * @param metadataFields the metadataFields to set
     */
    public void setMetadataFields(Map<String, List<String>> metadataFields) {
        this.metadataFields = metadataFields;
    }

    /**
     * Returns the first metadata value for the language specific version of the given field name. If no value is found, the value of the generic
     * version is returned.
     *
     * @param fieldName Solr field name
     * @param language ISO 639-1 language code
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataValueForLanguage(String fieldName, String language) {
        if (StringUtils.isNotEmpty(language)) {
            String value = getMetadataValue(fieldName + SolrConstants.MIDFIX_LANG + language.toUpperCase());
            if (value != null) {
                return value;
            }
        }

        return getMetadataValue(fieldName);
    }

    /**
     * Returns the first metadata value for the given field name.
     *
     * @param fieldName Solr field name.
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataValue(String fieldName) {
        List<String> values = getMetadataValues(fieldName);
        if (!values.isEmpty()) {
            return values.get(0);
        } else if (fieldName != null && !fieldName.contains(SolrConstants.MIDFIX_LANG)) {
            return getMetadataValue(fieldName + SolrConstants.MIDFIX_LANG + BeanUtils.getLocale().getLanguage().toUpperCase());
        }

        return null;
    }

    /**
     * Returns all metadata values for the given field name.
     *
     * @param fieldName Field Name of Lucene
     * @return a {@link java.util.List} object.
     */
    public List<String> getMetadataValues(String fieldName) {
        List<String> values = metadataFields.get(fieldName);
        if (values != null) {
            return values;
        } else if (fieldName != null && !fieldName.contains(SolrConstants.MIDFIX_LANG)) {
            return getMetadataValues(fieldName + SolrConstants.MIDFIX_LANG + BeanUtils.getLocale().getLanguage().toUpperCase());
        }

        return Collections.emptyList();
    }

    /**
     * Generates a ContextObject (for a COinS <span> element) containing metadata from this <code>StructElement</code>.
     *
     * @param currentUrl a {@link java.lang.String} object.
     * @param topStruct StructElementStub representing the top structure element.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @should generate string element correctly
     * @should return unknown format if topstruct null
     * @should throw illegal
     */
    public String generateContextObject(String currentUrl, StructElementStub topStruct) throws PresentationException {
        StringBuilder sb = new StringBuilder("ctx_ver=Z39.88-2004&rft_val_fmt=info:ofi/fmt:kev:mtx:");

        String format = null;

        // Format
        if (topStruct != null && topStruct.getDocStructType() != null) {
            switch (topStruct.getDocStructType().toLowerCase()) {
                case "monograph":
                    format = "book";
                    break;
                case "manuscript":
                    format = "book";
                    break;
                case "chapter":
                    format = "bookitem";
                    break;
                case "periodicalVolume":
                    format = "journal";
                    break;
                case "periodicalIssue":
                    format = "issue";
                    break;
                case "article":
                    format = "article";
                    break;
                default:
                    format = "unknown";
                    break;
            }
        } else {
            format = "unknown";
        }
        sb.append(format);

        // Topstruct metadata
        sb.append(getKEVForField(topStruct, SolrConstants.TITLE, "rft.title", "&"));
        sb.append(getKEVForField(topStruct, "MD_CREATOR", "rft.au", "&"));
        sb.append(getKEVForField(topStruct, "MD_CORPORATION", "rft.aucorp", "&"));
        sb.append(getKEVForField(topStruct, "MD_ISBN", "rft.isbn", "&"));
        sb.append(getKEVForField(topStruct, "MD_ISSN", "rft.issn", "&"));
        sb.append(getKEVForField(topStruct, "MD_YEARPUBLISH", "rft.date", "&"));
        sb.append(getKEVForField(topStruct, "MD_PUBLISHER", "rft.pub", "&"));
        sb.append(getKEVForField(topStruct, "MD_PLACEPUBLISH", "rft.place", "&"));
        sb.append(getKEVForField(topStruct, "MD_EDITION", "rft.edition", "&"));
        sb.append(getKEVForField(topStruct, "MD_SERIES", "rft.series", "&"));
        sb.append(getKEVForField(topStruct, "MD_SUBJECT", "rft.subject", "&"));
        sb.append(getKEVForField(topStruct, "MD_LANGUAGE", "rft.language", "&"));
        sb.append(getKEVForField(topStruct, "NUMPAGES", "rft.tpages", "&"));

        if (StringUtils.isNotEmpty(currentUrl)) {
            try {
                sb.append("&rft.id=").append(URLEncoder.encode(currentUrl, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
        }

        return sb.toString();
    }

    public boolean isHasMetadata(String fieldName) {
        return this.getMetadataFields().containsKey(fieldName);
    }

    /**
     * <p>
     * getMultiLanguageMetadataValue.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @return a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     */
    public IMetadataValue getMultiLanguageMetadataValue(String fieldName) {
        List<String> fieldNames = this.getMetadataFields()
                .keySet()
                .stream()
                .filter(key -> key.equals(fieldName) || key.startsWith(fieldName + SolrConstants.MIDFIX_LANG))
                .collect(Collectors.toList());
        Map<String, List<String>> valueMap =
                fieldNames.stream().collect(Collectors.toMap(field -> getLanguage(field), this::getMetadataValues));
        if (valueMap.size() == 1 && valueMap.containsKey(MultiLanguageMetadataValue.DEFAULT_LANGUAGE)) {
            //only default language: Simple MEtadata value
            return new SimpleMetadataValue(StringUtils.join(valueMap.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE), "; "));
        }

        return new MultiLanguageMetadataValue(valueMap);
    }

    private static String getLanguage(String fieldName) {
        if (fieldName.contains(SolrConstants.MIDFIX_LANG)) {
            return fieldName.substring(fieldName.indexOf(SolrConstants.MIDFIX_LANG) + SolrConstants.MIDFIX_LANG.length());
        }

        return MultiLanguageMetadataValue.DEFAULT_LANGUAGE;
    }

    /**
     * <p>
     * getKEVForField.
     * </p>
     *
     * @param se a {@link io.goobi.viewer.model.viewer.StructElementStub} object.
     * @param solrField a {@link java.lang.String} object.
     * @param targetField a {@link java.lang.String} object.
     * @param prefix a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getKEVForField(StructElementStub se, String solrField, String targetField, String prefix) {
        if (se == null) {
            return "";
        }
        if (StringUtils.isEmpty(solrField)) {
            throw new IllegalArgumentException("solrField may not be null or empty");
        }
        if (StringUtils.isEmpty(targetField)) {
            throw new IllegalArgumentException("targetField may not be null or empty");
        }
        String value = se.getMetadataValue(solrField);
        if (StringUtils.isNotEmpty(value)) {
            try {
                StringBuilder sb = new StringBuilder();
                if (StringUtils.isNotEmpty(prefix)) {
                    sb.append(prefix);
                }
                sb.append(targetField).append('=').append(URLEncoder.encode(value, "UTF-8"));
                return sb.toString();
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
        }

        return "";
    }

}
