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
package io.goobi.viewer.model.toc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessDeniedInfoConfig;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IAccessDeniedThumbnailOutput;
import io.goobi.viewer.model.viewer.PageType;

/**
 * Single TOC entry.
 */
public class TOCElement implements IAccessDeniedThumbnailOutput, Serializable {

    private static final long serialVersionUID = 5022749180237132594L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(TOCElement.class);

    private final Map<String, String> metadata = new HashMap<>();
    private final IMetadataValue label;
    private final String pageNo;
    private final String pageNoLabel;
    private final String iddoc;
    private final String logId;
    private final int level;
    private final String topStructPi;
    private final String thumbnailUrl;
    private final String recordMimeType;
    private final boolean anchorOrGroup;
    private String urlPrefix = "";
    private String urlSuffix = "";
    private final boolean accessPermissionPdf;
    private AccessPermission accessPermissionThumbnail = null;
    /** Element is visible in the current tree. */
    private boolean visible = true;
    private int id = -1;
    private int parentId = -1;
    private String footerId = "";
    /** Element is expanded or collapsed. */
    private boolean expanded = false;
    /** Element has child elements. TODO: this might be unnecessary */
    private boolean hasChild = false;
    private List<String> groupIds = null;
    private PageType pageType = PageType.viewMetadata;

    /**
     * Creates a new TOCElement instance.
     *
     * @param label multilingual display label for this element
     * @param pageNo physical page number as string
     * @param pageNoLabel human-readable page label
     * @param iddoc Solr IDDOC identifier of this element
     * @param logId logical structure ID from METS
     * @param level nesting depth in the TOC hierarchy
     * @param topStructPi persistent identifier of the top-level record
     * @param thumbnailUrl URL of the representative thumbnail image
     * @param accessPermissionPdf true if PDF download is permitted
     * @param anchorOrGroup true if this element is an anchor or group record
     * @param hasImages true if the structure element has image pages
     * @param recordMimeType MIME type of the associated record
     * @param docStructType document structure type string from METS
     * @param footerId identifier for the associated footer configuration
     * @should add logId to url
     * @should set correct view url for given docStructType
     */
    public TOCElement(IMetadataValue label, String pageNo, String pageNoLabel, String iddoc, String logId, int level, String topStructPi,
            String thumbnailUrl, boolean accessPermissionPdf, boolean anchorOrGroup, boolean hasImages, String recordMimeType, String docStructType,
            String footerId) {
        this.label = label;
        this.pageNo = pageNo;
        this.pageNoLabel = pageNoLabel;
        this.iddoc = iddoc;
        this.logId = logId;
        this.level = level;
        this.topStructPi = topStructPi;
        this.thumbnailUrl = thumbnailUrl;
        this.accessPermissionPdf = accessPermissionPdf;
        this.anchorOrGroup = anchorOrGroup;
        this.recordMimeType = recordMimeType;
        this.footerId = footerId;

        pageType = PageType.determinePageType(docStructType, recordMimeType, anchorOrGroup, hasImages, false);
        urlPrefix = new StringBuilder().append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').toString();
        urlSuffix =
                DataManager.getInstance()
                        .getUrlBuilder()
                        .buildPageUrl(topStructPi, pageNo != null ? Integer.valueOf(pageNo) : 1, logId, pageType, false);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((logId == null) ? 0 : logId.hashCode());
        result = prime * result + ((pageNo == null) ? 0 : pageNo.hashCode());
        result = prime * result + ((topStructPi == null) ? 0 : topStructPi.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TOCElement other = (TOCElement) obj;
        if (logId == null) {
            if (other.logId != null) {
                return false;
            }
        } else if (!logId.equals(other.logId)) {
            return false;
        }
        if (pageNo == null) {
            if (other.pageNo != null) {
                return false;
            }
        } else if (!pageNo.equals(other.pageNo)) {
            return false;
        }
        if (topStructPi == null) {
            if (other.topStructPi != null) {
                return false;
            }
        } else if (!topStructPi.equals(other.topStructPi)) {
            return false;
        }
        return true;
    }

    /**
     * getContentServerPdfUrl.
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getContentServerPdfUrl() throws ViewerConfigurationException {
        return BeanUtils.getImageDeliveryBean()
                .getPdf()
                .getPdfUrl(topStructPi, Optional.ofNullable(logId), label.getValue());

    }

    /**
     * Checks whether the current user has permissions to download a PDFs for this element.
     *
     * @return true if the current user has PDF download permission for this TOC element, false otherwise
     */
    public boolean isAccessPermissionPdf() {
        return accessPermissionPdf;
    }

    
    public AccessPermission getAccessPermissionThumbnail() {
        return accessPermissionThumbnail;
    }

    
    public void setAccessPermissionThumbnail(AccessPermission accessPermissionThumbnail) {
        this.accessPermissionThumbnail = accessPermissionThumbnail;
    }

    /**
     * Getter for the field <code>thumbnailUrl</code>.
     *
     * @return the thumbnail URL for this TOC element
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * Getter for the field <code>thumbnailUrl</code>.
     *
     * @param width desired thumbnail width in pixels
     * @param height desired thumbnail height in pixels
     * @return the thumbnail URL for this TOC element scaled to the given dimensions
     */
    public String getThumbnailUrl(int width, int height) {

        String url = thumbnailUrl;
        if (StringUtils.isNotBlank(url)) {
            Scale scale = new Scale.ScaleToBox(width, height);
            try {
                return BeanUtils.getImageDeliveryBean()
                        .getIiif()
                        .getModifiedIIIFFUrl(thumbnailUrl, RegionRequest.FULL, scale, Rotation.NONE, Colortype.DEFAULT,
                                ImageFileFormat.getImageFileFormatFromFileExtension(thumbnailUrl));
            } catch (ViewerConfigurationException e) {
                logger.error("Cannot reach ImageDeliveryBean for iiif url generation");
                return thumbnailUrl;
            }
        }
        return url;
    }

    @Override
    public String getAccessDeniedThumbnailUrl(Locale locale) throws IndexUnreachableException, DAOException {
        logger.trace("getAccessDeniedThumbnailUrl: locale: {}, LOGID: {}", locale, logId);
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
     * Getter for the field <code>label</code>.
     *
     * @return the label value for the current locale, or the default value if no locale is available
     */
    public String getLabel() {
        Locale locale = BeanUtils.getLocale();
        if (locale != null) {
            return getLabel(locale);
        }

        return label.getValue().orElse("");
    }

    /**
     * Getter for the field <code>label</code>.
     *
     * @param locale locale used to select the label value
     * @return the label value for the given locale, or the default value if none is set for that locale
     */
    public String getLabel(Locale locale) {
        return label.getValue(locale).orElse(label.getValue().orElse(""));
    }

    /**
     * Getter for the field <code>label</code>.
     *
     * @param locale BCP 47 language tag for label selection
     * @return the label value for the given language tag, or the default value if none is set for that language
     */
    public String getLabel(String locale) {
        return label.getValue(locale).orElse(label.getValue().orElse(""));
    }

    /**
     * Getter for the field <code>metadata</code>.
     *
     * @return the metadata map containing additional field values for this TOC element
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * getMetadataValue.
     *
     * @param key metadata field name to look up
     * @return the metadata value for the given field name, or null if not present
     */
    public String getMetadataValue(String key) {
        return metadata.get(key);
    }

    /**
     * Getter for the field <code>pageNo</code>.
     *
     * @return the page number associated with this TOC element
     */
    public String getPageNo() {
        return pageNo;
    }

    /**
     * Getter for the field <code>iddoc</code>.
     *
     * @return the internal Solr document identifier for this TOC element
     */
    public String getIddoc() {
        return iddoc;
    }

    /**
     * Getter for the field <code>pageNoLabel</code>.
     *
     * @return the human-readable page number label for this TOC element
     */
    public String getPageNoLabel() {
        return pageNoLabel;
    }

    /**
     * Getter for the field <code>topStructPi</code>.
     *
     * @return the persistent identifier of the top-level structure this element belongs to
     */
    public String getTopStructPi() {
        return topStructPi;
    }

    /**
     * Gibt die logID aus der Mets Datei zurück.
     *
     * @return the logical identifier from the METS file for this TOC element
     */
    public String getLogId() {
        return this.logId;
    }

    /**
     * Getter for the field <code>level</code>.
     *
     * @return a int.
     */
    public int getLevel() {
        return level;
    }

    /**
     * getUrl.
     *
     * @return the URL for this TOC element
     * @should return URL containing page type, PI, page number and logId for fullscreen view
     * @should return URL containing page type, PI, page number and logId for reading mode view
     */
    public String getUrl() {
        return urlPrefix + urlSuffix;
    }

    /**
     * Returns the URL for this element that links to the requested view type.
     *
     * @param viewType name of the requested page view type
     * @should construct full screen url correctly
     * @should construct reading mode url correctly
     * @return the URL for this TOC element pointing to the given view type
     */
    public String getUrl(String viewType) {
        if (anchorOrGroup) {
            return urlPrefix + urlSuffix;
        }

        PageType pType = PageType.getByName(viewType);
        if (pType != null) {
            switch (pType) {
                case viewFullscreen:
                    if (PageType.viewObject.equals(this.pageType) || PageType.viewImage.equals(this.pageType)) {
                        return urlPrefix + DataManager.getInstance()
                                .getUrlBuilder()
                                .buildPageUrl(topStructPi, Integer.valueOf(pageNo), logId, PageType.viewFullscreen, false);
                    }
                    break;
                case viewImage:
                    return urlPrefix
                            + DataManager.getInstance()
                                    .getUrlBuilder()
                                    .buildPageUrl(topStructPi, Integer.valueOf(pageNo), logId, PageType.viewImage, false);
                default:
                    return urlPrefix + urlSuffix;
            }
        }

        return urlPrefix + urlSuffix;
    }

    /**
     * getFullscreenUrl.
     *
     * @return the URL for displaying this TOC element in fullscreen view
     */
    public String getFullscreenUrl() {
        return getUrl(PageType.viewFullscreen.name());
    }

    /**
     * isVisible.
     *
     * @return true if this TOC element is currently visible, false otherwise
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Setter for the field <code>visible</code>.
     *
     * @param visible true to show this element, false to hide it
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Getter for the field <code>parentId</code>.
     *
     * @return a int.
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * Setter for the field <code>parentId</code>.
     *
     * @param parentId ID of the parent TOC element
     */
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    /**
     * getID.
     *
     * @return a int.
     */
    public int getID() {
        return id;
    }

    /**
     * setID.
     *
     * @param iD numeric ID to assign to this element
     */
    public void setID(int iD) {
        id = iD;
    }

    /**
     * isExpanded.
     *
     * @return true if this TOC element is currently shown expanded, false otherwise
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Setter for the field <code>expanded</code>.
     *
     * @param expanded true if this element should be shown expanded
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * isHasChild.
     *
     * @return true if this TOC element has at least one child element, false otherwise
     */
    public boolean isHasChild() {
        return hasChild;
    }

    /**
     * Setter for the field <code>hasChild</code>.
     *
     * @param hasChild true if this element has child elements
     */
    public void setHasChild(boolean hasChild) {
        this.hasChild = hasChild;
    }

    /**
     * Getter for the field <code>groupIds</code>.
     *
     * @return the list of group identifier strings associated with this TOC element
     */
    public List<String> getGroupIds() {
        return groupIds;
    }

    /**
     * Setter for the field <code>groupIds</code>.
     *
     * @param groupIds list of group identifier strings to assign
     */
    public void setGroupIds(List<String> groupIds) {
        this.groupIds = groupIds;
    }

    /**
     * Getter for the field <code>recordMimeType</code>.
     *
     * @return the MIME type of the record associated with this TOC element
     */
    public String getRecordMimeType() {
        return recordMimeType;
    }

    /**
     * isEmpty.
     *
     * @return true if label is null, empty or blank
     */
    public boolean isEmpty() {
        return StringUtils.isBlank(this.label.toString());
    }
}