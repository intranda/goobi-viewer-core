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
package io.goobi.viewer.model.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.IIIFUrlHandler;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.metadata.MetadataValue;
import io.goobi.viewer.model.security.AccessDeniedInfoConfig;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IAccessDeniedThumbnailOutput;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.EventElement;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.StructElementStub;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;

/**
 * Representation of a search hit.
 */
public class BrowseElement implements IAccessDeniedThumbnailOutput, Serializable {

    private static final long serialVersionUID = 6621169815560734613L;

    private static final Logger logger = LogManager.getLogger(BrowseElement.class);

    @JsonIgnore
    private String fulltext;
    private String fulltextForHtml;
    /** Element label (usually the title). */
    private final IMetadataValue label;
    /** Truncated and highlighted variant of the label. */
    private IMetadataValue labelShort = new SimpleMetadataValue();
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
    private String iddoc;
    private String thumbnailUrl;
    private int imageNo;
    @JsonIgnore
    private String volumeNo = null;
    /** StructElementStubs for hierarchy URLs. */
    @JsonIgnore
    private final List<StructElementStub> structElements = new ArrayList<>();
    @JsonIgnore
    private List<EventElement> events;
    @JsonIgnore
    private boolean work = false;
    @JsonIgnore
    private boolean anchor = false;
    @JsonIgnore
    private boolean cmsPage = false;
    @JsonIgnore
    private boolean hasImages = false;
    @JsonIgnore
    private boolean hasMedia = false;
    @JsonIgnore
    private boolean hasTeiFiles = false;
    @JsonIgnore
    private boolean showThumbnail = false;
    @JsonIgnore
    private long numVolumes = 0;
    private String pi;
    private String logId;
    @JsonIgnore
    private NavigationHelper navigationHelper;
    /** Map containing metadata lists for this search hit. */
    @JsonIgnore
    private Map<String, List<Metadata>> metadataListMap = Collections.emptyMap();
    @JsonIgnore
    private final Set<String> existingMetadataFields = new HashSet<>();
    /** List metadata fields that are not explicitly configured for display, but contain search terms. */
    @JsonIgnore
    private final List<Metadata> foundMetadataList = new ArrayList<>();
    @JsonIgnore
    private String mimeType = "";
    @JsonIgnore
    private String contextObject;
    private String url;
    @JsonIgnore
    private String risExport;
    @JsonIgnore
    private String sidebarPrevUrl;
    @JsonIgnore
    private String sidebarNextUrl;
    @JsonIgnore
    private final Locale locale;
    @JsonIgnore
    private final String dataRepository;
    @JsonIgnore
    private AccessPermission accessPermissionThumbnail = null;

    private List<String> recordLanguages;

    /**
     * Constructor for unit tests and special instances.
     *
     * @param pi
     * @param imageNo
     * @param label
     * @param fulltext
     * @param locale
     * @param dataRepository
     * @param url Injected URL, overrides URL generation
     *
     * @should build overview page url correctly
     */
    public BrowseElement(String pi, int imageNo, String label, String fulltext, Locale locale, String dataRepository, String url) {
        this.pi = pi;
        this.imageNo = imageNo;
        this.label = new SimpleMetadataValue(label);
        this.fulltext = fulltext;
        this.locale = locale;
        this.metadataListMap = HashMap.newHashMap(1);
        this.metadataListMap.put(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>());
        this.url = url;
        if (this.url == null) {
            this.url = generateUrl();
        }
        this.dataRepository = dataRepository;
    }

    /**
     * Constructor.
     *
     * @param structElement {@link StructElement}
     * @param metadataListMap
     * @param locale
     * @param fulltext
     * @param searchTerms
     * @param thumbs
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    BrowseElement(StructElement structElement, Map<String, List<Metadata>> metadataListMap, Locale locale, String fulltext,
            Map<String, Set<String>> searchTerms, ThumbnailHandler thumbs) throws PresentationException, IndexUnreachableException, DAOException {
        this(structElement, metadataListMap, locale, fulltext, searchTerms, thumbs, null);
    }

    /**
     * Constructor.
     *
     * @param structElement {@link StructElement}
     * @param metadataListMap
     * @param locale
     * @param fulltext
     * @param searchTerms
     * @param thumbs
     * @param user The user for whom the thumbnail accessCondition is calculated. If null, it is fetched from the jsfContext if one exists
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    BrowseElement(StructElement structElement, Map<String, List<Metadata>> metadataListMap, Locale locale, String fulltext,
            Map<String, Set<String>> searchTerms, ThumbnailHandler thumbs, User user)
            throws PresentationException, IndexUnreachableException, DAOException {
        if (structElement == null) {
            throw new IllegalArgumentException("structElement may not be null");
        }

        this.metadataListMap = metadataListMap;
        if (this.metadataListMap == null || this.metadataListMap.isEmpty()) {
            this.metadataListMap = HashMap.newHashMap(1);
            this.metadataListMap.put(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>());
        }
        this.locale = locale;
        this.fulltext = fulltext;

        // Collect the docstruct hierarchy
        StructElement anchorStructElement = null;
        StructElement topStructElement = null; // this can be null in unit tests
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

        // Determine Solr document type. Must happen before certain things, such as label generation.
        docType = DocType.getByName(structElement.getMetadataValue(SolrConstants.DOCTYPE));
        if (DocType.METADATA.equals(docType)) {
            metadataGroupType = MetadataGroupType.getByName(structElement.getMetadataValue(SolrConstants.METADATATYPE));
            // The LABEL field in grouped metadata docs contains the name of the field defined in the indexed configuration
            originalFieldName = structElement.getMetadataValue(SolrConstants.LABEL);
        }

        // If the topstruct is a volume of any kind or a subelement, add the anchor and volume labels to
        if (!structElement.isAnchor() && topStructElement != null) {
            // Add anchor label to volumes
            anchorStructElement = topStructElement.getParent();
            if (anchorStructElement != null) {
                // Add anchor to the docstruct hierarchy
                structElements.add(anchorStructElement.createStub());
            }
        }

        // Populate metadata
        int length = DataManager.getInstance().getConfiguration().getSearchHitMetadataValueLength();
        // int number = DataManager.getInstance().getConfiguration().getSearchHitMetadataValueNumber();
        for (Entry<String, List<Metadata>> entry : this.metadataListMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                // logger.trace("populating metadata list {}", entry.getKey()); //NOSONAR Debug
                populateMetadataList(entry.getValue(), structElement, anchorStructElement, searchTerms, length, locale);
            }
        }

        // Add event metadata for LIDO records
        if (topStructElement != null && topStructElement.isLidoRecord()) {
            populateEvents(topStructElement, searchTerms);
        }

        if (DataManager.getInstance().getConfiguration().isSearchRisExportEnabled()) {
            risExport = MetadataTools.generateRIS(structElement);
        }

        if (navigationHelper == null) {
            try {
                navigationHelper = BeanUtils.getNavigationHelper();
            } catch (NullPointerException e) {
                // logger.trace("No navigationHelper available"); //NOSONAR Debug
            }
        }

        work = structElement.isWork();
        anchor = structElement.isAnchor();
        cmsPage = structElement.isCmsPage();
        numVolumes = structElement.getNumVolumes();
        docStructType = structElement.getDocStructType();
        dataRepository = structElement.getMetadataValue(SolrConstants.DATAREPOSITORY);
        label = createMultiLanguageLabel(structElement);

        pi = structElement.getPi();
        if (pi == null) {
            logger.warn("Index document {} has no PI_TOPSTRUCT field. Please re-index.", structElement.getLuceneId());
            return;
        }
        pi = StringTools.intern(pi);
        iddoc = structElement.getLuceneId();
        logId = StringTools.intern(structElement.getMetadataValue(SolrConstants.LOGID));
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
        if (thumbs != null) {
            String sbThumbnailUrl = thumbs.getThumbnailUrl(structElement);
            if (sbThumbnailUrl != null && !sbThumbnailUrl.isEmpty()) {
                thumbnailUrl = StringTools.intern(sbThumbnailUrl);
            }

            // Check thumbnail access so that a custom access denied image can be used
            String thumbnailPi = pi;
            if (isAnchor() && StringConstants.ANCHOR_THUMBNAIL_MODE_FIRSTVOLUME
                    .equals(DataManager.getInstance().getConfiguration().getAnchorThumbnailMode())) {
                StructElement firstVolume = structElement.getFirstVolume(new ArrayList<>(ThumbnailHandler.REQUIRED_SOLR_FIELDS));
                if (firstVolume != null) {
                    thumbnailPi = firstVolume.getPi();
                    logger.trace("Using first volume for thumbnail: {}", thumbnailPi);
                    String thumbPageNo = firstVolume.getMetadataValue(SolrConstants.THUMBPAGENO);
                    imageNo = StringUtils.isNotBlank(thumbPageNo) ? Integer.parseInt(thumbPageNo) : 1;
                }
            }
            PhysicalElement pe = ThumbnailHandler.getPage(thumbnailPi, imageNo);
            if (pe != null) {
                accessPermissionThumbnail = pe.getAccessPermission(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS, user);
            }
        }

        BaseMimeType baseMimeType = BaseMimeType.getByName(this.mimeType);
        //check if we have images
        hasImages = !isAnchor() && (BaseMimeType.IMAGE.equals(baseMimeType) || structElement.isHasImages());
        //..or if we have video or audio or a 3d-object
        hasMedia = !hasImages && !isAnchor() && baseMimeType.isMediaType();

        showThumbnail = hasImages || hasMedia || isAnchor() || cmsPage;

        // TEI files
        hasTeiFiles = structElement.getMetadataFields().keySet().stream().filter(k -> k.startsWith(SolrConstants.FILENAME_TEI)).count() > 0;

        //record languages
        this.recordLanguages = structElement.getMetadataValues(SolrConstants.LANGUAGE);

        this.url = generateUrl();
        sidebarPrevUrl = generateSidebarUrl("prevHit");
        sidebarNextUrl = generateSidebarUrl("nextHit");

        Collections.reverse(structElements);
    }

    /**
     * 
     * @param metadataList
     * @param structElement
     * @param anchorStructElement
     * @param searchTerms
     * @param length
     * @param locale
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    void populateMetadataList(List<Metadata> metadataList, StructElement structElement, StructElement anchorStructElement,
            Map<String, Set<String>> searchTerms, int length, Locale locale) throws IndexUnreachableException, PresentationException {
        if (metadataList == null) {
            throw new IllegalArgumentException("metadataList may not be null");
        }

        for (Metadata md : metadataList) {
            md.populate(structElement, anchorStructElement, String.valueOf(structElement.getLuceneId()), md.getSortFields(), searchTerms, length,
                    locale);
        }
    }

    /**
     * Looks up LIDO events and search hit metadata for the given record topstruct element. Applies search hit value highlighting, if search terms are
     * provided.
     *
     * @param topStructElement Top structure element of the LIDO record
     * @param searchTerms Map containing all generated search terms
     * @throws IndexUnreachableException
     */
    private void populateEvents(StructElement topStructElement, Map<String, Set<String>> searchTerms) throws IndexUnreachableException {
        if (topStructElement == null || !topStructElement.isLidoRecord()) {
            return;
        }
        logger.trace("populateEvents: {}, {}", topStructElement.getLabel(), searchTerms);

        this.events = topStructElement.generateEventElements(locale, true);
        if (this.events.isEmpty()) {
            return;
        }

        Collections.sort(this.events);

        // Value highlighting
        if (searchTerms == null) {
            return;
        }
        for (EventElement event : events) {
            for (Metadata md : event.getSearchHitMetadata()) {
                for (MetadataParameter param : md.getParams()) {
                    for (MetadataValue mdValue : md.getValues()) {
                        if (searchTerms.get(md.getLabel()) != null) {
                            mdValue.applyHighlightingToParamValue(md.getParams().indexOf(param), searchTerms.get(md.getLabel()));
                        } else if (searchTerms.get(SolrConstants.DEFAULT) != null) {
                            mdValue.applyHighlightingToParamValue(md.getParams().indexOf(param), searchTerms.get(SolrConstants.DEFAULT));
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * createMultiLanguageLabel.
     * </p>
     *
     * @param structElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     */
    public IMetadataValue createMultiLanguageLabel(StructElement structElement) {
        MultiLanguageMetadataValue value = new MultiLanguageMetadataValue();
        List<Locale> usedLocales = Optional.ofNullable(ViewerResourceBundle.getAllLocales())
                .filter(l -> !l.isEmpty())
                .orElse(Collections.singletonList(Locale.ENGLISH));
        for (Locale loc : usedLocales) {
            // logger.trace("Used locale: {}", loc.getLanguage()); //NOSONAR Debug
            StringBuilder sbLabel = new StringBuilder(generateLabel(structElement, loc));
            String subtitle = structElement.getMetadataValueForLanguage(SolrConstants.SUBTITLE, loc.getLanguage());
            if (StringUtils.isNotEmpty(subtitle)) {
                sbLabel.append(" : ").append(subtitle);
            }
            value.setValue(sbLabel.toString(), loc);
        }

        return value;
    }

    /**
     *
     * @param structElement
     * @param sortFields If manual sorting was used, display the sorting fields
     * @param ignoreFields Fields to be skipped
     * @should add sort fields correctly
     * @should not add fields on ignore list
     * @should not add fields already in the list
     */
    void addSortFieldsToMetadata(StructElement structElement, List<StringPair> sortFields, Set<String> ignoreFields) {
        if (sortFields == null || sortFields.isEmpty()) {
            return;
        }

        for (StringPair sortField : sortFields) {
            // Skip fields that are in the ignore list
            if (ignoreFields != null && ignoreFields.contains(sortField.getOne())) {
                continue;
            }
            // Title is already in the header
            if ("SORT_TITLE".equals(sortField.getOne())) {
                continue;
            }
            // Skip fields that are already in the list
            boolean skip = false;

            for (Entry<String, List<Metadata>> entry : this.metadataListMap.entrySet()) {
                for (Metadata md : entry.getValue()) {
                    if (md.getLabel().equals(sortField.getOne().replace("SORT_", "MD_"))) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }
                // Look up the exact field name in the Solr doc and add its values that contain any of the terms for that field
                if (!skip && structElement.getMetadataFields().containsKey(sortField.getOne())) {
                    List<String> fieldValues = structElement.getMetadataFields().get(sortField.getOne());
                    for (String fieldValue : fieldValues) {
                        MetadataParameterType type;
                        switch (sortField.getOne()) {
                            case SolrConstants.DATECREATED:
                            case SolrConstants.DATEINDEXED:
                            case SolrConstants.DATEUPDATED:
                                type = MetadataParameterType.MILLISFIELD;
                                break;
                            default:
                                type = MetadataParameterType.FIELD;
                                break;
                        }

                        Metadata md = new Metadata(String.valueOf(structElement.getLuceneId()), sortField.getOne(), "",
                                new MetadataParameter().setType(type), fieldValue, locale);

                        entry.getValue().add(md);
                        foundMetadataList.add(md);
                    }
                }
            }
        }
    }

    /**
     * @param filename
     * @return Mime type for the given filename
     * @should return empty string for unknown file extensions
     */
    static String getMimeTypeFromExtension(String filename) {
        ImageFileFormat format = ImageFileFormat.getImageFileFormatFromFileExtension(filename);
        if (format != null) {
            return format.getMimeType();
        }

        return "";
    }

    /**
     *
     * @param se
     * @param locale
     * @return Generated label
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
                            case ORIGININFO:
                            case OTHER:
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
                    ret = ViewerResourceBundle.getTranslation(ret, locale);
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
                    ret = ViewerResourceBundle.getTranslation(ret, locale);
                    break;
                case UGC:
                    // User-generated content
                    ret = DisplayUserGeneratedContent.generateUgcLabel(se);
                    ret = ViewerResourceBundle.getTranslation(ret, locale);
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
     * @return Generated label
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
                if (key.equals(SolrConstants.TITLE + SolrConstants.MIDFIX_LANG + locale.getLanguage().toUpperCase())) {
                    ret = se.getMetadataValue(key);
                    break;
                } else if (key.equals(SolrConstants.TITLE + SolrConstants.MIDFIX_LANG + "DE")) {
                    germanTitle = se.getMetadataValue(key);
                } else if (key.equals(SolrConstants.TITLE + SolrConstants.MIDFIX_LANG + "EN")) {
                    englishTitle = se.getMetadataValue(key);
                } else if (key.matches(SolrConstants.TITLE + SolrConstants.MIDFIX_LANG + "[A-Z][A-Z]")) {
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
            // Fallback to DOCSTRCT
            if (StringUtils.isEmpty(ret)) {
                ret = ViewerResourceBundle.getTranslation(se.getDocStructType(), locale);
                // Fallback to DOCTYPE
                if (StringUtils.isEmpty(ret)) {
                    ret = ViewerResourceBundle.getTranslation("doctype_" + se.getMetadataValue(SolrConstants.DOCTYPE), locale);
                }
            }
        }

        return ret;

    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    public String getLabel() {
        return label.getValue(BeanUtils.getLocale()).orElse(label.getValue().orElse(""));
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
     */
    public String getLabel(Locale locale) {
        return label.getValue(locale).orElse("");
    }

    /**
     * <p>
     * getLabelAsMetadataValue.
     * </p>
     *
     * @return a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     */
    public IMetadataValue getLabelAsMetadataValue() {
        return label;
    }

    /**
     * <p>
     * Getter for the field <code>labelShort</code>.
     * </p>
     *
     * @return the labelShort
     */
    public String getLabelShort() {
        return labelShort.getValue(BeanUtils.getLocale()).orElse(labelShort.getValue().orElse(""));
    }

    /**
     * <p>
     * Setter for the field <code>labelShort</code>.
     * </p>
     *
     * @param labelShort the labelShort to set
     */
    public void setLabelShort(IMetadataValue labelShort) {
        this.labelShort = labelShort;
    }

    /**
     * <p>
     * Getter for the field <code>docStructType</code>.
     * </p>
     *
     * @return the type
     */
    public String getDocStructType() {
        return docStructType;
    }

    /**
     * <p>
     * Getter for the field <code>iddoc</code>.
     * </p>
     *
     * @return the iddoc
     */
    public String getIddoc() {
        return iddoc;
    }

    /**
     * <p>
     * Getter for the field <code>thumbnailUrl</code>.
     * </p>
     *
     * @return the thumbnailUrl
     */
    public String getThumbnailUrl() {
        //        logger.trace("thumbnailUrl {}", thumbnailUrl); //NOSONAR Debug
        return thumbnailUrl;
    }

    /**
     * Called from HTML.
     *
     * @param width a {@link java.lang.String} object.
     * @param height a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(String width, String height) {
        synchronized (this) {
            String thumbUrl = getThumbnailUrl();
            return new IIIFUrlHandler().getModifiedIIIFFUrl(thumbUrl, null,
                    new Scale.ScaleToBox(Integer.parseInt(width), Integer.parseInt(height)), null, null, null);
        }
    }

    @Override
    public String getAccessDeniedThumbnailUrl(Locale locale) {
        logger.trace("getAccessDeniedThumbnailUrl: locale: {}, PI: {}", locale, pi);
        if (accessPermissionThumbnail != null && accessPermissionThumbnail.getAccessDeniedPlaceholderInfo() != null) {
            AccessDeniedInfoConfig placeholderInfo = accessPermissionThumbnail.getAccessDeniedPlaceholderInfo().get(locale.getLanguage());
            if (placeholderInfo != null && StringUtils.isNotEmpty(placeholderInfo.getImageUri())) {
                logger.trace("returning custom image: {}", placeholderInfo.getImageUri());
                return placeholderInfo.getImageUri();
            }
        }

        return null;
    }

    /**
     * <p>
     * Getter for the field <code>imageNo</code>.
     * </p>
     *
     * @return a int.
     */
    public int getImageNo() {
        return imageNo;
    }

    /**
     * @param imageNo the imageNo to set
     */
    public void setImageNo(int imageNo) {
        this.imageNo = imageNo;
    }

    /**
     * <p>
     * Getter for the field <code>structElements</code>.
     * </p>
     *
     * @return the structElements
     */
    public List<StructElementStub> getStructElements() {
        return structElements;
    }

    /**
     * Returns the lowest <code>StructElementStub</code> in the list.
     *
     * @return last StructElementStub in the list
     */
    public StructElementStub getBottomStructElement() {
        if (structElements.isEmpty()) {
            return null;
        }

        return structElements.get(structElements.size() - 1);
    }

    /**
     * @return the events
     */
    public List<EventElement> getEvents() {
        return events;
    }

    /**
     * <p>
     * Setter for the field <code>fulltext</code>.
     * </p>
     *
     * @param fulltext the fulltext to set
     */
    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    /**
     * <p>
     * Getter for the field <code>fulltext</code>.
     * </p>
     *
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
                fulltextForHtml = StringTools.stripJS(fulltext).replace("\n", " ");
            } else {
                fulltextForHtml = "";
            }
        }

        return fulltextForHtml;
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
     * Checks whether the search hit should identify itself as a group document when being displayed.
     * 
     * @return true if group and not newspaper; false otherwise
     */
    public boolean isDisplayGroupStatus() {
        return isGroup() && !"newspaper".equalsIgnoreCase(getDocStructType());
    }

    /**
     *
     * @return true if doctype is GROUP; false otherwise
     */
    public boolean isGroup() {
        return DocType.GROUP.equals(docType);
    }

    /**
     * 
     * @return true if doctype is ARCHIVE; false otherwise
     */
    public boolean isArchive() {
        return DocType.ARCHIVE.equals(docType);
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
     * isHasImages.
     * </p>
     *
     * @return the hasImages
     */
    public boolean isHasImages() {
        return hasImages;
    }

    /**
     * <p>
     * Setter for the field <code>hasImages</code>.
     * </p>
     *
     * @param hasImages the hasImages to set
     */
    public void setHasImages(boolean hasImages) {
        this.hasImages = hasImages;
    }

    /**
     * @return the hasTeiFiles
     */
    public boolean isHasTeiFiles() {
        return hasTeiFiles;
    }

    /**
     * @param hasTeiFiles the hasTeiFiles to set
     */
    public void setHasTeiFiles(boolean hasTeiFiles) {
        this.hasTeiFiles = hasTeiFiles;
    }

    /**
     * @return the showThumbnail
     */
    public boolean isShowThumbnail() {
        return showThumbnail;
    }

    /**
     * @param showThumbnail the showThumbnail to set
     */
    public void setShowThumbnail(boolean showThumbnail) {
        this.showThumbnail = showThumbnail;
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
     * Setter for the field <code>pi</code>.
     * </p>
     *
     * @param pi the identifier to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
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
     * <p>
     * Getter for the field <code>sidebarPrevUrl</code>.
     * </p>
     *
     * @return the sidebarPrevUrl
     */
    public String getSidebarPrevUrl() {
        return sidebarPrevUrl;
    }

    /**
     * <p>
     * Getter for the field <code>sidebarNextUrl</code>.
     * </p>
     *
     * @return the sidebarNextUrl
     */
    public String getSidebarNextUrl() {
        return sidebarNextUrl;
    }

    /**
     *
     * @return Generated URL
     */
    private String generateUrl() {
        return DataManager.getInstance().getUrlBuilder().generateURL(this);
    }

    /**
     * Important: hits have to have 4 Pretty parameters (e.g. /image/nextHit/PPN123/1/LOG_0001/)
     *
     * @param type
     * @return Generated URL
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
            // logger.trace("Found configured page type: {}", configuredPageType.getName()); //NOSONAR Debug
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
                    .append(PageType.viewObject.getName())
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

    /**
     * @return the risExport
     */
    public String getRisExport() {
        return risExport;
    }

    /**
     * 
     * @return List of field names in the metadata list
     */
    public Set<String> getMetadataFieldNames() {
        Set<String> ret = new HashSet<>(getMetadataList().size());
        for (Metadata md : getMetadataList()) {
            ret.add(md.getLabel());
        }

        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>metadataList</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getMetadataList() {
        return metadataListMap.get(Configuration.METADATA_LIST_TYPE_SEARCH_HIT);
    }

    public List<String> getMetadataValues(String field) {
        return getMetadataListForLocale(field, BeanUtils.getLocale()).stream()
                .flatMap(md -> md.getValues().stream())
                .map(MetadataValue::getCombinedValue)
                .toList();
    }

    public String getFirstMetadataValue(String field) {
        return getMetadataListForLocale(field, BeanUtils.getLocale()).stream()
                .flatMap(md -> md.getValues().stream())
                .findFirst()
                .map(MetadataValue::getCombinedValue)
                .orElse("");
    }

    /**
     * 
     * @param field
     * @param locale
     * @return List<Metadata>
     */
    public List<Metadata> getMetadataListForLocale(String field, Locale locale) {
        return Metadata.filterMetadata(this.metadataListMap.get(Configuration.METADATA_LIST_TYPE_SEARCH_HIT),
                locale != null ? locale.getLanguage() : null, field);
    }

    /**
     *
     * @param field Requested field name
     * @param locale Requested locale
     * @param metadataListType
     * @return List<Metadata>
     */
    public List<Metadata> getMetadataListForLocale(String field, Locale locale, String metadataListType) {
        return Metadata.filterMetadata(this.metadataListMap.get(metadataListType), locale != null ? locale.getLanguage() : null, field);
    }

    /**
     * <p>
     * getMetadataListForLocale.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getMetadataListForLocale(Locale locale) {
        return getMetadataListForLocale(locale, Configuration.METADATA_LIST_TYPE_SEARCH_HIT);
    }

    /**
     * 
     * @param locale
     * @param metadataListType
     * @return List<Metadata>
     */
    public List<Metadata> getMetadataListForLocale(Locale locale, String metadataListType) {
        return Metadata.filterMetadata(this.metadataListMap.get(metadataListType), locale != null ? locale.getLanguage() : null, null);
    }

    /**
     * <p>
     * getMetadataListForCurrentLocale.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Metadata> getMetadataListForCurrentLocale() {
        return getMetadataListForLocale(BeanUtils.getLocale());
    }

    /**
     * 
     * @return First metadata list in metadataListMap that's not the default search metadata list configuration; empty list if not found
     */
    public List<Metadata> getSecondaryMetadataListForCurrentLocale() {
        if (this.metadataListMap.size() > 1) {
            for (Entry<String, List<Metadata>> entry : this.metadataListMap.entrySet()) {
                if (!Configuration.METADATA_LIST_TYPE_SEARCH_HIT.equals(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * @return the existingMetadataFields
     */
    public Set<String> getExistingMetadataFields() {
        return existingMetadataFields;
    }

    /**
     * <p>
     * Getter for the field <code>metadataGroupType</code>.
     * </p>
     *
     * @return the metadataGroupType
     */
    public MetadataGroupType getMetadataGroupType() {
        return metadataGroupType;
    }

    /**
     * <p>
     * Getter for the field <code>metadataList</code>.
     * </p>
     *
     * @param metadataLabel a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
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
     * <p>
     * Getter for the field <code>foundMetadataList</code>.
     * </p>
     *
     * @return the foundMetadataList
     */
    public List<Metadata> getFoundMetadataList() {
        return foundMetadataList;
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
     * @return the accessPermissionThumbnail
     */
    public AccessPermission getAccessPermissionThumbnail() {
        return accessPermissionThumbnail;
    }

    /**
     * @param accessPermissionThumbnail the accessPermissionThumbnail to set
     */
    public void setAccessPermissionThumbnail(AccessPermission accessPermissionThumbnail) {
        this.accessPermissionThumbnail = accessPermissionThumbnail;
    }

    /**
     * Returns the ContextObject value for a COinS element using the docstruct hierarchy for this search hit..
     *
     * @return a {@link java.lang.String} object.
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
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            }
        }

        return contextObject;
    }

    /**
     * <p>
     * Getter for the field <code>recordLanguages</code>.
     * </p>
     *
     * @return the recordLanguages
     */
    public List<String> getRecordLanguages() {
        return recordLanguages;
    }

    /**
     * <p>
     * Setter for the field <code>hasMedia</code>.
     * </p>
     *
     * @param hasMedia the hasMedia to set
     */
    public void setHasMedia(boolean hasMedia) {
        this.hasMedia = hasMedia;
    }

    /**
     * <p>
     * isHasMedia.
     * </p>
     *
     * @return the hasMedia
     */
    public boolean isHasMedia() {
        return hasMedia;
    }

    /**
     * <p>
     * Getter for the field <code>originalFieldName</code>.
     * </p>
     *
     * @return the originalFieldName
     */
    public String getOriginalFieldName() {
        return originalFieldName;
    }

    /**
     * <p>
     * determinePageType.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public PageType determinePageType() {
        return PageType.determinePageType(docStructType, mimeType, anchor || DocType.GROUP.equals(docType), hasImages || hasMedia, false);
    }

    /**
     * <p>
     * Getter for the field <code>logId</code>.
     * </p>
     *
     * @return the logId
     */
    public String getLogId() {
        return logId;
    }

    /**
     * @param logId the logId to set
     */
    public void setLogId(String logId) {
        this.logId = logId;
    }

    /**
     * <p>
     * Getter for the field <code>docType</code>.
     * </p>
     *
     * @return the docType
     */
    public DocType getDocType() {
        return docType;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

}
