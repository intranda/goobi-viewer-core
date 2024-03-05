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
package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.media.CMSMediaItemMetadata;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_media")
@DiscriminatorValue("media")
public class CMSMediaContent extends CMSContent implements CMSMediaHolder, Comparable<CMSMediaContent> {

    private static final Logger logger = LogManager.getLogger(CMSMediaContent.class);

    private static final String BACKEND_COMPONENT_NAME = "media";

    @JoinColumn(name = "media_item_id")
    private CMSMediaItem mediaItem;

    public CMSMediaContent() {
        super();
    }

    public CMSMediaContent(CMSMediaContent orig) {
        super(orig);
        this.mediaItem = orig.mediaItem;
    }

    @Override
    public String getBackendComponentName() {
        return BACKEND_COMPONENT_NAME;
    }

    @Override
    public void setMediaItem(CMSMediaItem item) {
        this.mediaItem = item;
    }

    @Override
    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }

    @Override
    public String getMediaFilter() {
        return String.format("(%s)|(%s)", CmsMediaBean.getImageFilter(), CmsMediaBean.getVideoFilter());
    }

    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getImageTypes() + ", " + CmsMediaBean.getVideoTypes();
    }

    @Override
    public boolean hasMediaItem() {
        return this.mediaItem != null;
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales()
                            .stream()
                            .findFirst()
                            .orElse(BeanUtils.getLocale()),
                    Collections.emptyList());
        }
        return null;
    }

    public String getUrl() throws UnsupportedEncodingException {
        return getUrl(null, null);
    }

    public String getMediaType() {
        return getMediaItem() != null ? getMediaItem().getContentType() : "";
    }

    public String getUrl(String width, String height) throws UnsupportedEncodingException {

        String contentString = "";
        String type = getMediaType();
        switch (type) {
            case CMSMediaItem.CONTENT_TYPE_XML:
                contentString = CmsMediaBean.getMediaFileAsString(getMediaItem());
                break;
            case CMSMediaItem.CONTENT_TYPE_PDF:
            case CMSMediaItem.CONTENT_TYPE_VIDEO:
            case CMSMediaItem.CONTENT_TYPE_AUDIO:
                boolean useContentApi = DataManager.getInstance().getConfiguration().isUseIIIFApiUrlForCmsMediaUrls();
                Optional<AbstractApiUrlManager> urls;
                if (useContentApi) {
                    urls = DataManager.getInstance().getRestApiManager().getContentApiManager();
                } else {
                    urls = DataManager.getInstance().getRestApiManager().getDataApiManager();
                }

                boolean legacyApi = !urls.isPresent();
                if (legacyApi) {
                    String baseUrl = useContentApi ? DataManager.getInstance().getRestApiManager().getContentApiUrl()
                            : DataManager.getInstance().getRestApiManager().getDataApiUrl();
                    URI uri = URI.create(baseUrl + "cms/media/get/"
                            + getMediaItem().getId() + ".pdf");
                    return uri.toString();
                }
                String filename = getMediaItem().getFileName();
                filename = URLEncoder.encode(filename, "utf-8");
                return urls.get().path(ApiUrls.CMS_MEDIA, ApiUrls.CMS_MEDIA_FILES_FILE).params(filename).build();

            default:
                // Images
                contentString = CmsMediaBean.getMediaUrl(getMediaItem(), width, height);
        }
        return contentString;
    }

    @Override
    public CMSContent copy() {
        return new CMSMediaContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        if (StringUtils.isEmpty(outputFolderPath)) {
            throw new IllegalArgumentException("hotfolderPath may not be null or emptys");
        }
        if (StringUtils.isEmpty(namingScheme)) {
            throw new IllegalArgumentException("namingScheme may not be null or empty");
        }
        if (this.mediaItem == null || !mediaItem.isHasExportableText()) {
            return Collections.emptyList();
        }

        List<File> ret = new ArrayList<>();
        Path cmsDataDir = Paths.get(outputFolderPath, namingScheme + IndexerTools.SUFFIX_CMS);
        if (!Files.isDirectory(cmsDataDir)) {
            Files.createDirectory(cmsDataDir);
        }

        // Export media item HTML content
        String html = CmsMediaBean.getMediaFileAsString(mediaItem);
        if (StringUtils.isNotEmpty(html)) {
            File file = new File(cmsDataDir.toFile(), this.getId() + "-" + this.mediaItem.getId() + ".html");
            FileUtils.writeStringToFile(file, html, StringTools.DEFAULT_ENCODING);
            ret.add(file);
        }

        return ret;
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        return null;
    }

    /**
     * <p>
     * getMediaName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMediaName() {
        CMSMediaItemMetadata metadata = getMediaMetadata();
        return metadata == null ? "" : metadata.getName();
    }

    /**
     * <p>
     * getMediaDescription.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMediaDescription() {
        CMSMediaItemMetadata metadata = getMediaMetadata();
        return metadata == null ? "" : metadata.getDescription();
    }

    /**
     * <p>
     * getMediaMetadata.
     * </p>
     *
     * @return The media item metadata object of the current language associated with the contentItem with the given itemId. May return null if no
     *         such item exists
     */
    public CMSMediaItemMetadata getMediaMetadata() {

        if (getMediaItem() != null) {
            return getMediaItem().getCurrentLanguageMetadata();
        }
        return null;
    }

    @Override
    public String getData(Integer w, Integer h) {
        try {
            return getUrl(Optional.ofNullable(w).map(Object::toString).orElse(null), Optional.ofNullable(h).map(Object::toString).orElse(null));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error loading media item url for media item {}. Reason: {}", this.mediaItem, e.toString());
            return "";
        }
    }

    @Override
    public boolean isEmpty() {
        return Optional.ofNullable(mediaItem).map(media -> StringUtils.isBlank(media.getFileName())).orElse(true);
    }

    @Override
    public int hashCode() {
        if (this.getId() == null) {
            return 0;
        }
        return this.getId().intValue();
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 != null && this.getClass().equals(arg0.getClass())) {
            return Objects.equals(this.getId(), ((CMSMediaContent) arg0).getId());
        }
        return false;
    }

    @Override
    public int compareTo(CMSMediaContent arg0) {
        return Integer.compare(this.getMediaItem().getDisplayOrder(), arg0.getMediaItem().getDisplayOrder());
    }

}
