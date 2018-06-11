package de.intranda.digiverso.presentation.servlets.rest.content;

import javax.servlet.http.HttpServletRequest;

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

    /**
     * @see ContentResource#getAltoDocument(String,String)
     * @verifies throw ContentNotFoundException if file not found
     */
    @Test(expected = ContentNotFoundException.class)
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
    @Test(expected = ServiceNotAllowedException.class)
    public void getTeiDocument_shouldThrowContentNotFoundExceptionIfFileNotFound() throws Exception {
        resource.getTeiDocument("notfound", "en");
    }
}