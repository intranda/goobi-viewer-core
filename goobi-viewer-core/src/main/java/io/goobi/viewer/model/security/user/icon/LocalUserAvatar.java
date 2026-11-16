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
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.http.HttpServletRequest;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.authentication.UserAvatarResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.security.user.User;

/**
 * @author Florian Alpers
 */
public class LocalUserAvatar implements UserAvatar {

    private static final Logger logger = LogManager.getLogger(LocalUserAvatar.class);

    private final Long userId;
    private final Long updated;

    public LocalUserAvatar(User user) {
        this.userId = user.getId();
        this.updated = user.getLocalAvatarUpdated();

    }

    /**
     * @should return avatar api url if image uploaded
     * @should return default avatar url if no image uploaded
     */
    @Override
    public String getIconUrl(int size, HttpServletRequest request) {
        // the REST path serves an uploaded file only; without one it answers 404 and the browser
        // renders a broken image, so show the placeholder the "default" option uses
        Optional<Path> avatarFile = getAvatarFile();
        if (avatarFile.isEmpty()) {
            return new DefaultUserAvatar().getIconUrl(size, request);
        }

        return DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> getImageUrl(urls, size, getDisplayFormat(avatarFile.get())))
                .filter(StringUtils::isNotEmpty)
                .orElseGet(() -> new DefaultUserAvatar().getIconUrl(size, request));
    }

    private Optional<Path> getAvatarFile() {
        if (this.userId == null) {
            return Optional.empty();
        }
        try {
            return UserAvatarResource.getUserAvatarFile(this.userId);
        } catch (IOException e) {
            logger.error("Error reading local avatar file of user {}: {}", this.userId, e.toString());
            return Optional.empty();
        }
    }

    private static String getDisplayFormat(Path avatarFile) {
        String suffix = FilenameUtils.getExtension(avatarFile.getFileName().toString());
        ImageFileFormat fileFormat = ImageFileFormat.getImageFileFormatFromFileExtension(suffix);

        return fileFormat != null ? ImageFileFormat.getMatchingTargetFormat(fileFormat).getFileExtension() : suffix;
    }

    private String getImageUrl(AbstractApiUrlManager urls, int size, String displayFormat) {
        return urls.path(USERS_USER_AVATAR_IMAGE, USERS_USER_AVATAR_IMAGE_IIIF)
                .params(this.userId, Region.FULL_IMAGE, "!" + size + "," + size, 0, "default", displayFormat)
                .query("updated", this.updated)
                .build();
    }
}
