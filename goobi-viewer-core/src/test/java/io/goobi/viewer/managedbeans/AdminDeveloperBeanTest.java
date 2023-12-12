package io.goobi.viewer.managedbeans;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.CronExpression;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.XmlTools;


public class AdminDeveloperBeanTest {

    Configuration config;
    AdminDeveloperBean bean;
    Path configPath = Path.of("src/test/resources/localConfig");
    Path zipPath = Path.of("src/test/resources/output/viewer_dump");

    @Before
    public void setup() {
        config = Mockito.mock(Configuration.class);
        Mockito.when(config.getTheme()).thenReturn("reference");
        Mockito.when(config.getConfigLocalPath()).thenReturn(configPath.toAbsolutePath().toString());
        bean = new AdminDeveloperBean(config);
    }
    
    @Test
    public void test_createZipFile() throws IOException, InterruptedException, JDOMException {
        Path zipFile = bean.createDeveloperArchive(zipPath);
        assertTrue(Files.exists(zipPath));
        
    }
    
    @Test
    public void test_createConfigDocument() throws IOException, JDOMException {
        Path viewerConfigPath = Path.of("src/test/resources/config_viewer_developer.xml");
        Document doc = bean.createDeveloperViewerConfig(viewerConfigPath);
        assertEquals("https://example.com/solr/collection2", XmlTools.evaluateToFirstString("//config/urls/solr", doc, Collections.emptyList()).orElse(""));
        assertEquals("https://example.com/viewer/api/v1/", XmlTools.evaluateToFirstString("//config/urls/iiif", doc, Collections.emptyList()).orElse(""));
        assertEquals("true", XmlTools.evaluateToFirstAttributeString("//config/urls/iiif/@useForCmsMedia", doc, Collections.emptyList()).orElse(""));
        assertEquals("http://localhost:8080/viewer/api/v1/", XmlTools.evaluateToFirstString("//config/urls/rest", doc, Collections.emptyList()).orElse(""));

    }

    @After
    public void after() throws IOException {
        if(Files.isDirectory(zipPath)) {
            FileUtils.cleanDirectory(zipPath.toFile());
        } else {
            Files.createDirectories(zipPath);
        }
    }
}
