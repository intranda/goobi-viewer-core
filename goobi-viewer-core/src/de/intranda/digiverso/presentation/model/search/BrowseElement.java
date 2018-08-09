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
package de.intranda.digiverso.presentation.model.search;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrConstants.MetadataGroupType;
import de.intranda.digiverso.presentation.controller.StringTools;
import de.intranda.digiverso.presentation.controller.imaging.IIIFUrlHandler;
import de.intranda.digiverso.presentation.controller.imaging.ThumbnailHandler;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.crowdsourcing.DisplayUserGeneratedContent;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter.MetadataParameterType;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.model.viewer.StructElementStub;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;

/**
 * Representation of a search hit. TODO integrate into SearchHit
 */
public class BrowseElement implements Serializable {

    private static final long serialVersionUID = 6621169815560734613L;

    private static final Logger logger = LoggerFactory.getLogger(BrowseElement.class);

    @JsonIgnore
    private String fulltext;
    private String fulltextForHtml;
    /** Element label (usually the title). */
    private final String label;
    /** Truncated and highlighted variant of the label. */
    private String labelShort;
    /** Type of the index document. */
    private DocType docType;
    /** Type of grouped metadata document (person, etc.) */
    @JsonIgnore
    private MetadataGroupType metadataGroupType = null;
    /** Name of the grouped metadata field. */
    @JsonIgnore
    private String originalFieldName = null;
    /** Type of the docstruct. */
    private String docStructType;
    private long iddoc;
    private String thumbnailUrl;
    private boolean thumbnailAccessDenied = false;
    private int imageNo;
    @JsonIgnore
    private String volumeNo = null;
    /** StructElementStubs for hierarchy URLs. */
    @JsonIgnore
    private List<StructElementStub> structElements = new ArrayList<>();
    @JsonIgnore
    private boolean anchor = false;
    @JsonIgnore
    private boolean hasImages = false;
    @JsonIgnore
    private boolean hasMedia = false;
    @JsonIgnore
    private boolean useOverviewPage = false;
    @JsonIgnore
    private long numVolumes = 0;
    private String pi;
    private String logId;
    @JsonIgnore
    private NavigationHelper navigationHelper;
    @JsonIgnore
    private List<Metadata> metadataList = null;
    /**
     * List of just the metadata fields that were added because they contained search terms (for use where not the entire metadata list is desired).
     */
    @JsonIgnore
    private final List<Metadata> additionalMetadataList = new ArrayList<>();
    @JsonIgnore
    private String mimeType = "";
    @JsonIgnore
    private String contextObject;
    private String url;
    @JsonIgnore
    private String sidebarPrevUrl;
    @JsonIgnore
    private String sidebarNextUrl;
    @JsonIgnore
    private final Locale locale;
    @JsonIgnore
    private final String dataRepository;

    private List<String> recordLanguages;

    /**
     * Constructor for unit tests and special instances.
     * 
     * @param pi
     * @param label
     * @param locale
     * @param fulltext
     * @param useOverviewPage
     * @should build overview page url correctly
     */
    BrowseElement(String pi, int imageNo, String label, String fulltext, boolean useOverviewPage, Locale locale, String dataRepository) {
        this.pi = pi;
        this.imageNo = imageNo;
        this.label = label;
        this.fulltext = fulltext;
        this.useOverviewPage = useOverviewPage;
        this.locale = locale;
        this.metadataList = new ArrayList<>();
        this.url = generateUrl();
        this.dataRepository = dataRepository;
    }

    /**
     * Constructor.
     *
     * @param structElement {@link StructElement}
     * @param metadataList
     * @param locale
     * @param fulltext
     * @param useThumbnail
     * @param
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    BrowseElement(StructElement structElement, List<Metadata> metadataList, Locale locale, String fulltext, boolean useThumbnail,
            Map<String, Set<String>> searchTerms, ThumbnailHandler thumbs)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        this.metadataList = Metadata.filterMetadataByLanguage(metadataList, locale != null ? locale.getLanguage() : null);
        this.locale = locale;
        this.fulltext = fulltext;

        // Collect the docstruct hierarchy
        StructElement anchorStructElement = null;
        StructElement topStructElement = null;
        StructElement tempElement = structElement;
        while (tempElement != null && !tempElement.isWork()) {
            structElements.add(tempElement.createStub());
            tempElement = tempElement.getParent();
        }
        // Add topstruct to the hierarchy
        if (tempElement != null) {
            structElements.add(tempElement.createStub());
            topStructElement = tempElement;
        }
        // TODO this should be obsolete
        if (structElement.isWork()) {
            topStructElement = structElement;
        }

        // Determine Solr document type. Must happen before certain things, such as label generation.
        docType = DocType.getByName(structElement.getMetadataValue(SolrConstants.DOCTYPE));
        if (DocType.METADATA.equals(docType)) {
            metadataGroupType = MetadataGroupType.getByName(structElement.getMetadataValue(SolrConstants.METADATATYPE));
            // The LABEL field in grouped metadata docs contains the name of the field defined in the indexed configuration
            originalFieldName = structElement.getMetadataValue(SolrConstants.LABEL);
        }

        // If the topstruct is a volume of any kind or a subelement, add the anchor and volume labels to
        if (!structElement.isAnchor() && topStructElement != null) {
            if (this.metadataList == null) {
                this.metadataList = new ArrayList<>();
            }
            int position = 0;
            // Add anchor label to volumes
            if (!structElement.isAnchor()) {
                anchorStructElement = topStructElement.getParent();
                if (anchorStructElement != null) {
                    // Add anchor to the docstruct hierarchy
                    structElements.add(anchorStructElement.createStub());
                    if (DataManager.getInstance().getConfiguration().isDisplayTopstructLabel()) {
                        String anchorLabel = generateLabel(anchorStructElement, locale);
                        if (StringUtils.isNotEmpty(anchorLabel)) {
                            this.metadataList.add(position,
                                    new Metadata(
                                            anchorStructElement.getDocStructType(), null, new MetadataParameter(MetadataParameterType.FIELD, null,
                                                    anchorStructElement.getDocStructType(), null, null, null, null, false, false),
                                            Helper.intern(anchorLabel)));
                            position++;
                        }
                    }
                }
            }
            // Add topstruct label to lower docstructs
            if (!structElement.isWork() && DataManager.getInstance().getConfiguration().isDisplayTopstructLabel()) {
                String topstructLabel = generateLabel(topStructElement, locale);
                if (StringUtils.isNotEmpty(topstructLabel)) {
                    // Add volume number, if the parent is a volume
                    if (topStructElement.isAnchorChild() && StringUtils.isNotEmpty(topStructElement.getVolumeNo())) {
                        topstructLabel = new StringBuilder(topstructLabel).append(" (").append(topStructElement.getVolumeNo()).append(')').toString();
                    }
                    this.metadataList.add(position,
                            new Metadata(
                                    topStructElement.getDocStructType(), null, new MetadataParameter(MetadataParameterType.FIELD, null,
                                            topStructElement.getDocStructType(), null, null, null, null, false, false),
                                    Helper.intern(topstructLabel)));
                }
            }
        }

        if (this.metadataList != null) {
            int length = DataManager.getInstance().getConfiguration().getSearchHitMetadataValueLength();
            int number = DataManager.getInstance().getConfiguration().getSearchHitMetadataValueNumber();
            for (Metadata md : this.metadataList) {
                for (MetadataParameter param : md.getParams()) {
                    StructElement elementToUse = structElement;
                    if (StringUtils.isNotEmpty(param.getSource())) {
                        tempElement = structElement;
                        while (tempElement != null) {
                            if (param.getSource().equals(tempElement.getDocStructType())) {
                                elementToUse = tempElement;
                                break;
                            }
                            tempElement = tempElement.getParent();
                        }
                    } else if (MetadataParameterType.TOPSTRUCTFIELD.equals(param.getType()) && topStructElement != null) {
                        // Use topstruct value, if the parameter has the type "topstructfield"
                        elementToUse = topStructElement;
                    } else if (MetadataParameterType.ANCHORFIELD.equals(param.getType())) {
                        // Use anchor value, if the parameter has the type "anchorfield"
                        if (anchorStructElement != null) {
                            elementToUse = anchorStructElement;
                        } else {
                            // Add empty parameter if there is no anchor
                            md.setParamValue(0, md.getParams().indexOf(param), "", null, null, null, locale);
                            continue;
                        }
                    }
                    int count = 0;
                    List<String> metadataValues = elementToUse.getMetadataValues(param.getKey());
                    // If the current element does not contain metadata values, look in the topstruct
                    if (metadataValues.isEmpty()) {
                        if (topStructElement != null && !topStructElement.equals(elementToUse)
                                && !MetadataParameterType.ANCHORFIELD.equals(param.getType()) && !param.isDontUseTopstructValue()) {
                            metadataValues = topStructElement.getMetadataValues(param.getKey());
                            // logger.debug("Checking topstruct metadata: " + topStructElement.getDocStruct());
                        } else {
                            md.setParamValue(count, md.getParams().indexOf(param), "", null, null, null, locale);
                            count++;
                        }
                    }
                    // Set actual values
                    for (String value : metadataValues) {
                        if (count >= md.getNumber() && md.getNumber() != -1 || count >= number) {
                            break;
                        }
                        if (length > 0 && value.length() > length) {
                            value = new StringBuilder(value.substring(0, length - 3)).append("...").toString();
                        }
                        // Add highlighting
                        if (searchTerms != null) {
                            if (searchTerms.get(md.getLabel()) != null) {
                                value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(md.getLabel()));
                            }
                            if (searchTerms.get(SolrConstants.DEFAULT) != null) {
                                value = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(SolrConstants.DEFAULT));
                            }
                        }
                        md.setParamValue(count, md.getParams().indexOf(param), Helper.intern(value), null,
                                param.isAddUrl() ? elementToUse.getUrl() : null, null, locale);
                        count++;
                    }
                }
            }
        }

        if (navigationHelper == null) {
            try {
                navigationHelper = BeanUtils.getNavigationHelper();
            } catch (NullPointerException e) {
                // logger.trace("No navigationHelper available");
            }
        }

        anchor = structElement.isAnchor();
        numVolumes = structElement.getNumVolumes();
        docStructType = structElement.getDocStructType();
        dataRepository = structElement.getMetadataValue(SolrConstants.DATAREPOSITORY);

        if (DocType.GROUP.equals(docType)) {
            label = docType.getLabel(null);
        } else {
            StringBuilder sbLabel = new StringBuilder(generateLabel(structElement, locale));
            String subtitle = structElement.getMetadataValue(SolrConstants.SUBTITLE);
            if (StringUtils.isNotEmpty(subtitle)) {
                sbLabel.append(" : ").append(subtitle);
            }
            label = Helper.intern(sbLabel.toString());
        }

        pi = structElement.getTopStruct().getPi();
        if (pi == null) {
            logger.error("Index document {}  has no PI_TOPSTRUCT field. Please re-index.", structElement.getLuceneId());
            return;
        }
        pi = Helper.intern(pi);
        iddoc = structElement.getLuceneId();
        logId = Helper.intern(structElement.getMetadataValue(SolrConstants.LOGID));
        volumeNo = structElement.getVolumeNo();
        if (StringUtils.isEmpty(volumeNo)) {
            volumeNo = structElement.getVolumeNoSort();
        }

        // generate thumbnail url
        String filename = structElement.getMetadataValue(SolrConstants.FILENAME);
        if (StringUtils.isEmpty(filename)) {
            filename = structElement.getMetadataValue(SolrConstants.THUMBNAIL);
        }
        if (StringUtils.isEmpty(filename)) {
            filename = structElement.getFirstPageFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED);
        }
        //        try {
        if (anchor) {
            mimeType = structElement.getFirstVolumeFieldValue(SolrConstants.MIMETYPE);
        } else {
            mimeType = structElement.getMetadataValue(SolrConstants.MIMETYPE);
        }
        if (mimeType == null && filename != null) {
            mimeType = getMimeTypeFromExtension(filename);
        }
        if (mimeType == null) {
            mimeType = "";
        }
        //        } catch (NullPointerException | IndexOutOfBoundsException e) {
        //            //no children
        //        }

        String imageNoStr = structElement.getMetadataValue(SolrConstants.ORDER);
        if (StringUtils.isNotEmpty(imageNoStr)) {
            // ORDER field exists (page doc)
            try {
                imageNo = Integer.parseInt(imageNoStr);
            } catch (NumberFormatException e) {
                logger.debug("No valid image number found for IDDOC {}, make a 1 here", iddoc);
                imageNo = 0;
            }
        } else {
            // Use FILENAME (page) or THUMBPAGENO (docstruct doc)
            imageNoStr = structElement.getMetadataValue(SolrConstants.FILENAME);
            if (StringUtils.isNotEmpty(imageNoStr)) {
                imageNoStr = imageNoStr.substring(0, imageNoStr.indexOf('.'));
            } else {
                imageNoStr = structElement.getMetadataValue(SolrConstants.THUMBPAGENO);
            }
            if (StringUtils.isNotBlank(imageNoStr)) {
                try {
                    imageNo = Integer.parseInt(imageNoStr);
                } catch (NumberFormatException e) {
                    logger.debug("No valid image number found for IDDOC {}, make a 1 here", iddoc);
                    imageNo = 0;
                }
            } else {
                imageNo = 1;
            }
        }

        // Thumbnail
        String sbThumbnailUrl = thumbs.getThumbnailUrl(structElement);
        if (sbThumbnailUrl != null && sbThumbnailUrl.length() > 0) {
            thumbnailUrl = Helper.intern(sbThumbnailUrl.toString());
        }

        //check if we have images
        hasImages = !isAnchor() && this.mimeType.startsWith("image");

        //..or if we have video or audio
        hasMedia = !hasImages && !isAnchor()
                && (this.mimeType.startsWith("audio") || this.mimeType.startsWith("video") || this.mimeType.startsWith("text")/*sandboxed*/);

        // Only topstructs should be openened with their overview page view (if they have one)
        //        if ((structElement.isWork() || structElement.isAnchor()) && OverviewPage.loadOverviewPage(structElement, locale) != null) {
        //            useOverviewPage = true;
        //        }

        //record languages
        this.recordLanguages = structElement.getMetadataValues(SolrConstants.LANGUAGE);

        this.url = generateUrl();
        sidebarPrevUrl = generateSidebarUrl("prevHit");
        sidebarNextUrl = generateSidebarUrl("nextHit");

        Collections.reverse(structElements);
    }

    /**
     * Adds metadata fields that aren't configured in <code>metadataList</code> but match give search terms. Applies highlighting to matched terms.
     * 
     * @param structElement
     * @param searchTerms
     * @param ignoreFields Fields to be skipped
     * @param translateFields Fields to be translated
     * @should add metadata fields that match search terms
     * @should not add duplicates from default terms
     * @should not add duplicates from explicit terms
     * @should not add ignored fields
     * @should translate configured field values correctly
     */
    void addAdditionalMetadataContainingSearchTerms(StructElement structElement, Map<String, Set<String>> searchTerms, Set<String> ignoreFields,
            Set<String> translateFields) {
        // logger.trace("addAdditionalMetadataContainingSearchTerms");
        if (searchTerms == null) {
            return;
        }
        boolean overviewPageFetched = false;
        for (String termsFieldName : searchTerms.keySet()) {
            // Skip fields that are in the ignore list
            if (ignoreFields != null && ignoreFields.contains(termsFieldName)) {
                continue;
            }
            // Skip fields that are already in the list
            boolean skip = false;
            for (Metadata md : metadataList) {
                if (md.getLabel().equals(termsFieldName)) {
                    continue;
                }
            }
            if (skip) {
                continue;
            }
            switch (termsFieldName) {
                case SolrConstants.DEFAULT:
                    // If searching in DEFAULT, add all fields that contain any of the terms (instead of DEFAULT)
                    for (String docFieldName : structElement.getMetadataFields().keySet()) {
                        // Skip fields that are in the ignore list
                        if (ignoreFields != null && ignoreFields.contains(docFieldName)) {
                            continue;
                        }
                        if (!docFieldName.startsWith("MD_") || docFieldName.endsWith(SolrConstants._UNTOKENIZED)) {
                            continue;
                        }
                        // Skip fields that are already in the list
                        for (Metadata md : metadataList) {
                            if (md.getLabel().equals(docFieldName)) {
                                skip = true;
                                break;
                            }
                        }
                        if (skip) {
                            skip = false;
                            continue;
                        }
                        List<String> fieldValues = structElement.getMetadataFields().get(docFieldName);
                        for (String fieldValue : fieldValues) {
                            // Skip values that are equal to the hit label
                            if (fieldValue.equals(label)) {
                                continue;
                            }
                            String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                            if (!highlightedValue.equals(fieldValue)) {
                                // Translate values for certain fields
                                if (translateFields != null && translateFields.contains(docFieldName)) {
                                    String translatedValue = Helper.getTranslation(fieldValue, locale);
                                    // highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                    // "$1" + translatedValue + "$3");
                                    highlightedValue = SearchHelper.applyHighlightingToPhrase(translatedValue, searchTerms.get(termsFieldName));
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                metadataList.add(new Metadata(docFieldName, "", highlightedValue));
                                additionalMetadataList.add(new Metadata(docFieldName, "", highlightedValue));
                            }
                        }
                    }
                    break;
                default:
                    // Skip fields that are already in the list
                    for (Metadata md : metadataList) {
                        if (md.getLabel().equals(termsFieldName)) {
                            skip = true;
                            break;
                        }
                    }
                    // Look up the exact field name in the Solr doc and add its values that contain any of the terms for that field
                    if (!skip && structElement.getMetadataFields().containsKey(termsFieldName)) {
                        List<String> fieldValues = structElement.getMetadataFields().get(termsFieldName);
                        for (String fieldValue : fieldValues) {
                            String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                            if (!highlightedValue.equals(fieldValue)) {
                                // Translate values for certain fields
                                if (translateFields != null && translateFields.contains(termsFieldName)) {
                                    String translatedValue = Helper.getTranslation(fieldValue, locale);
                                    // highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                    // "$1" + translatedValue + "$3");
                                    highlightedValue = SearchHelper.applyHighlightingToPhrase(translatedValue, searchTerms.get(termsFieldName));
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                metadataList.add(new Metadata(termsFieldName, "", highlightedValue));
                                additionalMetadataList.add(new Metadata(termsFieldName, "", highlightedValue));
                            }
                        }
                    }
                    break;
            }
        }
    }

    /**
     * @param filename
     * @return
     */
    private static String getMimeTypeFromExtension(String filename) {
        try {
            URL fileUrl = new URL(filename);
            return ImageFileFormat.getImageFileFormatFromFileExtension(fileUrl.getPath()).getMimeType();
        } catch (MalformedURLException e) {
        }
        return "";
    }

    /**
     * 
     * @param se
     * @param locale
     * @return
     */
    private String generateLabel(StructElement se, Locale locale) {
        String ret = "";

        if (docType != null) {
            switch (docType) {
                case METADATA:
                    // Grouped metadata
                    if (metadataGroupType != null) {
                        switch (metadataGroupType) {
                            case PERSON:
                            case CORPORATION:
                            case LOCATION:
                            case SUBJECT:
                            case PUBLISHER:
                                if (se.getMetadataValue("NORM_NAME") != null) {
                                    ret = se.getMetadataValue("NORM_NAME");
                                } else {
                                    ret = se.getMetadataValue("MD_VALUE");
                                }
                                if (ret == null) {
                                    ret = se.getMetadataValue(SolrConstants.LABEL);
                                }
                                break;
                            default:
                                ret = se.getMetadataValue(SolrConstants.LABEL);
                                break;
                        }
                    } else {
                        ret = se.getMetadataValue(SolrConstants.LABEL);
                    }
                    ret = Helper.getTranslation(ret, locale);
                    break;
                case EVENT:
                    // Try to use the event name or type (optionally with dates), otherwise use LABEL
                    ret = se.getMetadataValue("MD_EVENTNAME");
                    if (StringUtils.isEmpty(ret)) {
                        ret = se.getMetadataValue(SolrConstants.EVENTTYPE);
                    }
                    if (StringUtils.isNotEmpty(ret)) {
                        String eventDate = se.getMetadataValue(SolrConstants.EVENTDATE);
                        String eventDateStart = se.getMetadataValue(SolrConstants.EVENTDATESTART);
                        String eventDateEnd = se.getMetadataValue(SolrConstants.EVENTDATESTART);
                        if (StringUtils.isNotEmpty(eventDateStart) && StringUtils.isNotEmpty(eventDateEnd) && !eventDateStart.equals(eventDateEnd)) {
                            ret += " (" + eventDateStart + " - " + eventDateEnd + ")";
                        } else if (StringUtils.isNotEmpty(eventDate)) {
                            ret += " (" + eventDate + ")";
                        }
                    } else {
                        ret = se.getMetadataValue(SolrConstants.LABEL);
                    }
                    ret = Helper.getTranslation(ret, locale);
                    break;
                case UGC:
                    // User-generated content
                    ret = DisplayUserGeneratedContent.generateUgcLabel(se);
                    ret = Helper.getTranslation(ret, locale);
                    break;
                default:
                    ret = generateDefaultLabel(se, locale);
                    break;
            }
        } else {
            logger.warn("{} field seems to be missing on Solr document {}", SolrConstants.DOCTYPE, se.getLuceneId());
            ret = generateDefaultLabel(se, locale);
        }

        if (ret == null) {
            ret = "";
            logger.error("Index document {}, has no LABEL, MD_TITLE or DOCSTRUCT fields. Perhaps there is no connection to the owner doc?",
                    se.getLuceneId());
        }

        return ret;
    }

    /**
     * 
     * @param se
     * @param locale
     * @return
     * @should translate docstruct label
     */
    static String generateDefaultLabel(StructElement se, Locale locale) {
        String ret = null;
        if (locale != null) {
            // Prefer localized title
            String englishTitle = null;
            String germanTitle = null;
            String anyTitle = null;
            for (String key : se.getMetadataFields().keySet()) {
                if (key.equals(SolrConstants.TITLE + "_LANG_" + locale.getLanguage().toUpperCase())) {
                    ret = se.getMetadataValue(key);
                    break;
                } else if (key.equals(SolrConstants.TITLE + "_LANG_DE")) {
                    germanTitle = se.getMetadataValue(key);
                } else if (key.equals(SolrConstants.TITLE + "_LANG_EN")) {
                    englishTitle = se.getMetadataValue(key);
                } else if (key.matches(SolrConstants.TITLE + "_LANG_[A-Z][A-Z]")) {
                    anyTitle = se.getMetadataValue(key);
                }
            }
            if (StringUtils.isBlank(ret)) {
                if (StringUtils.isNotBlank(englishTitle)) {
                    ret = englishTitle;
                } else if (StringUtils.isNotBlank(germanTitle)) {
                    ret = germanTitle;
                } else {
                    ret = anyTitle;
                }
            }
        }
        // Fallback to LABEL or TITLE
        if (StringUtils.isEmpty(ret)) {
            ret = se.getMetadataValue(SolrConstants.LABEL);
            if (StringUtils.isEmpty(ret)) {
                ret = se.getMetadataValue(SolrConstants.TITLE);
            }
        }
        if (StringUtils.isEmpty(ret)) {
            ret = Helper.getTranslation(se.getDocStructType(), locale);
        }

        return ret;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the labelShort
     */
    public String getLabelShort() {
        return labelShort;
    }

    /**
     * @param labelShort the labelShort to set
     */
    public void setLabelShort(String labelShort) {
        this.labelShort = labelShort;
    }

    /**
     * @return the type
     */
    public String getDocStructType() {
        return docStructType;
    }

    /**
     * @return the iddoc
     */
    public long getIddoc() {
        return iddoc;
    }

    /**
     * @return the thumbnailUrl
     */
    public String getThumbnailUrl() {
        //        logger.trace("thumbnailUrl {}", thumbnailUrl);
        return thumbnailUrl;
    }

    /**
     * Called from HTML.
     *
     * @param width
     * @param height
     * @return
     */
    public String getThumbnailUrl(String width, String height) {
        synchronized (this) {
            String url = getThumbnailUrl();
            String urlNew = new IIIFUrlHandler().getModifiedIIIFFUrl(url, null,
                    new Scale.ScaleToBox(Integer.parseInt(width), Integer.parseInt(height)), null, null, null);
            return urlNew;
        }
    }

    public int getImageNo() {
        return imageNo;
    }

    /**
     * @param structElements the structElements to set
     */
    public void setStructElements(List<StructElementStub> structElements) {
        this.structElements = structElements;
    }

    /**
     * @return the structElements
     */
    public List<StructElementStub> getStructElements() {
        return structElements;
    }

    /**
     * @param fulltext the fulltext to set
     */
    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    /**
     * @return the fulltext
     */
    public String getFulltext() {
        return fulltext;
    }

    /**
     * Returns a relevant full-text fragment for displaying in the search hit box, stripped of any contained JavaScript.
     *
     * @return Full-text fragment sans any line breaks or JavaScript
     * @should remove any line breaks
     * @should remove any JS
     */
    public String getFulltextForHtml() {
        if (fulltextForHtml == null) {
            if (fulltext != null) {
                fulltextForHtml = StringTools.stripJS(fulltext).replaceAll("\n", " ");
            } else {
                fulltextForHtml = "";
            }
        }

        return fulltextForHtml;
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
     * @return the hasImages
     */
    public boolean isHasImages() {
        return hasImages;
    }

    /**
     * @param hasImages the hasImages to set
     */
    public void setHasImages(boolean hasImages) {
        this.hasImages = hasImages;
    }

    /**
     * @return the hasOverviewPage
     */
    public boolean isHasOverviewPage() {
        return useOverviewPage;
    }

    /**
     * @return the numVolumes
     */
    public long getNumVolumes() {
        return numVolumes;
    }

    /**
     * @param pi the identifier to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the identifier
     */
    public String getPi() {
        return pi;
    }

    /**
     * Returns the search hint URL (without the application root!).
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the sidebarPrevUrl
     */
    public String getSidebarPrevUrl() {
        return sidebarPrevUrl;
    }

    /**
     * @return the sidebarNextUrl
     */
    public String getSidebarNextUrl() {
        return sidebarNextUrl;
    }

    /**
     *
     * @return
     */
    private String generateUrl() {
        // For aggregated person search hits, start another search (label contains the person's name in this case)
        StringBuilder sb = new StringBuilder();
        if (metadataGroupType != null) {
            switch (metadataGroupType) {
                case PERSON:
                case CORPORATION:
                case LOCATION:
                case SUBJECT:
                case PUBLISHER:
                    // Person metadata search hit ==> execute search for that person
                    // TODO not for aggregated hits?
                    try {
                        sb.append(PageType.search.getName())
                                .append("/-/")
                                .append(originalFieldName)
                                .append(":\"")
                                .append(URLEncoder.encode(label, SearchBean.URL_ENCODING))
                                .append("\"/1/-/-/");
                    } catch (UnsupportedEncodingException e) {
                        logger.error("{}: {}", e.getMessage(), label);
                        sb = new StringBuilder();
                        sb.append('/').append(PageType.search.getName()).append("/-/").append(originalFieldName).append(":\"").append(label).append(
                                "\"/1/-/-/");
                    }

                    break;
                default:
                    PageType pageType = PageType.determinePageType(docStructType, mimeType, anchor || DocType.GROUP.equals(docType),
                            hasImages || hasMedia, useOverviewPage, false);
                    // Hack for linking TEI full-text hits to the full-text page
                    if ("TEI".equals(label)) {
                        pageType = PageType.viewFulltext;
                    }
                    sb.append(pageType.getName()).append('/').append(pi).append('/').append(imageNo).append('/');
                    // Hack for viewers that need a language parameter instead of LOGID
                    String theme = DataManager.getInstance().getConfiguration().getTheme();
                    if (theme != null) {
                        switch (theme) {
                            case "geiwv":
                            case "wienerlibrary-novemberpogrom":
                                sb.append(DataManager.getInstance().getLanguageHelper().getLanguage(BeanUtils.getLocale().getLanguage()).getIsoCode())
                                        .append("/");
                                break;
                            default:
                                sb.append(StringUtils.isNotEmpty(logId) ? logId : '-').append('/');
                        }
                    }
                    break;
            }
        } else {
            PageType pageType = PageType.determinePageType(docStructType, mimeType, anchor || DocType.GROUP.equals(docType), hasImages || hasMedia,
                    useOverviewPage, false);
            if (DocType.UGC.equals(docType)) {
                pageType = PageType.viewObject;
            } else if ("TEI".equals(label)) {
                // Hack for linking TEI full-text hits to the full-text page
                pageType = PageType.viewFulltext;
            }
            sb.append(pageType.getName()).append('/').append(pi).append('/').append(imageNo).append('/');
            // Hack for viewers that need a language parameter instead of LOGID
            String theme = DataManager.getInstance().getConfiguration().getTheme();
            if (theme != null) {
                switch (theme) {
                    case "geiwv":
                    case "wienerlibrary-novemberpogrom":
                        sb.append(DataManager.getInstance().getLanguageHelper().getLanguage(BeanUtils.getLocale().getLanguage()).getIsoCode())
                                .append("/");
                        break;
                    default:
                        sb.append(StringUtils.isNotEmpty(logId) ? logId : '-').append('/');
                }
            }
        }

        // logger.trace("generateUrl: {}", sb.toString());
        return sb.toString();

    }

    /**
     * Important: hits have to have 3 Pretty parameters (e.g. /image/nextHit/PPN123/1/)
     *
     * @param type
     * @return
     */
    private String generateSidebarUrl(String type) {
        PageType configuredPageType = PageType.getPageTypeForDocStructType(docStructType);

        StringBuilder sb = new StringBuilder();
        if (anchor) {
            if (navigationHelper != null && PageType.viewMetadata.getName().equals(navigationHelper.getCurrentView())) {
                // Use the preferred view, if set and allowed for multivolumes
                String view = StringUtils.isNotEmpty(navigationHelper.getPreferredView()) ? navigationHelper.getPreferredView()
                        : PageType.viewToc.getName();
                if (!view.equals(PageType.viewToc.getName()) && !view.equals(PageType.viewMetadata.getName())) {
                    view = PageType.viewToc.getName();
                }
                sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                        .append('/')
                        .append(view)
                        .append('/')
                        .append(type)
                        .append('/')
                        .append(pi)
                        .append('/')
                        .append(imageNo)
                        .append('/')
                        .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                        .append('/');
            } else {
                sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                        .append('/')
                        .append(PageType.viewToc.getName())
                        .append('/')
                        .append(type)
                        .append('/')
                        .append(pi)
                        .append('/')
                        .append(imageNo)
                        .append('/')
                        .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                        .append('/');
            }
        } else if (navigationHelper != null && StringUtils.isNotEmpty(navigationHelper.getPreferredView())) {
            // Use the preferred view, if set
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(navigationHelper.getPreferredView())
                    .append('/')
                    .append(type)
                    .append('/')
                    .append(pi)
                    .append('/')
                    .append(imageNo)
                    .append('/')
                    .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        } else if (configuredPageType != null) {
            // logger.trace("Found configured page type: {}", configuredPageType.getName());
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(configuredPageType.getName())
                    .append('/')
                    .append(type)
                    .append('/')
                    .append(pi)
                    .append('/')
                    .append(imageNo)
                    .append('/')
                    .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        } else if (hasImages || hasMedia) {
            // Regular image view
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(PageType.viewImage.getName())
                    .append('/')
                    .append(type)
                    .append('/')
                    .append(pi)
                    .append('/')
                    .append(imageNo)
                    .append('/')
                    .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        } else {
            // Metadata view for elements without a thumbnail
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(PageType.viewMetadata.getName())
                    .append('/')
                    .append(type)
                    .append('/')
                    .append(pi)
                    .append('/')
                    .append(imageNo)
                    .append('/')
                    .append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        }

        return sb.toString();
    }

    public List<Metadata> getMetadataList() {
        return metadataList;
    }

    public void setMetadataList(List<Metadata> metadataList) {
        this.metadataList = metadataList;
    }

    /**
     * @return the thumbnailAccessDenied
     */
    public boolean isThumbnailAccessDenied() {
        return thumbnailAccessDenied;
    }

    /**
     * @return the metadataGroupType
     */
    public MetadataGroupType getMetadataGroupType() {
        return metadataGroupType;
    }

    /**
     * @param metadataName
     * @return
     */
    public List<Metadata> getMetadataList(String metadataLabel) {
        List<Metadata> list = new ArrayList<>();
        for (Metadata metadata : getMetadataList()) {
            if (metadata.getLabel().equals(metadataLabel)) {
                list.add(metadata);
            }
        }
        return list;
    }

    /**
     * @return the additionalMetadataList
     */
    public List<Metadata> getAdditionalMetadataList() {
        return additionalMetadataList;
    }

    /**
     * @return the dataRepository
     */
    public String getDataRepository() {
        return dataRepository;
    }

    /**
     * Returns the ContextObject value for a COinS element using the docstruct hierarchy for this search hit..
     *
     * @return
     */
    public String getContextObject() {
        if (contextObject == null && !structElements.isEmpty()) {
            StructElementStub topStruct = structElements.get(structElements.size() - 1);
            if (topStruct.isAnchor() && structElements.size() > 1) {
                topStruct = structElements.get(structElements.size() - 2);
            }
            try {
                contextObject = structElements.get(0).generateContextObject(getUrl(), topStruct);
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return contextObject;
    }

    /**
     * @return the recordLanguages
     */
    public List<String> getRecordLanguages() {
        return recordLanguages;
    }

    /**
     * @param hasMedia the hasMedia to set
     */
    public void setHasMedia(boolean hasMedia) {
        this.hasMedia = hasMedia;
    }

    /**
     * @return the hasMedia
     */
    public boolean isHasMedia() {
        return hasMedia;
    }

}
