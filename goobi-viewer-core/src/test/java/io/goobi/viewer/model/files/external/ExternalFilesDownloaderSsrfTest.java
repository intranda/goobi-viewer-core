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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.goobi.viewer.AbstractTest;

class ExternalFilesDownloaderSsrfTest extends AbstractTest {

    private static final URI PRIVATE_URI = URI.create("http://192.168.0.1/resource");

    /**
     * @verifies throw IOException for private network address
     */
    @Test
    void downloadExternalFiles_shouldThrowIOExceptionForPrivateNetworkAddress(@TempDir Path downloadFolder) {
        ExternalFilesDownloader downloader = new ExternalFilesDownloader(downloadFolder, progress -> {});
        IOException ex = assertThrows(IOException.class,
                () -> downloader.downloadExternalFiles(PRIVATE_URI, Map.of()));
        assertTrue(ex.getMessage().contains("blocked") || ex.getMessage().contains("rejected"),
                "Expected SSRF-blocked message, got: " + ex.getMessage());
    }

    /**
     * @verifies return false for private network address
     */
    @Test
    void resourceExists_shouldReturnFalseForPrivateNetworkAddress() {
        assertFalse(ExternalFilesDownloader.resourceExists(PRIVATE_URI, Map.of()));
    }

}
