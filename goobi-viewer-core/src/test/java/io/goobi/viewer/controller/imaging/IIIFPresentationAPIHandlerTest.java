package io.goobi.viewer.controller.imaging;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.ConfigurationTest;
import io.goobi.viewer.controller.DataManager;

public class IIIFPresentationAPIHandlerTest extends AbstractTest {

    private static final String REST_API_URL = "http://localhost:8080/viewer/api/v1";
    
    private IIIFPresentationAPIHandler handler;
    private AbstractApiUrlManager urls;
    
    private String PI = "PI-SAMPLE";
    private String DC = "DC";
    private String COLLECTION = "sonstige.ocr";

    @Before
    public void setUp() throws Exception {
        this.urls = new ApiUrls();
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
        handler = new IIIFPresentationAPIHandler(urls, DataManager.getInstance().getConfiguration());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetManifestUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/manifest/", handler.getManifestUrl("PI-SAMPLE"));
    }

    @Test
    public void testGetCollectionUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/collections/DC/", handler.getCollectionUrl());

    }

    @Test
    public void testGetCollectionUrlString() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/collections/DC/", handler.getCollectionUrl("DC"));

    }

    @Test
    public void testGetCollectionUrlStringString() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/collections/DC/sonstige.ocr",
                handler.getCollectionUrl("DC", "sonstige.ocr"));

    }

    @Test
    public void testGetLayerUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/layers/FULLTEXT/",
                handler.getLayerUrl("PI-SAMPLE", "fulltext"));

    }

    @Test
    public void testGetAnnotationsUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/pages/12/annotations/",
                handler.getAnnotationsUrl("PI-SAMPLE", 12, "crowdsourcing"));

    }

    @Test
    public void testGetCanvasUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/pages/12/canvas/",
                handler.getCanvasUrl("PI-SAMPLE", 12));

    }

    @Test
    public void testGetRangeUrl() throws URISyntaxException {
        Assert.assertEquals(REST_API_URL + "/records/PI-SAMPLE/sections/LOG_0007/range/",
                handler.getRangeUrl("PI-SAMPLE", "LOG_0007"));

    }

}
