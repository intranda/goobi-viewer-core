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
package io.goobi.viewer.model.files.external;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

public class ExternalResource {

    private final URI exteralResourceUri;
    private final ExternalFilesDownloader downloader;
    private final boolean exists;

    public ExternalResource(URI exteralResourceUri, ExternalFilesDownloader downloader) {
        this.exteralResourceUri = exteralResourceUri;
        this.downloader = downloader;
        this.exists = checkExistance();
    }

    private boolean checkExistance() {
        return downloader.resourceExists(exteralResourceUri);
    }

    public boolean exists() {
        return exists;
    }

    public Future<ExternalResource> downloadResource() throws IOException {
        this.downloader.downloadExternalFiles(exteralResourceUri);
        return null;
    }

}
