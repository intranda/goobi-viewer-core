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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class ExternalResourceTest {

    private final URI externalResourceUri = URI.create("https://d-nb.info/1287088031/34");

    @Test
    void testCheckExistance(@TempDir Path downloadFolder) {
        @SuppressWarnings("unchecked")
        Consumer<Progress> consumer = (Consumer<Progress>) Mockito.spy(Consumer.class);
        ExternalResource resource = new ExternalResource(externalResourceUri, new ExternalFilesDownloader(downloadFolder, consumer));
        Mockito.verify(consumer, Mockito.never()).accept(Mockito.any(Progress.class));
        assertTrue(resource.exists());
    }

    @Test
    void testDownload(@TempDir Path downloadFolder) throws IOException {
        @SuppressWarnings("unchecked")
        Consumer<Progress> consumer = (Consumer<Progress>) Mockito.spy(Consumer.class);
        ExternalResource resource = new ExternalResource(externalResourceUri, new ExternalFilesDownloader(downloadFolder, consumer));
        resource.downloadResource();
        Mockito.verify(consumer, Mockito.atLeast(2)).accept(Mockito.any(Progress.class));
    }

}
