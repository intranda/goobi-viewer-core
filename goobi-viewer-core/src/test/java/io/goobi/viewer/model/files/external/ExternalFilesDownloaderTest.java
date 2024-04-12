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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import io.goobi.viewer.TestServlet;

class ExternalFilesDownloaderTest {

    private final Path testZipFile = Path.of("src/test/resources/data/viewer/external-files/1287088031.zip");
    private final TestServlet server = new TestServlet("127.0.0.1", 9191);

    @Test
    void test(@TempDir Path downloadFolder) throws IOException, InterruptedException, ExecutionException, TimeoutException {

        byte[] body = Files.readAllBytes(testZipFile);
        server.getServerClient()
                .when(HttpRequest.request().withPath("/exteral/files/1287088031.zip"))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", "application/zip")).withBody(body));

        assertTrue(Files.isDirectory(downloadFolder) || Files.createDirectory(downloadFolder) != null);
        @SuppressWarnings("unchecked")
        Consumer<Progress> consumer = (Consumer<Progress>) Mockito.spy(Consumer.class);
        URI uri = URI.create("http://127.0.0.1:9191/exteral/files/1287088031.zip");
        ExternalFilesDownloader download = new ExternalFilesDownloader(downloadFolder, consumer);
        Path downloadPath = download.downloadExternalFiles(uri);
        Mockito.verify(consumer, Mockito.atLeast(2)).accept(Mockito.any(Progress.class));
        assertTrue(Files.exists(downloadPath));
    }

}
