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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.export.bagit.CollectionArchiveService;
import io.goobi.viewer.model.viewer.collections.BrowseDcElement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * View helper exposing per-collection BagIt archive availability and download URLs to the collection page. The collection page only renders
 * the download link when {@link UserBean#isLoggedIn()} is true; the download endpoint itself is additionally gated server-side.
 */
@Named
@ApplicationScoped
public class CollectionArchiveBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger(CollectionArchiveBean.class);

    /**
     * @return true if per-collection archive generation is enabled in the configuration
     */
    public boolean isEnabled() {
        return DataManager.getInstance().getConfiguration().isCollectionArchivesEnabled();
    }

    /**
     * @param element the collection element
     * @return true if a downloadable archive currently exists on disk for the given collection
     */
    public boolean isAvailable(BrowseDcElement element) {
        if (element == null || !isEnabled()) {
            return false;
        }
        try {
            return new CollectionArchiveService().findArchive(element.getField(), element.getName()).isPresent();
        } catch (IOException e) {
            logger.warn("Could not determine collection archive availability for {}:{}", element.getField(), element.getName(), e);
            return false;
        }
    }

    /**
     * Builds the download URL for a collection's archive. The URL is deterministic from field and collection name; whether a file actually
     * exists is decided by {@link #isAvailable(BrowseDcElement)} (used to gate rendering) and by the download endpoint itself.
     *
     * @param element the collection element
     * @return the absolute REST download URL, or an empty string if the API base is unavailable
     */
    public String getDownloadUrl(BrowseDcElement element) {
        if (element == null) {
            return "";
        }
        AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null);
        if (urls == null) {
            return "";
        }
        return urls.path(ApiUrls.COLLECTIONS_ARCHIVE, ApiUrls.COLLECTIONS_ARCHIVE_DOWNLOAD)
                .params(element.getField(), element.getName())
                .build();
    }
}
