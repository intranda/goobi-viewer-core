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
package io.goobi.viewer.model.security.user.icon;

import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_USER_AVATAR_IMAGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_USER_AVATAR_IMAGE_IIIF;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.authentication.UserAvatarResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.security.user.User;

/**
 * @author florian
 *
 */
public class LocalUserAvatar implements UserAvatar {

    private final Long userId;
    private final Long updated;

    public LocalUserAvatar(User user) {
        this.userId = user.getId();
        this.updated = user.getLocalAvatarUpdated();

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.user.icon.IconProvider#getIconUrl(int, jakarta.servlet.http.HttpServletRequest)
     */
    @Override
    public String getIconUrl(int size, HttpServletRequest request) {
        return DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> getImageUrl(urls, this.userId, size))
                .orElse("");

    }

    private String getImageUrl(AbstractApiUrlManager urls, Long userId, int size) {
        try {
            String sizeString = "!" + size + "," + size;
            String format = UserAvatarResource.getAvatarFileSuffix(userId);
            String displayFormat = format;
            ImageFileFormat fileFormat = ImageFileFormat.getImageFileFormatFromFileExtension(format);
            if (fileFormat != null) {
                displayFormat = ImageFileFormat.getMatchingTargetFormat(fileFormat).getFileExtension();                
            }
            
            return urls.path(USERS_USER_AVATAR_IMAGE, USERS_USER_AVATAR_IMAGE_IIIF)
                    .params(userId, Region.FULL_IMAGE, sizeString, 0, "default", displayFormat)
                    .query("updated", this.updated)
                    //                    .query("timestamp", System.currentTimeMillis())
                    .build();
        } catch (IOException e) {
            return "";
        }
    }
}
