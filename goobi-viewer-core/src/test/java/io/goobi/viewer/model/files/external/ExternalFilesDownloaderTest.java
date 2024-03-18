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
            Consumer<Progress> consumer = (Consumer<Progress>)Mockito.spy(Consumer.class);
            URI uri = URI.create("http://127.0.0.1:9191/exteral/files/1287088031.zip");
            ExternalFilesDownloader download = new ExternalFilesDownloader(downloadFolder, consumer);
            Path downloadPath = download.downloadExternalFiles(uri);
            Mockito.verify(consumer, Mockito.atLeast(2)).accept(Mockito.any(Progress.class));
            assertTrue(Files.exists(downloadPath));
    }

}
