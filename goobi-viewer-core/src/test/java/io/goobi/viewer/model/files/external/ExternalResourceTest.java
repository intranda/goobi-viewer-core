package io.goobi.viewer.model.files.external;


import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockitoSession;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.Test;
import org.mockito.Mockito;


public class ExternalResourceTest {

    private final URI externalResourceUri = URI.create("https://d-nb.info/1287088031/34");
    private final Path downloadFolder = Path.of("src/test/resources/output/external-files");

    @Test
    public void testCheckExistance() {
        Consumer consumer = Mockito.spy(Consumer.class);
        ExternalResource resource = new ExternalResource(externalResourceUri, new ExternalFilesDownloader(downloadFolder, consumer));
        Mockito.verify(consumer, Mockito.never()).accept(Mockito.anyLong());
        assertTrue(resource.exists());
    }
    
    @Test
    public void testDownload() throws IOException {
        Consumer consumer = Mockito.spy(Consumer.class);
        ExternalResource resource = new ExternalResource(externalResourceUri, new ExternalFilesDownloader(downloadFolder, consumer));
        resource.downloadResource();
        Mockito.verify(consumer, Mockito.atLeast(2)).accept(Mockito.anyLong());
    }

}
