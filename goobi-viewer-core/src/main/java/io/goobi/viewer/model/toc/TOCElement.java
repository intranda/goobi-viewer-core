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
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.PageType;

/**
 * Single TOC entry.
 */
public class TOCElement implements Serializable {

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
     * <p>
     * Constructor for TOCElement.
     * </p>
     *
     * @param label a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     * @param pageNo a {@link java.lang.String} object.
     * @param pageNoLabel a {@link java.lang.String} object.
     * @param iddoc a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param level a int.
     * @param topStructPi a {@link java.lang.String} object.
     * @param thumbnailUrl a {@link java.lang.String} object.
     * @param accessPermissionPdf a boolean.
     * @param anchorOrGroup a boolean.
     * @param hasImages a boolean.
     * @param recordMimeType a {@link java.lang.String} object.
     * @param docStructType a {@link java.lang.String} object.
     * @param footerId a {@link java.lang.String} object.
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
     * <p>
     * getContentServerPdfUrl.
     * </p>
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
     * @return a boolean.
     */
    public boolean isAccessPermissionPdf() {
        return accessPermissionPdf;
    }

    /**
     * <p>
     * Getter for the field <code>thumbnailUrl</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * <p>
     * Getter for the field <code>thumbnailUrl</code>.
     * </p>
     *
     * @param width a int.
     * @param height a int.
     * @return a {@link java.lang.String} object.
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

    /*********************************** Getter and Setter ***************************************/

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    public String getLabel() {
        Locale locale = BeanUtils.getLocale();
        if (locale != null) {
            return getLabel(locale);
        }

        return label.getValue().orElse("");
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     * @param locale a {@link java.util.Locale} object.
     */
    public String getLabel(Locale locale) {
        return label.getValue(locale).orElse(label.getValue().orElse(""));
    }

    /**
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     * @param locale a {@link java.lang.String} object.
     */
    public String getLabel(String locale) {
        return label.getValue(locale).orElse(label.getValue().orElse(""));
    }

    /**
     * <p>
     * Getter for the field <code>metadata</code>.
     * </p>
     *
     * @return the metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * <p>
     * getMetadataValue.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataValue(String key) {
        return metadata.get(key);
    }

    /**
     * <p>
     * Getter for the field <code>pageNo</code>.
     * </p>
     *
     * @return the pageNo
     */
    public String getPageNo() {
        return pageNo;
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
     * Getter for the field <code>pageNoLabel</code>.
     * </p>
     *
     * @return the pageNoLabel
     */
    public String getPageNoLabel() {
        return pageNoLabel;
    }

    /**
     * <p>
     * Getter for the field <code>topStructPi</code>.
     * </p>
     *
     * @return the topStructPi
     */
    public String getTopStructPi() {
        return topStructPi;
    }

    /**
     * gibt die logID aus der Mets Datei zurück
     *
     * @return the logID
     */
    public String getLogId() {
        return this.logId;
    }

    /**
     * <p>
     * Getter for the field <code>level</code>.
     * </p>
     *
     * @return a int.
     */
    public int getLevel() {
        return level;
    }

    /**
     * <p>
     * getUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUrl() {
        return urlPrefix + urlSuffix;
    }

    /**
     * Returns the URL for this element that links to the requested view type.
     *
     * @param viewType a {@link java.lang.String} object.
     * @should construct full screen url correctly
     * @should construct reading mode url correctly
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getFullscreenUrl.
     * </p>
     *
     * @return the fullscreenUrl
     */
    public String getFullscreenUrl() {
        return getUrl(PageType.viewFullscreen.name());
    }

    /**
     * <p>
     * isVisible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * <p>
     * Setter for the field <code>visible</code>.
     * </p>
     *
     * @param visible a boolean.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * <p>
     * Getter for the field <code>parentId</code>.
     * </p>
     *
     * @return a int.
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * <p>
     * Setter for the field <code>parentId</code>.
     * </p>
     *
     * @param parentId a int.
     */
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    /**
     * <p>
     * getID.
     * </p>
     *
     * @return a int.
     */
    public int getID() {
        return id;
    }

    /**
     * <p>
     * setID.
     * </p>
     *
     * @param iD a int.
     */
    public void setID(int iD) {
        id = iD;
    }

    /**
     * <p>
     * isExpanded.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * <p>
     * Setter for the field <code>expanded</code>.
     * </p>
     *
     * @param expanded a boolean.
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * <p>
     * isHasChild.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasChild() {
        return hasChild;
    }

    /**
     * <p>
     * Setter for the field <code>hasChild</code>.
     * </p>
     *
     * @param hasChild a boolean.
     */
    public void setHasChild(boolean hasChild) {
        this.hasChild = hasChild;
    }

    /**
     * <p>
     * Getter for the field <code>groupIds</code>.
     * </p>
     *
     * @return the groupIds
     */
    public List<String> getGroupIds() {
        return groupIds;
    }

    /**
     * <p>
     * Setter for the field <code>groupIds</code>.
     * </p>
     *
     * @param groupIds the groupIds to set
     */
    public void setGroupIds(List<String> groupIds) {
        this.groupIds = groupIds;
    }

    /**
     * <p>
     * Getter for the field <code>recordMimeType</code>.
     * </p>
     *
     * @return the recordMimeType
     */
    public String getRecordMimeType() {
        return recordMimeType;
    }

    /**
     * <p>
     * isEmpty.
     * </p>
     *
     * @return true if label is null, empty or blank
     */
    public boolean isEmpty() {
        return StringUtils.isBlank(this.label.toString());
    }

}
