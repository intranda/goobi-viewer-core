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
package de.intranda.digiverso.presentation.model.viewer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;

/**
 * StructElement essentials with a reduced memory footprint.
 */
public class StructElementStub implements Comparable<StructElementStub>, Serializable {

    private static final long serialVersionUID = -5448163073874698918L;

    private static final Logger logger = LoggerFactory.getLogger(StructElementStub.class);

    public static final String SOURCE_DOC_FORMAT_METS = "METS";
    public static final String SOURCE_DOC_FORMAT_LIDO = "LIDO";

    /** Docstruct types that represent museum objects. */
    private static List<String> museumDocstructTypes;

    /** IDDOC of the Solr document representing this structure element. */
    protected long luceneId;
    /** Identifier of top level structure element of the tree to which this structure element belongs. */
    protected String pi;
    /** Logical structure element ID from METS. */
    protected String logid;
    /** Type of this structure element. */
    protected String docStructType = "";
    /** True if this element is a top level structure element. */
    protected boolean work = false;
    /** True if this element is an anchor element. */
    protected boolean anchor = false;
    /** True if this element is a volume element. */
    protected boolean volume = false;
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
    protected String sourceDocFormat = SOURCE_DOC_FORMAT_METS;
    /** Content of the LABEL or MD_TITLE fields. Used to display the record label in the browser's title bar. */
    protected String label = null;
    /** Name of the data repository for this record. */
    protected String dataRepository;
    /** Map containing all field values from the Solr document. */
    protected Map<String, List<String>> metadataFields = new HashMap<>();

    public StructElementStub() {
        // the emptiness inside
    }

    public StructElementStub(long luceneId) {
        this.luceneId = luceneId;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
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
     * @return the work
     */
    public boolean isWork() {
        return work;
    }

    /**
     * @param work the work to set
     */
    public void setWork(boolean work) {
        this.work = work;
    }

    /**
     * @return the anchor
     */
    public boolean isAnchor() {
        return anchor;
    }

    /**
     * @param anchor the anchor to set
     */
    public void setAnchor(boolean anchor) {
        this.anchor = anchor;
    }

    /**
     * @return the volume
     */
    public boolean isVolume() {
        return volume;
    }

    /**
     * @param volume the volume to set
     */
    public void setVolume(boolean volume) {
        this.volume = volume;
    }

    /**
     * @return the numVolumes
     */
    public long getNumVolumes() {
        return numVolumes;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    public long getLuceneId() {
        return luceneId;
    }

    /**
     * @return the logid
     */
    public String getLogid() {
        return logid;
    }

    /**
     * @param logid the logid to set
     */
    public void setLogid(String logid) {
        this.logid = logid;
    }

    /**
     * @return the docStructType
     */
    public String getDocStructType() {
        return docStructType;
    }

    /**
     * @param docStructType the docStructType to set
     */
    public void setDocStructType(String docStructType) {
        this.docStructType = docStructType;
    }

    /**
     * @return the imageNumber
     */
    public int getImageNumber() {
        return imageNumber;
    }

    /**
     * @param imageNumber the imageNumber to set
     */
    public void setImageNumber(int imageNumber) {
        this.imageNumber = imageNumber;
    }

    /**
     * @return the volumeNo
     */
    public String getVolumeNo() {
        return volumeNo;
    }

    /**
     * @param volumeNo the volumeNo to set
     */
    public void setVolumeNo(String volumeNo) {
        this.volumeNo = volumeNo;
    }

    /**
     * @return the volumeNoSort
     */
    public String getVolumeNoSort() {
        return volumeNoSort;
    }

    /**
     * @param volumeNoSort the volumeNoSort to set
     */
    public void setVolumeNoSort(String volumeNoSort) {
        this.volumeNoSort = volumeNoSort;
    }

    public String getUrl() {
        if (anchor) {
            return getUrl(PageType.viewToc);
        }
        return getUrl(PageType.viewImage);
    }

    /**
     * Returns a URL to this element (but for the metadata view).
     *
     * @return
     * @throws PresentationException
     */
    public String getMetadataUrl() throws PresentationException {
        return getUrl(PageType.viewMetadata);
    }

    public String getUrl(String pageTypeName) {
        PageType pageType = PageType.getByName(pageTypeName);
        if (pageType != null) {
            return getUrl(pageType);
        }
        return getUrl();
    }

    public String getUrl(PageType pageType) {
        // Only viewToc and viewMetadata are allowed for anchors
        if (anchor && pageType != PageType.viewMetadata) {
            pageType = PageType.viewToc;
        }

        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(pageType.getName()).append('/').append(getPi()).append(
                '/').append(getImageNumber()).append('/');
        if (StringUtils.isNotEmpty(logid)) {
            sbUrl.append(logid).append('/');
        }

        return sbUrl.toString();
    }

    /**
     * @return
     * @deprecated Use StructElementStub.isLidoRecord()
     */
    @Deprecated
    public boolean isMuseumType() {
        return isLidoRecord();
    }

    /**
     * 
     * @return
     */
    public boolean isLidoRecord() {
        return "LIDO".equals(sourceDocFormat);
    }

    /**
     * Returns the number of pages (for the entire record, not a particular docstruct).
     *
     * @return
     * @throws IndexUnreachableException
     */
    public int getNumPages() throws IndexUnreachableException {
        
        try {
            String query = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':').append(getPi()).append(" AND ").append(SolrConstants.DOCTYPE)
                    .append(':').append(DocType.PAGE.name()).toString();
            SolrDocumentList result = DataManager.getInstance().getSearchIndex().search(query, 0, null, Collections.singletonList(
                    SolrConstants.ORDER));
            return (int) result.getNumFound();
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }

        return 0;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    /**
     * @return the sourceDocFormat
     */
    public String getSourceDocFormat() {
        return sourceDocFormat;
    }

    /**
     * @param sourceDocFormat the sourceDocFormat to set
     */
    public void setSourceDocFormat(String sourceDocFormat) {
        this.sourceDocFormat = sourceDocFormat;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return getLabel(null);
    }

    /**
     * Returns a language specific version of MD_TITLE, if available. If not available or if locale is null, the regular label is returned.
     * 
     * @param language
     * @return Locale-specific version of MD_TITLE if requested and found; label otherwise
     * @should return locale specific title if so requested
     * @should return label if no locale specific title found
     * @should return label if locale is null
     */
    public String getLabel(String language) {
        if (language != null) {
            String mdField = SolrConstants.TITLE + "_LANG_" + language.toUpperCase();
            String label = getMetadataValue(mdField);
            if (StringUtils.isNotEmpty(label)) {
                return label;
            }
        }

        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the dataRepository
     */
    public String getDataRepository() {
        return dataRepository;
    }

    /**
     * @param dataRepository the dataRepository to set
     */
    public void setDataRepository(String dataRepository) {
        this.dataRepository = dataRepository;
    }

    /**
     * @return the metadataFields
     */
    public Map<String, List<String>> getMetadataFields() {
        return metadataFields;
    }

    /**
     * @param metadataFields the metadataFields to set
     */
    public void setMetadataFields(Map<String, List<String>> metadataFields) {
        this.metadataFields = metadataFields;
    }

    /**
     * Returns the first meetadata value for the language speciic version of the given field name. If no value is found, the value of the generic
     * version is returned.
     * 
     * @param fieldName Solr field name
     * @param language ISO 639-1 language code
     * @return
     */
    public String getMetadataValueForLanguage(String fieldName, String language) {
        if (StringUtils.isNotEmpty(language)) {
            String value = getMetadataValue(fieldName + SolrConstants._LANG_ + language.toUpperCase());
            if (value != null) {
                return value;
            }
        }

        return getMetadataValue(fieldName);
    }

    /**
     * Returns the first meetadata value for the given field name.
     *
     * @param fieldName Solr field name.
     * @return
     */
    public String getMetadataValue(String fieldName) {
        List<String> values = getMetadataValues(fieldName);
        if (!values.isEmpty()) {
            return values.get(0);
        } else if(!fieldName.contains("_LANG_")){
            return  getMetadataValue(fieldName + "_LANG_" + BeanUtils.getLocale().getLanguage().toUpperCase());
        }

        return null;
    }

    /**
     * Returns all metadata values for the given field name.
     *
     * @param fieldName Field Name of Lucene
     * @return
     */
    public List<String> getMetadataValues(String fieldName) {
        List<String> values = metadataFields.get(fieldName);
        if (values != null) {
            return values;
        } else if(!fieldName.contains("_LANG_")){
            return  getMetadataValues(fieldName + "_LANG_" + BeanUtils.getLocale().getLanguage().toUpperCase());
        }

        return Collections.emptyList();
    }

    /**
     * Generates a ContextObject (for a COinS <span> element) containing metadata from this <code>StructElement</code>.
     *
     * @param currentUrl
     * @param topStruct StructElementStub representing the top structure element.
     * @return
     * @throws PresentationException
     * @should generate string element correctly
     */
    public String generateContextObject(String currentUrl, StructElementStub topStruct) throws PresentationException {
        StringBuilder sb = new StringBuilder("ctx_ver=Z39.88-2004&rft_val_fmt=info:ofi/fmt:kev:mtx:");

        String format = null;

        // Format
        if (topStruct.getDocStructType() != null) {
            switch (topStruct.getDocStructType()) {
                case "Monograph":
                    format = "book";
                    break;
                case "Manuscript":
                    format = "book";
                    break;
                case "Chapter":
                    format = "bookitem";
                    break;
                case "PeriodicalVolume":
                    format = "journal";
                    break;
                case "PeriodicalIssue":
                    format = "issue";
                    break;
                case "Article":
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

        //        StructElement topStruct = getTopStruct();

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

        //        // Chapter metadata
        //        if (!this.equals(topStruct)) {
        //            sb.append(getKEVForField(this, LuceneConstants.TITLE, "rft.btitle", "&"));
        //            //            sb.append(getKEVForField(this, LuceneConstants.TITLE, "rft.atitle", "&"));
        //            String firstPage = getMetadataValue("ORDERLABELFIRST");
        //            String lastPage = getMetadataValue("ORDERLABELLAST");
        //            if (firstPage != null && lastPage != null) {
        //                try {
        //                    sb.append("&rft.pages=").append(URLEncoder.encode(new StringBuilder(firstPage).append('-').append(lastPage).toString(), "UTF-8"));
        //                } catch (UnsupportedEncodingException e) {
        //                    logger.error(e.getMessage());
        //                }
        //            }
        //        } else {
        //        sb.append(getKEVForField(topStruct, LuceneConstants.TITLE, "rft.title", "&"));
        //        }

        if (StringUtils.isNotEmpty(currentUrl)) {
            try {
                sb.append("&rft.id=").append(URLEncoder.encode(currentUrl, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
        }

        return sb.toString();
    }
    
    public IMetadataValue getMultiLanguageMetadataValue(String fieldName) {
        List<String> fieldNames = this.getMetadataFields().keySet().stream().filter(key -> key.equals(fieldName) || key.startsWith(fieldName + "_LANG_")).collect(Collectors.toList());
        Map<String, List<String>> valueMap = fieldNames.stream().collect(Collectors.toMap(field -> getLanguage(field), field -> getMetadataValues(field)));
        if(valueMap.size() == 1 && valueMap.containsKey(MultiLanguageMetadataValue.DEFAULT_LANGUAGE)) {
            //only default language: Simple MEtadata value
            return new SimpleMetadataValue(StringUtils.join(valueMap.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE), "; "));
        } else {
            return new MultiLanguageMetadataValue(valueMap);
        }
    }
    
    private String getLanguage(String fieldName) {
        if(fieldName.contains("_LANG_")) {
            return fieldName.substring(fieldName.indexOf("_LANG_") + "_LANG_".length());
        } else {
            return MultiLanguageMetadataValue.DEFAULT_LANGUAGE;
        }
    }

    /**
     *
     * @param se
     * @param solrField
     * @param targetField
     * @param prefix
     * @return
     */
    public static String getKEVForField(StructElementStub se, String solrField, String targetField, String prefix) {
        if (se == null) {
            throw new IllegalArgumentException("se may not be null");
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
