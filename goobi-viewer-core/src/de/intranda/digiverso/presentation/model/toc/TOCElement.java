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
package de.intranda.digiverso.presentation.model.toc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.PageType;

/**
 * Single TOC entry.
 */
public class TOCElement implements Serializable {

    private static final long serialVersionUID = 5022749180237132594L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(TOCElement.class);

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
    private int id = 0;
    private int parentId = 0;
    private String footerId = "";
    /** Element is expanded or collapsed. */
    private boolean expanded = false;
    /** Element has child elements. TODO: this might be unnecessary */
    private boolean hasChild = false;
    private List<String> groupIds = null;
    private PageType pageType = PageType.viewMetadata;

    /**
     *
     * @param label
     * @param pageNo
     * @param pageNoLabel
     * @param iddoc
     * @param logId
     * @param level
     * @param topStructPi
     * @param thumbnailUrl
     * @param accessPermissionPdf
     * @param anchorOrGroup
     * @param hasImages
     * @param recordMimeType
     * @param docStructType
     * @param footerId
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
                DataManager.getInstance().getUrlBuilder().buildPageUrl(topStructPi, pageNo != null ? Integer.valueOf(pageNo) : 1, logId, pageType);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((logId == null) ? 0 : logId.hashCode());
        result = prime * result + ((pageNo == null) ? 0 : pageNo.hashCode());
        result = prime * result + ((topStructPi == null) ? 0 : topStructPi.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
     * @param str {@link String}
     * @return {@link String}
     * @throws ViewerConfigurationException
     */
    public String getContentServerPdfUrl() throws ViewerConfigurationException {
        return BeanUtils.getImageDeliveryBean()
                .getPdf()
                .getPdfUrl(topStructPi, Optional.ofNullable(logId), Optional.ofNullable(getFooterId()), Optional.empty(), label.getValue());

    }

    private String getFooterId() {
        return this.footerId;
    }

    /**
     * Checks whether the current user has permissions to download a PDFs for this element.
     *
     * @return
     */
    public boolean isAccessPermissionPdf() {
        return accessPermissionPdf;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getThumbnailUrl(int width, int height) {

        String url = new String(thumbnailUrl);
        if (StringUtils.isNotBlank(url)) {
            if (url.contains("width=")) {
                url = url.replaceAll("width=\\d+", "width=" + width);
            } else {
                url = url + "&width=" + width;
            }
            if (url.contains("height=")) {
                url = url.replaceAll("height=\\d+", "height=" + height);
            } else {
                url = url + "&height=" + height;
            }
        }
        return url;
    }

    /*********************************** Getter and Setter ***************************************/

    /**
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
     * @return the label
     */
    public String getLabel(Locale locale) {
        return label.getValue(locale).orElse(label.getValue().orElse(""));
    }

    /**
     * @return the label
     */
    public String getLabel(String locale) {
        return label.getValue(locale).orElse(label.getValue().orElse(""));
    }

    /**
     * @return the metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getMetadataValue(String key) {
        return metadata.get(key);
    }

    /**
     * @return the pageNo
     */
    public String getPageNo() {
        return pageNo;
    }

    /**
     * @return the iddoc
     */
    public String getIddoc() {
        return iddoc;
    }

    /**
     * @return the pageNoLabel
     */
    public String getPageNoLabel() {
        return pageNoLabel;
    }

    /**
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

    public int getLevel() {
        return level;
    }

    /**
     * @return the subLabel
     */
    @Deprecated
    public String getSubLabel() {
        return label.getValue().orElse("");
    }

    public String getUrl() {
        return urlPrefix + urlSuffix;
    }

    /**
     * Returns the URL for this element that links to the requested view type.
     * 
     * @param viewType
     * @return
     * @should construct full screen url correctly
     * @should construct reading mode url correctly
     */
    public String getUrl(String viewType) {
        if (anchorOrGroup) {
            return urlPrefix + urlSuffix;
        }

        PageType pageType = PageType.getByName(viewType);
        if (pageType != null) {
            switch (pageType) {
                case viewFullscreen:
                    if (PageType.viewObject.equals(this.pageType) || PageType.viewImage.equals(this.pageType)) {
                        return urlPrefix + DataManager.getInstance()
                                .getUrlBuilder()
                                .buildPageUrl(topStructPi, Integer.valueOf(pageNo), logId, PageType.viewFullscreen);
                    }
                    break;
                case viewImage:
                    return urlPrefix
                            + DataManager.getInstance().getUrlBuilder().buildPageUrl(topStructPi, Integer.valueOf(pageNo), logId, PageType.viewImage);
                default:
                    return urlPrefix + urlSuffix;
            }
        }

        return urlPrefix + urlSuffix;
    }

    /**
     * @return the fullscreenUrl
     * @deprecated
     */
    public String getFullscreenUrl() {
        return getUrl(PageType.viewFullscreen.name());
    }

    /**
     * @return the fullscreenUrl
     * @deprecated renamed to fullscreen
     */
    @Deprecated
    public String getReadingModeUrl() {
        return getFullscreenUrl();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getID() {
        return id;
    }

    public void setID(int iD) {
        id = iD;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isHasChild() {
        return hasChild;
    }

    public void setHasChild(boolean hasChild) {
        this.hasChild = hasChild;
    }

    /**
     * @return the groupIds
     */
    public List<String> getGroupIds() {
        return groupIds;
    }

    /**
     * @param groupIds the groupIds to set
     */
    public void setGroupIds(List<String> groupIds) {
        this.groupIds = groupIds;
    }

    /**
     * @return the recordMimeType
     */
    public String getRecordMimeType() {
        return recordMimeType;
    }

    /**
     * 
     * @return true if label is null, empty or blank
     */
    public boolean isEmpty() {
        return StringUtils.isBlank(this.label.toString());
    }

}
