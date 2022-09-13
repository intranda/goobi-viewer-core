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
package io.goobi.viewer.model.cms.content;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Optional;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSMediaHolder;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;

public class CMSImage implements CMSContent, CMSMediaHolder {

    private static final String BACKEND_COMPONENT_NAME = "image";

    private CMSMediaItem mediaItem;

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
        return CmsMediaBean.getImageFilter();
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

    public String getUrl() throws UnsupportedEncodingException, ViewerConfigurationException {
        return getUrl(null, null);
    }

    public String getUrl(String width, String height) throws ViewerConfigurationException, UnsupportedEncodingException {

        String contentString = "";
        String type = getMediaItem() != null ? getMediaItem().getContentType() : "";
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

}
