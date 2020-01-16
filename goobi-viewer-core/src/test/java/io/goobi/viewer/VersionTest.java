package io.goobi.viewer;

import org.junit.Assert;
import org.junit.Test;

public class VersionTest {

    final static String MANIFEST = "Manifest-Version: 1.0\r\n" + "Public-Version: 20.01\r\n" + "Implementation-Version: 6d948ec\r\n"
            + "Built-By: root\r\n" + "version: 4.3.0-SNAPSHOT\r\n" + "Created-By: Apache Maven\r\n" + "Build-Jdk: 1.8.0_232\r\n"
            + "ApplicationName: goobi-viewer-core\r\n" + "Implementation-Build-Date: 2020-01-15 18:30";

    /**
     * @see Version#getInfo(String,String)
     * @verifies extract fields correctly
     */
    @Test
    public void getInfo_shouldExtractFieldsCorrectly() throws Exception {
        Assert.assertEquals("20.01", Version.getInfo("Public-Version", MANIFEST));
        Assert.assertEquals("Apache Maven", Version.getInfo("Created-By", MANIFEST));
        Assert.assertEquals("2020-01-15 18:30", Version.getInfo("Implementation-Build-Date", MANIFEST));
    }
}