package io.goobi.viewer.model.files.external;


import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class ExternalResourceTest {

    private final URI externalResourceUri = URI.create("https://d-nb.info/1287088031/34");
    private final Path downloadFolder = Path.of("src/test/resources/output/external-files");

    @Test
    void testCheckExistance() {
        Consumer consumer = Mockito.spy(Consumer.class);
        ExternalResource resource = new ExternalResource(externalResourceUri, new ExternalFilesDownloader(downloadFolder, consumer));
        Mockito.verify(consumer, Mockito.never()).accept(Mockito.any(Progress.class));
        assertTrue(resource.exists());
    }
    
    @Test
    void testDownload() throws IOException {
        Consumer consumer = Mockito.spy(Consumer.class);
        ExternalResource resource = new ExternalResource(externalResourceUri, new ExternalFilesDownloader(downloadFolder, consumer));
        resource.downloadResource();
        Mockito.verify(consumer, Mockito.atLeast(2)).accept(Mockito.any(Progress.class));
    }

}
