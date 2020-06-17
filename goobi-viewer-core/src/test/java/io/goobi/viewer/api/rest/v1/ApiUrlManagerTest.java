/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.v1;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author florian
 *
 */
public class ApiUrlManagerTest {

    private final static String PATH_PATTERN = "/records/{pi}/images/{filename}/{title}.pdf";
    private final static String PI = "12345";
    private final static String FILENAME = "00000001.tif";
    private final static String TITLE = "output";
    private final static String HOST_URL = "https://viewer/api/v1/";
    private final static String PATH_FINAL = "https://viewer/api/v1/records/12345/images/00000001.tif/output.pdf";
    private final static String PATH_FINAL_QUERIES = "https://viewer/api/v1/records/12345/images/00000001.tif/output.pdf?max=1&author=Mia Mustermann&watermarkText=Hosting url: http://sample.org";

    private final static String QUERY_PARAM_MAX = "max";
    private final static String QUERY_PARAM_MAX_VALUE = "1";
    private final static String QUERY_PARAM_AUTHOR = "author";
    private final static String QUERY_PARAM_AUTHOR_VALUE = "Mia Mustermann";
    private final static String QUERY_PARAM_TEXT = "watermarkText";
    private final static String QUERY_PARAM_TEXT_VALUE = "Hosting url: http://sample.org";
    
    private ApiUrlManager manager;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.manager = new ApiUrlManager(HOST_URL);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetUrl() {
        String url = manager.getUrl(PATH_PATTERN, PI, FILENAME, TITLE);
        assertEquals(PATH_FINAL, url);
    }
    
    @Test
    public void testGetUrlWithQuery() {
        Map<String, String> queries = new LinkedHashMap<>();
        queries.put(QUERY_PARAM_MAX, QUERY_PARAM_MAX_VALUE);
        queries.put(QUERY_PARAM_AUTHOR, QUERY_PARAM_AUTHOR_VALUE);
        queries.put(QUERY_PARAM_TEXT, QUERY_PARAM_TEXT_VALUE);
        String url = manager.getUrl(PATH_PATTERN, queries, PI, FILENAME, TITLE);
        assertEquals(PATH_FINAL_QUERIES, url);
    }

}
