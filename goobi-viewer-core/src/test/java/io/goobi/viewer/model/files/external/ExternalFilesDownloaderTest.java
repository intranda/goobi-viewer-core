package io.goobi.viewer.model.files.external;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import io.goobi.viewer.TestServlet;

public class ExternalFilesDownloaderTest {

    private final Path testZipFile = Path.of("src/test/resources/data/viewer/external-files/1287088031.zip");
    private final Path downloadFolder = Path.of("src/test/resources/output/external-files");
    private final TestServlet server = new TestServlet("127.0.0.1", 9191);

    @Test
    public void test() throws IOException, InterruptedException, ExecutionException, TimeoutException {

        byte[] body = Files.readAllBytes(testZipFile);
        server.getServerClient()
                .when(HttpRequest.request().withPath("/exteral/files/1287088031.zip"))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", "application/zip")).withBody(body));

        assertTrue(Files.isDirectory(downloadFolder) || Files.createDirectory(downloadFolder) != null);
        //        URI uri = testZipFile.toAbsolutePath().toUri();
        //        URI uri = URI.create("https://d-nb.info/1287088031/34");
//                URI uri = URI.create("https://www.splittermond.de/wp-content/uploads/2016/01/Splittermond_GRW_erratiert.pdf");
            Consumer consumer = Mockito.spy(Consumer.class);
            URI uri = URI.create("http://127.0.0.1:9191/exteral/files/1287088031.zip");
            ExternalFilesDownloader download = new ExternalFilesDownloader(downloadFolder, consumer);
            Path downloadPath = download.downloadExternalFiles(uri);
            Mockito.verify(consumer, Mockito.atLeast(2)).accept(Mockito.any(Progress.class));
            assertTrue(Files.exists(downloadPath));
    }

    @After
    public void after() throws Exception {
        server.shutdown();
        if (Files.exists(downloadFolder)) {
            FileUtils.deleteDirectory(downloadFolder.toFile());
        }
    }

}
