package io.goobi.viewer.model.files.external;


import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

public class ExternalFilesDownloaderTest {
    
    private final Path testZipFile = Path.of("src/test/resources/data/viewer/external-files/1287088031.zip");
    private final Path downloadFolder = Path.of("src/test/resources/output/external-files");

    @Test
    public void test() throws IOException {
        assertTrue(Files.isDirectory(downloadFolder) || Files.createDirectory(downloadFolder) != null);
        URI uri = testZipFile.toAbsolutePath().toUri();
//        URI uri = URI.create("https://d-nb.info/1287088031/34");
        ExternalFilesDownloader download = new ExternalFilesDownloader(downloadFolder);
        Path downloaded = download.downloadExternalFiles(uri);
        assertTrue(Files.isDirectory(downloaded));
    }
    
    @After
    public void after() throws IOException {
        if(Files.exists(downloadFolder)) {
            FileUtils.deleteDirectory(downloadFolder.toFile());
        }
    }

}
