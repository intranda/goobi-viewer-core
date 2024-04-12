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
package io.goobi.viewer.controller.imaging;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;

/**
 * Resolves urls audio or video files
 *
 * @author Florian Alpers
 */
public class MediaHandler {

    private static final String URL_TEMPLATE = "media/{mimeType}/{identifier}/{filename}";

    private AbstractApiUrlManager urls;

    public MediaHandler() {
        this.urls = DataManager.getInstance().getRestApiManager().getContentApiManager().orElse(null);
    }

    /**
     * <p>
     * Constructor for MediaHandler.
     * </p>
     *
     * @param urls
     */
    public MediaHandler(AbstractApiUrlManager urls) {
        this.urls = urls;
    }

    /**
     * Returns the url to the media object for the given pi and filename
     *
     * @param type The mime type to use
     * @param format
     * @param pi The pi of the requested work
     * @param filename The media filename
     * @return the url to the media file of the given pi and filename
     * @throws IllegalRequestException
     */
    public String getMediaUrl(String type, String format, String pi, String filename) throws IllegalRequestException {

        if (urls != null) {
            if (type.equalsIgnoreCase("audio")) {
                return urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_AUDIO).params(pi, format, filename).build();
            } else if (type.equalsIgnoreCase("video")) {
                return urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_VIDEO).params(pi, format, filename).build();
            } else {
                throw new IllegalRequestException("Unknown media type " + type);
            }
        }

        return DataManager.getInstance().getConfiguration().getIIIFApiUrl()
                + URL_TEMPLATE.replace("{mimeType}", type + "/" + format).replace("{identifier}", pi).replace("{filename}", filename);
    }
}
