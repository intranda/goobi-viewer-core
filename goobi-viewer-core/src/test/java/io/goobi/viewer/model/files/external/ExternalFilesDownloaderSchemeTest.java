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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalFilesDownloaderSchemeTest {

    /**
     * @verifies throw IOException for file scheme URI
     */
    @Test
    void downloadExternalFiles_shouldThrowIOExceptionForFileSchemeUri(@TempDir Path downloadFolder) {
        ExternalFilesDownloader downloader = new ExternalFilesDownloader(downloadFolder, progress -> {});
        assertThrows(IOException.class, () -> downloader.downloadExternalFiles(URI.create("file:///etc/passwd"), Map.of()));
    }

    /**
     * @verifies return false for file scheme URI
     */
    @Test
    void resourceExists_shouldReturnFalseForFileSchemeUri() {
        assertFalse(ExternalFilesDownloader.resourceExists(URI.create("file:///etc/passwd"), Map.of()));
    }

}
