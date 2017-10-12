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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrConstants.MetadataGroupType;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter.MetadataParameterType;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.model.viewer.StructElementStub;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;

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
    private boolean useOverviewPage = false;
    @JsonIgnore
    private long numVolumes = 0;
    private String pi;
    private String logId;
    @JsonIgnore
    private NavigationHelper navigationHelper;
    @JsonIgnore
    private List<Metadata> metadataList = null;
    @JsonIgnore
    private String fileExtension = "";
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
    BrowseElement(String pi, int imageNo, String label, String fulltext, boolean useOverviewPage, Locale locale) {
        this.pi = pi;
        this.imageNo = imageNo;
        this.label = label;
        this.fulltext = fulltext;
        this.useOverviewPage = useOverviewPage;
        this.locale = locale;
        this.metadataList = new ArrayList<>();
        this.url = generateUrl();
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
     */
    BrowseElement(StructElement structElement, List<Metadata> metadataList, Locale locale, String fulltext, boolean useThumbnail,
            Map<String, Set<String>> searchTerms) throws PresentationException, IndexUnreachableException, DAOException {
        this.metadataList = metadataList;
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
            originalFieldName = structElement.getLabel();
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
                            this.metadataList.add(position, new Metadata(anchorStructElement.getDocStructType(), null, new MetadataParameter(
                                    MetadataParameterType.FIELD, null, anchorStructElement.getDocStructType(), null, null, null, false, false), Helper
                                            .intern(anchorLabel)));
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
                    this.metadataList.add(position, new Metadata(topStructElement.getDocStructType(), null, new MetadataParameter(
                            MetadataParameterType.FIELD, null, topStructElement.getDocStructType(), null, null, null, false, false), Helper.intern(
                                    topstructLabel)));
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
                        if (topStructElement != null && !topStructElement.equals(elementToUse) && !MetadataParameterType.ANCHORFIELD.equals(param
                                .getType()) && !param.isDontUseTopstructValue()) {
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
                        md.setParamValue(count, md.getParams().indexOf(param), Helper.intern(value), null, param.isAddUrl() ? elementToUse.getUrl()
                                : null, null, locale);
                        count++;
                    }
                }
            }
        }

        if (navigationHelper == null) {
            try {
                navigationHelper = (NavigationHelper) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("navigationHelper");
            } catch (NullPointerException e) {
                // logger.trace("No navigationHelper available");
            }
        }

        anchor = structElement.isAnchor();
        numVolumes = structElement.getNumVolumes();
        docStructType = structElement.getDocStructType();

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
        if (filename != null) {
            fileExtension = FilenameUtils.getExtension(filename).toLowerCase();
        }
        try {
            if (anchor) {
                mimeType = structElement.getFirstVolumeFieldValue(SolrConstants.MIMETYPE);
            } else {
                mimeType = structElement.getMetadataValue(SolrConstants.MIMETYPE);
            }
            if (mimeType == null && filename != null) {
                mimeType = getMimeTypeFromExtension(filename);
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            //no children
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
        int thumbWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
        int thumbHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
        if (isAbsoluteUrl(filename) && structElement.mayShowThumbnail()) {
            // Use absolute thumbnail URL directly (replace requested image size if this is an IFFF URL); no access permission check
            thumbnailUrl = PhysicalElement.getModifiedIIIFFUrl(filename, thumbWidth, thumbHeight);
            hasImages = true;
        } else if (structElement.mayShowThumbnail()) {
            // Construct URL
            String filepath = filename;
            if (StringUtils.isNotEmpty(filepath)) {
                // Determine whether the file is in a data repository
                String dataRepository = structElement.getMetadataValue(SolrConstants.DATAREPOSITORY);
                if (StringUtils.isNotEmpty(dataRepository)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("file:/").append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome().charAt(0) == '/' ? '/' : "")
                            .append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append(dataRepository).append('/').append(
                                    DataManager.getInstance().getConfiguration().getMediaFolder()).append('/').append(pi).append('/').append(
                                            filepath);
                    filepath = sb.toString();
                } else {
                    filepath = new StringBuilder(pi).append('/').append(filepath).toString();
                }
            }
            // logger.trace("filepath: {}", filepath);

            StringBuilder sbThumbnailUrl = new StringBuilder(140);
            if (anchor) {
                // Anchor
                //                logger.trace("anchor");
                switch (DataManager.getInstance().getConfiguration().getAnchorThumbnailMode()) {
                    case "FIRSTVOLUME":
                        // Use first volume's representative image
                        filepath = getFirstVolumeThumbnailPath(pi);
                        sbThumbnailUrl.append(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append(
                                "?action=image&sourcepath=").append(filepath).append("&width=").append(thumbWidth).append("&height=").append(
                                        thumbHeight).append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true").append(DataManager
                                                .getInstance().getConfiguration().isForceJpegConversion() ? "&format=jpg" : "");
                        break;
                    default:
                        // Default anchor thumbnail
                        sbThumbnailUrl.append(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append(
                                "?action=image&sourcepath=").append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append(
                                        "/resources/themes/").append(DataManager.getInstance().getConfiguration().getTheme()).append(
                                                "/images/multivolume_thumbnail.jpg&width=").append(thumbWidth).append("&height=").append(thumbHeight)
                                .append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true&format=jpg");
                        break;
                }
            } else if (DocType.DOCSTRCT.equals(docType) || DocType.PAGE.equals(docType) || docType == null) {
                // Docstruct or page
                //                logger.trace("normal");
                if (StringUtils.isNotEmpty(filepath) && !isAbsoluteUrl(filename)) {
                    switch (fileExtension) {
                        case "pdf":
                            // E-Publication page
                            sbThumbnailUrl.append(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append(
                                    "?action=image&sourcepath=").append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append(
                                            "/resources/themes/").append(DataManager.getInstance().getConfiguration().getTheme()).append(
                                                    "/images/thumbnail_epub.jpg&width=").append(thumbWidth).append("&height=").append(thumbHeight)
                                    .append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true&format=jpg");
                            break;
                        default:
                            // Regular page or docstruct
                            if (useThumbnail) {
                                boolean access = FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext()
                                        .getRequest() != null ? SearchHelper.checkAccessPermissionForThumbnail((HttpServletRequest) FacesContext
                                                .getCurrentInstance().getExternalContext().getRequest(), pi, filename) : false;
                                // Flag the thumbnail for this element as access denied, so that further visualization can be triggered in HTML.
                                // The display of the "access denied" thumbnail is done in the ContentServerWrapperServlet.
                                if (!access) {
                                    thumbnailAccessDenied = true;
                                }
                                sbThumbnailUrl.append(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append(
                                        "?action=image&sourcepath=").append(filepath).append("&width=").append(thumbWidth).append("&height=").append(
                                                thumbHeight).append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true").append(DataManager
                                                        .getInstance().getConfiguration().isForceJpegConversion() ? "&format=jpg" : "");
                            }
                            hasImages = true;
                            break;
                    }
                } else if (mimeType != null) {
                    switch (mimeType) {
                        // Default thumbnail for video with no thumbnail file
                        case "image/png":
                        case "image/jpg":
                            sbThumbnailUrl.append(filename);
                            hasImages = true;
                            break;
                        case PhysicalElement.MIME_TYPE_VIDEO:
                        case PhysicalElement.MIME_TYPE_SANDBOXED_HTML:
                            sbThumbnailUrl.append(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append(
                                    "?action=image&sourcepath=").append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append(
                                            "/resources/themes/").append(DataManager.getInstance().getConfiguration().getTheme()).append(
                                                    "/images/thumbnail_video.jpg&width=").append(thumbWidth).append("&height=").append(thumbHeight)
                                    .append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true&format=jpg");
                            logger.trace("Thumbnail: {}", sbThumbnailUrl.toString());
                        case PhysicalElement.MIME_TYPE_AUDIO:
                            hasImages = true;
                            break;
                    }
                }
            } else if (DocType.EVENT.equals(docType)) {
                // LIDO event
                sbThumbnailUrl.append(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=image&sourcepath=")
                        .append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/resources/themes/").append(DataManager.getInstance()
                                .getConfiguration().getTheme()).append("/images/thumbnail_event.jpg&width=").append(thumbWidth).append("&height=")
                        .append(thumbHeight).append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true").append(DataManager.getInstance()
                                .getConfiguration().isForceJpegConversion() ? "&format=jpg" : "");
            } else if (DocType.GROUP.equals(docType)) {
                // Group (convolute / series)
                sbThumbnailUrl.append(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=image&sourcepath=")
                        .append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/resources/themes/").append(DataManager.getInstance()
                                .getConfiguration().getTheme()).append("/images/thumbnail_group.jpg&width=").append(thumbWidth).append("&height=")
                        .append(thumbHeight).append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true").append(DataManager.getInstance()
                                .getConfiguration().isForceJpegConversion() ? "&format=jpg" : "");
            } else if (metadataGroupType != null) {
                // Grouped metadata
                switch (metadataGroupType) {
                    case PERSON:
                        sbThumbnailUrl.append(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append(
                                "?action=image&sourcepath=").append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append(
                                        "/resources/themes/").append(DataManager.getInstance().getConfiguration().getTheme()).append(
                                                "/images/thumbnail_person.jpg&width=").append(thumbWidth).append("&height=").append(thumbHeight)
                                .append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true&format=jpg");
                        break;
                    default:
                        break;
                }
            }
            if (sbThumbnailUrl.length() > 0) {
                thumbnailUrl = Helper.intern(sbThumbnailUrl.toString());
            }
        }

        // Only topstructs should be openened with their overview page view (if they have one)
        if ((structElement.isWork() || structElement.isAnchor()) && OverviewPage.loadOverviewPage(structElement, locale) != null) {
            useOverviewPage = true;
        }

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
                //                case SolrConstants.OVERVIEWPAGE_DESCRIPTION:
                //                case SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT:
                //                    if (!overviewPageFetched && (structElement.isWork() || structElement.isAnchor())) {
                //                        // Only load the page once for both fields
                //                        overviewPageFetched = true;
                //                        try {
                //                            OverviewPage overviewPage = DataManager.getInstance().getDao().getOverviewPageForRecord(structElement.getPi(), null,
                //                                    null);
                //                            if (overviewPage != null) {
                //                                if (overviewPage.getDescription() != null) {
                //                                    String value = Jsoup.parse(overviewPage.getDescription()).text();
                //                                    String highlightedValue = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(
                //                                            SolrConstants.OVERVIEWPAGE_DESCRIPTION));
                //                                    if (!highlightedValue.equals(value)) {
                //                                        highlightedValue = SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.OVERVIEWPAGE_DESCRIPTION),
                //                                                highlightedValue, DataManager.getInstance().getConfiguration().getFulltextFragmentLength());
                //                                        metadataList.add(new Metadata("viewOverviewDescription", "", highlightedValue));
                //                                    }
                //                                }
                //                                if (overviewPage.getPublicationText() != null) {
                //                                    String value = Jsoup.parse(overviewPage.getPublicationText()).text();
                //                                    String highlightedValue = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(
                //                                            SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT));
                //                                    if (!highlightedValue.equals(value)) {
                //                                        highlightedValue = SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT),
                //                                                highlightedValue, DataManager.getInstance().getConfiguration().getFulltextFragmentLength());
                //                                        metadataList.add(new Metadata("viewOverviewPublication_publication", "", highlightedValue));
                //                                    }
                //                                }
                //                            }
                //                        } catch (DAOException e) {
                //                            logger.error(e.getMessage(), e);
                //                        }
                //                    }
                //                    break;
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
                                if (translateFields != null && translateFields.contains(termsFieldName)) {
                                    String translatedValue = Helper.getTranslation(fieldValue, locale);
                                    highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)", "$1"
                                            + translatedValue + "$3");
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                metadataList.add(new Metadata(docFieldName, "", highlightedValue));
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
                                    highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)", "$1"
                                            + translatedValue + "$3");
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                metadataList.add(new Metadata(termsFieldName, "", highlightedValue));
                            }
                        }
                    }
                    break;
            }
        }
    }

    /**
     * @param filepath
     * @return
     */
    private static boolean isAbsoluteUrl(String filepath) {
        if (StringUtils.isBlank(filepath)) {
            return false;
        }
        try {
            URI url = new URI(filepath);
            return url.isAbsolute();
        } catch (URISyntaxException e) {
            return false;
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
        return null;
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
                                if (se.getMetadataValue("NORM_NAME") != null) {
                                    ret = se.getMetadataValue("NORM_NAME");
                                } else {
                                    ret = se.getMetadataValue("MD_DISPLAYFORM");
                                }
                                break;
                            default:
                                ret = se.getMetadataValue(SolrConstants.LABEL);
                                break;
                        }
                    } else {
                        ret = se.getMetadataValue(SolrConstants.LABEL);
                    }
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
                    break;
                default:
                    ret = generateDefaultLabel(se, locale);
                    break;
            }
        } else {
            logger.warn("{} field seems to be missing on Solr document {}", SolrConstants.DOCTYPE, se.getLuceneId());
            return generateDefaultLabel(se, locale);
        }

        return ret;
    }

    /**
     * 
     * @param se
     * @param locale
     * @return
     */
    private String generateDefaultLabel(StructElement se, Locale locale) {
        String ret = se.getMetadataValue(SolrConstants.LABEL);
        if (StringUtils.isEmpty(ret)) {
            ret = se.getMetadataValue(SolrConstants.TITLE);
            if (StringUtils.isEmpty(ret)) {
                if (locale != null) {
                    for (String key : se.getMetadataFields().keySet()) {
                        if (key.startsWith(SolrConstants.TITLE)) {
                            if (key.endsWith(SolrConstants._LANG_ + locale.getLanguage().toUpperCase())) {
                                ret = se.getMetadataValue(key);
                                break;
                            }
                        }
                    }
                }
                if (StringUtils.isEmpty(ret)) {
                    ret = getDocStructType();
                }
            }
        }

        if (ret == null) {
            ret = "";
            logger.error("Index document {}, has no LABEL, MD_TITLE or DOCSTRUCT fields. Perhaps there is no connection to the owner doc?");
        }
        return ret;
    }

    /**
     * Retrieves the URL part for the representative thumbnail of the first indexed volume for the given anchor identifier.
     *
     * @param anchorPi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should build url part correctly
     */
    protected static String getFirstVolumeThumbnailPath(String anchorPi) throws PresentationException, IndexUnreachableException {
        String ret = null;

        String[] fields = { SolrConstants.PI, SolrConstants.DATAREPOSITORY, SolrConstants.THUMBNAIL };
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(new StringBuilder(SolrConstants.PI_PARENT).append(':').append(
                anchorPi).toString(), 1, Collections.singletonList(new StringPair(SolrConstants.CURRENTNOSORT, "asc")), Arrays.asList(fields));
        if (!docs.isEmpty()) {
            ret = (String) docs.get(0).getFieldValue(SolrConstants.THUMBNAIL);
            if (StringUtils.isNotEmpty(ret)) {
                String dataRepository = (String) docs.get(0).getFieldValue(SolrConstants.DATAREPOSITORY);
                if (StringUtils.isNotEmpty(dataRepository)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("file:/").append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome().charAt(0) == '/' ? '/' : "")
                            .append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append(dataRepository).append('/').append(
                                    DataManager.getInstance().getConfiguration().getMediaFolder()).append('/').append(docs.get(0).getFieldValue(
                                            SolrConstants.PI)).append('/').append(ret);
                    ret = sb.toString();
                } else {
                    ret = new StringBuilder((String) docs.get(0).getFieldValue(SolrConstants.PI)).append('/').append(ret).toString();
                }
            }
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
            url = url.replaceAll("width=\\d+", "").replaceAll("height=\\d+", "");
            StringBuilder urlBuilder = new StringBuilder(url);
            if (width != null) {
                urlBuilder.append("&width=").append(width);
            }
            if (height != null) {
                urlBuilder.append("&height=").append(height);
            }
            return urlBuilder.toString();
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
     * Returns a relevant full-text fragment for displaying in the search hit box.
     *
     * @return
     */
    public String getFulltextForHtml() {
        if (fulltextForHtml == null) {
            if (fulltext != null) {
                fulltextForHtml = fulltext.replaceAll("\n", " ");
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
        if (MetadataGroupType.PERSON.equals(metadataGroupType)) {
            // Person metadata search hit ==> execute search for that person
            try {
                sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(PageType.search.getName()).append("/-/").append(
                        originalFieldName).append(":\"").append(URLEncoder.encode(label, SearchBean.URL_ENCODING)).append("\"/1/-/-/");
            } catch (UnsupportedEncodingException e) {
                logger.error("{}: {}", e.getMessage(), label);
                sb = new StringBuilder();
                sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(PageType.search.getName()).append("/-/").append(
                        originalFieldName).append(":\"").append(label).append("\"/1/-/-/");
            }
        } else {
            //            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            //            sb.append('/');
            PageType pageType = PageType.determinePageType(docStructType, mimeType, anchor || DocType.GROUP.equals(docType), hasImages,
                    useOverviewPage, false);
            sb.append(pageType.getName()).append('/').append(pi).append('/').append(imageNo).append('/').append(StringUtils.isNotEmpty(logId) ? logId
                    : '-').append('/');
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
        PageType configuredPageType = PageType.getPagetTypeForDocStructType(docStructType);

        StringBuilder sb = new StringBuilder();
        if (anchor) {
            if (navigationHelper != null && PageType.viewMetadata.getName().equals(navigationHelper.getCurrentView())) {
                // Use the preferred view, if set and allowed for multivolumes
                String view = StringUtils.isNotEmpty(navigationHelper.getPreferredView()) ? navigationHelper.getPreferredView() : PageType.viewToc
                        .getName();
                if (!view.equals(PageType.viewToc.getName()) && !view.equals(PageType.viewMetadata.getName())) {
                    view = PageType.viewToc.getName();
                }
                sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(view).append('/').append(type).append('/').append(
                        pi).append('/').append(imageNo).append('/').append(StringUtils.isNotEmpty(logId) ? logId : '-').append('/');
            } else {
                sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(PageType.viewToc.getName()).append('/').append(
                        type).append('/').append(pi).append('/').append(imageNo).append('/').append(StringUtils.isNotEmpty(logId) ? logId : '-')
                        .append('/');
            }
        } else if (navigationHelper != null && StringUtils.isNotEmpty(navigationHelper.getPreferredView())) {
            // Use the preferred view, if set
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(navigationHelper.getPreferredView()).append('/')
                    .append(type).append('/').append(pi).append('/').append(imageNo).append('/').append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        } else if (configuredPageType != null) {
            logger.trace("Found configured page type: {}", configuredPageType.getName());
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(configuredPageType.getName()).append('/').append(type)
                    .append('/').append(pi).append('/').append(imageNo).append('/').append(StringUtils.isNotEmpty(logId) ? logId : '-').append('/');
        } else if (hasImages) {
            // Regular image view
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(PageType.viewImage.getName()).append('/').append(type)
                    .append('/').append(pi).append('/').append(imageNo).append('/').append(StringUtils.isNotEmpty(logId) ? logId : '-').append('/');
        } else {
            // Metadata view for elements without a thumbnail
            sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(PageType.viewMetadata.getName()).append('/').append(
                    type).append('/').append(pi).append('/').append(imageNo).append('/').append(StringUtils.isNotEmpty(logId) ? logId : '-').append(
                            '/');
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
}
