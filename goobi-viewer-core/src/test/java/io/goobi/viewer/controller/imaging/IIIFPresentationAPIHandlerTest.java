/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller.imaging;

import java.net.URISyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;

class IIIFPresentationAPIHandlerTest extends AbstractTest {

    private static final String REST_API_URL = "http://localhost:8080/viewer/api/v1";

    private IIIFPresentationAPIHandler handler;
    private AbstractApiUrlManager urls;

    private String PI = "PI-SAMPLE";
    private String DC = "DC";
    private String COLLECTION = "sonstige.ocr";

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.urls = new ApiUrls();
        handler = new IIIFPresentationAPIHandler(urls, DataManager.getInstance().getConfiguration());
    }

    /**
     * @verifies return expected value for given input
     * @see IIIFPresentationAPIHandler#getManifestUrl(String)
     */
    @Test
    void getManifestUrl_shouldReturnExpectedValueForGivenInput() throws URISyntaxException {
        Assertions.assertEquals(REST_API_URL + "/records/PI-SAMPLE/manifest/", handler.getManifestUrl("PI-SAMPLE"));
    }

    /**
     * @verifies return expected value for given input
     * @see IIIFPresentationAPIHandler#getCollectionUrl
     */
    @Test
    void getCollectionUrl_shouldReturnExpectedValueForGivenInput() throws URISyntaxException {
        Assertions.assertEquals(REST_API_URL + "/collections/DC/", handler.getCollectionUrl());

    }

    /**
     * @verifies return correct url for single field param
     * @see IIIFPresentationAPIHandler#getCollectionUrl()
     */
    @Test
    void getCollectionUrl_shouldReturnCorrectUrlForSingleFieldParam() throws URISyntaxException {
        Assertions.assertEquals(REST_API_URL + "/collections/DC/", handler.getCollectionUrl("DC"));

    }

    /**
     * @verifies return correct url for field and collection params
     * @see IIIFPresentationAPIHandler#getCollectionUrl()
     */
    @Test
    void getCollectionUrl_shouldReturnCorrectUrlForFieldAndCollectionParams() throws URISyntaxException {
        Assertions.assertEquals(REST_API_URL + "/collections/DC/sonstige.ocr",
                handler.getCollectionUrl("DC", "sonstige.ocr"));

    }

    /**
     * @verifies return expected value for given input
     * @see IIIFPresentationAPIHandler#getLayerUrl(String, String)
     */
    @Test
    void getLayerUrl_shouldReturnExpectedValueForGivenInput() throws URISyntaxException {
        Assertions.assertEquals(REST_API_URL + "/records/PI-SAMPLE/layers/FULLTEXT/",
                handler.getLayerUrl("PI-SAMPLE", "fulltext"));

    }

    /**
     * @verifies return expected value for given input
     * @see IIIFPresentationAPIHandler#getAnnotationsUrl(String, int, String)
     */
    @Test
    void getAnnotationsUrl_shouldReturnExpectedValueForGivenInput() throws URISyntaxException {
        Assertions.assertEquals(REST_API_URL + "/records/PI-SAMPLE/pages/12/annotations/",
                handler.getAnnotationsUrl("PI-SAMPLE", 12, "crowdsourcing"));

    }

    /**
     * @verifies return expected value for given input
     * @see IIIFPresentationAPIHandler#getCanvasUrl(String, int)
     */
    @Test
    void getCanvasUrl_shouldReturnExpectedValueForGivenInput() throws URISyntaxException {
        Assertions.assertEquals(REST_API_URL + "/records/PI-SAMPLE/pages/12/canvas/",
                handler.getCanvasUrl("PI-SAMPLE", 12));

    }

    /**
     * @verifies return expected value for given input
     * @see IIIFPresentationAPIHandler#getRangeUrl(String, String)
     */
    @Test
    void getRangeUrl_shouldReturnExpectedValueForGivenInput() throws URISyntaxException {
        Assertions.assertEquals(REST_API_URL + "/records/PI-SAMPLE/sections/LOG_0007/range/",
                handler.getRangeUrl("PI-SAMPLE", "LOG_0007"));

    }

}
