package io.goobi.viewer.model.job.download;

import java.awt.Dimension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DownloadOptionTest {

    @Test
    void test() {
        Assertions.assertEquals("jpg", new DownloadOption("small", "jpg", new Dimension(15, 15)).getExtension("00000001.tif"));
        Assertions.assertEquals("tif", new DownloadOption("small", "master", new Dimension(15, 15)).getExtension("00000001.tif"));
        Assertions.assertEquals("png", new DownloadOption("small", "master", new Dimension(15, 15)).getExtension("00000001.png"));
        Assertions.assertEquals("jpg", new DownloadOption("small", "jpeg", new Dimension(15, 15)).getExtension("00000001.png"));
    }

}
