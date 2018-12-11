package de.intranda.digiverso.presentation.servlets.rest.content;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.TestUtils;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;

public class ContentResourceTest extends AbstractDatabaseAndSolrEnabledTest {

    private ContentResource resource;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration configuration = new Configuration("resources/test/config_viewer.test.xml");
        DataManager.getInstance().injectConfiguration(configuration);

        HttpServletRequest request = TestUtils.mockHttpRequest();
        resource = new ContentResource(request);
    }

    /**
     * @see ContentResource#getAltoDocument(String,String)
     * @verifies return document correctly
     */
    @Test
    public void getAltoDocument_shouldReturnDocumentCorrectly() throws Exception {
        Assert.assertNotNull(resource.getAltoDocument("PPN517154005", "00000001.xml"));
    }
    
    @Test
    public void getAltoDocumentZip() throws Exception {
        StreamingOutput response = resource.getAltoDocument("PPN517154005");
        Path tempFile = Paths.get("test.zip");
        if(Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        Assert.assertFalse(Files.exists(tempFile));
        try(OutputStream out = Files.newOutputStream(tempFile)) {            
            response.write(out);
            out.flush();
        }
        Assert.assertTrue(Files.exists(tempFile));
        Assert.assertTrue(isValid(tempFile.toFile()));
        Files.delete(tempFile);        
    }

    /**
     * @see ContentResource#getAltoDocument(String,String)
     * @verifies throw ContentNotFoundException if file not found
     */
    @Test(expected = ServiceNotAllowedException.class)
    public void getAltoDocument_shouldThrowContentNotFoundExceptionIfFileNotFound() throws Exception {
        resource.getAltoDocument("notfound", "00000001.xml");
    }

    /**
     * @see ContentResource#getContentDocument(String,String,String,String)
     * @verifies return document correctly
     */
    @Test
    public void getContentDocument_shouldReturnDocumentCorrectly() throws Exception {
        Assert.assertNotNull(resource.getContentDocument(null, "fulltext", "PPN517154005", "00000001.txt"));
    }

    /**
     * @see ContentResource#getContentDocument(String,String,String,String)
     * @verifies throw ContentNotFoundException if file not found
     */
    @Test(expected = ServiceNotAllowedException.class)
    public void getContentDocument_shouldThrowContentNotFoundExceptionIfFileNotFound() throws Exception {
        resource.getContentDocument(null, "foo", "notfound", "00000001.xml");
    }

    /**
     * @see ContentResource#getFulltextDocument(String,String)
     * @verifies return document correctly
     */
    @Test
    public void getFulltextDocument_shouldReturnDocumentCorrectly() throws Exception {
        Assert.assertNotNull(resource.getFulltextDocument("PPN517154005", "00000001.txt"));
    }

    /**
     * @see ContentResource#getFulltextDocument(String,String)
     * @verifies throw ContentNotFoundException if file not found
     */
    @Test(expected = ServiceNotAllowedException.class)
    public void getFulltextDocument_shouldThrowContentNotFoundExceptionIfFileNotFound() throws Exception {
        resource.getFulltextDocument("notfound", "00000001.txt");
    }

    /**
     * @see ContentResource#getTeiDocument(String,String)
     * @verifies return document correctly
     */
    @Test
    public void getTeiDocument_shouldReturnDocumentCorrectly() throws Exception {
        String tei = resource.getTeiDocument("DE_2013_Riedel_PolitikUndCo_241__248", "de");
        Assert.assertNotNull(tei);
        Assert.assertTrue(tei.contains("Religionen und Sinndeutungssysteme"));
    }

    /**
     * @see ContentResource#getTeiDocument(String,String)
     * @verifies throw ContentNotFoundException if file not found
     */
    @Test(expected = ContentNotFoundException.class)
    public void getTeiDocument_shouldThrowContentNotFoundExceptionIfFileNotFound() throws Exception {
        resource.getTeiDocument("notfound", "en");
    }
    
    public static boolean isValid(final File file) {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(file);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                    zipfile = null;
                }
            } catch (IOException e) {
            }
        }
    }
    
    @Test
    public void testGetFiles() throws IOException {
        
        Path tempPath = Paths.get("test", "data", "temp");
        try {
            Path folder1 = tempPath.resolve("folder1");
            Path folder2 = tempPath.resolve("folder2");
            
            Files.createDirectories(folder1);
            Files.createDirectories(folder2);
        
        createFile(folder1, "A");
        createFile(folder1, "B");
        createFile(folder2, "C");
        createFile(folder2, "D");
        createFile(folder1, "E");
        createFile(folder2, "E");
        createFile(folder1, "F");
        createFile(folder2, "G");
        createFile(folder1, "H");
        createFile(folder2, "H");
        
        List<Path> files = new ContentResource().getFiles(folder1, folder2, ".*\\.txt");
        List<String> content = files.stream()
                .sorted((p1,p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))
                .map(file -> {
            try {
                return Files.readAllLines(file);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }).map(list -> StringUtils.join(list, "")).collect(Collectors.toList());
        
        Assert.assertEquals(8, content.size(), 0);
        
        Assert.assertEquals("folder1/A", content.get(0));
        Assert.assertEquals("folder1/B", content.get(1));
        Assert.assertEquals("folder2/C", content.get(2));
        Assert.assertEquals("folder2/D", content.get(3));
        Assert.assertEquals("folder1/E", content.get(4));
        Assert.assertEquals("folder1/F", content.get(5));
        Assert.assertEquals("folder2/G", content.get(6));
        Assert.assertEquals("folder1/H", content.get(7));
        
        files = new ContentResource().getFiles(folder2, folder1, ".*\\.txt");
        content = files.stream()
                .sorted((p1,p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))
                .map(file -> {
            try {
                return Files.readAllLines(file);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }).map(list -> StringUtils.join(list, "")).collect(Collectors.toList());
        
        Assert.assertEquals(8, content.size(), 0);
        
        Assert.assertEquals("folder1/A", content.get(0));
        Assert.assertEquals("folder1/B", content.get(1));
        Assert.assertEquals("folder2/C", content.get(2));
        Assert.assertEquals("folder2/D", content.get(3));
        Assert.assertEquals("folder2/E", content.get(4));
        Assert.assertEquals("folder1/F", content.get(5));
        Assert.assertEquals("folder2/G", content.get(6));
        Assert.assertEquals("folder2/H", content.get(7));
        
        }finally {
            FileUtils.deleteDirectory(tempPath.toFile());
        }
    }

    /**
     * @param folder1
     * @param string
     * @throws IOException 
     */
    private void createFile(Path folder, String name) throws IOException {
        if(folder != null) {
            Files.write(folder.resolve(name + ".txt"), Collections.singletonList(folder.getFileName().toString() + "/" + name));
        }

    }
}