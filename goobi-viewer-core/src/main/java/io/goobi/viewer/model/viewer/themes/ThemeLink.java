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
package io.goobi.viewer.model.viewer.themes;

import org.apache.commons.lang3.StringUtils;

/**
 * @author florian
 *
 */
public class ThemeLink {

    public static interface Service {
        public String getLabelKey();

        public String getHelpKey();

        public String getInternalName();
    }

    public static enum InternalService implements Service {
        contact("admin__themes__footer_contact__label", "admin__themes__footer_contact__help"),
        legal("admin__themes__footer_legal__label", "admin__themes__footer_legal__help"),
        privacy("admin__themes__footer_privacy__label", "admin__themes__footer_privacy__help");

        private String labelKey;
        private String helpKey;

        private InternalService(String labelKey, String helpKey) {
            this.labelKey = labelKey;
            this.helpKey = helpKey;
        }

        public String getLabelKey() {
            return labelKey;
        }

        public String getHelpKey() {
            return helpKey;
        }

        public String getInternalName() {
            return name();
        }
    }

    public static enum SocialMediaService implements Service {
        github("admin__themes__social_media_github__label", "admin__themes__social_media_github__help"),
        twitter("admin__themes__social_media_twitter__label", "admin__themes__social_media_twitter__help"),
        youtube("admin__themes__social_media_youtube__label", "admin__themes__social_media_youtube__help"),
        slideshare("admin__themes__social_media_slideshare__label", "admin__themes__social_media_slideshare__help"),
        facebook("admin__themes__social_media_facebook__label", "admin__themes__social_media_facebook__help"),
        instagram("admin__themes__social_media_instagram__label", "admin__themes__social_media_instagram__help");

        private String labelKey;

        private String helpKey;

        private SocialMediaService(String labelKey, String helpKey) {
            this.labelKey = labelKey;
            this.helpKey = helpKey;
        }

        public String getLabelKey() {
            return labelKey;
        }

        public String getHelpKey() {
            return helpKey;
        }

        @Override
        public String getInternalName() {
            return name();
        }
    }

    private final Service service;
    private String linkUrl;

    public ThemeLink(Service service) {
        this.service = service;
    }

    public ThemeLink(Service service, String linkUrl) {
        this(service);
        this.linkUrl = linkUrl;
    }

    public Service getService() {
        return service;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String url) {
        this.linkUrl = url;
    }

    public boolean hasLink() {
        return StringUtils.isNotBlank(linkUrl);
    }

    @Override
    public String toString() {
        return service.getInternalName() + ": " + getLinkUrl();
    }
}
