package io.goobi.viewer.model.files.external;


import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.junit.Test;


public class ExternalResourceTest {

    private final URI externalResourceUri = URI.create("https://d-nb.info/1287088031/34");
    private final Path downloadFolder = Path.of("src/test/resources/output/external-files");
    
    @Test
    public void testCheckExistance() {
        ExternalResource resource = new ExternalResource(externalResourceUri, new ExternalFilesDownloader(downloadFolder));
        assertTrue(resource.exists());
    }
    
    @Test
    public void testDownload() throws IOException {
        ExternalResource resource = new ExternalResource(externalResourceUri, new ExternalFilesDownloader(downloadFolder));
        resource.downloadResource();
        System.out.println("DONE");
    }

}
