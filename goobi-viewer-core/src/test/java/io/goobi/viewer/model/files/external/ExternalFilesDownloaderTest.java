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
    
    private final Path testFolder = Path.of("src/test/resources/downloads");

    @Test
    public void test() throws IOException {
        assertTrue(Files.exists(testFolder) || Files.createDirectory(testFolder) != null);
        URI uri = URI.create("https://d-nb.info/1287088031/34");
//        NetTools.callUrlGET(uri.toString());
        ExternalFilesDownloader download = new ExternalFilesDownloader(testFolder);
        Path downloaded = download.downloadExternalFiles(uri);
        assertTrue(Files.isDirectory(downloaded));
    }
    
    @After
    public void after() throws IOException {
        if(Files.exists(testFolder)) {
            FileUtils.deleteDirectory(testFolder.toFile());
        }
    }

}
