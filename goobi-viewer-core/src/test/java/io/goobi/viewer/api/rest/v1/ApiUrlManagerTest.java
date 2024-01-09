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
package io.goobi.viewer.api.rest.v1;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

/**
 * @author florian
 *
 */
public class ApiUrlManagerTest {

    private final static String PI = "12345";
    private final static String DIVID = "LOG_0003";
    private final static String HOST_URL = "https://viewer/api/v1/";
    private final static String PATH_FINAL = "https://viewer/api/v1/records/12345/sections/LOG_0003/pdf/";
    private final static String PATH_FINAL_QUERIES = "https://viewer/api/v1/records/12345/sections/LOG_0003/pdf/?max=1&author=Mia+Mustermann&watermarkText=Hosting+url%3A+http%3A%2F%2Fsample.org";

    private final static String QUERY_PARAM_MAX = "max";
    private final static String QUERY_PARAM_MAX_VALUE = "1";
    private final static String QUERY_PARAM_AUTHOR = "author";
    private final static String QUERY_PARAM_AUTHOR_VALUE = "Mia Mustermann";
    private final static String QUERY_PARAM_TEXT = "watermarkText";
    private final static String QUERY_PARAM_TEXT_VALUE = "Hosting url: http://sample.org";

    private ApiUrls manager;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        this.manager = new ApiUrls(HOST_URL);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    void testGetUrl() {
        String url = manager.path(RECORDS_SECTIONS, RECORDS_SECTIONS_PDF).params(PI, DIVID).build();
        assertEquals(PATH_FINAL, url);
    }

    @Test
    void testGetUrlWithQuery() {
        String url = manager.path(RECORDS_SECTIONS, RECORDS_SECTIONS_PDF)
                .params(PI, DIVID)
                .query(QUERY_PARAM_MAX, QUERY_PARAM_MAX_VALUE)
                .query(QUERY_PARAM_AUTHOR, QUERY_PARAM_AUTHOR_VALUE)
                .query(QUERY_PARAM_TEXT, QUERY_PARAM_TEXT_VALUE)
                .build();
        assertEquals(PATH_FINAL_QUERIES, url);
    }

    @Test
    void testParseParameter() {
        String pi = "PPN1234";
        String pageNo = "5";
        String id = "172";
        String template = manager.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS_COMMENT).build();
        String url = manager.path(RECORDS_PAGES, RECORDS_PAGES_COMMENTS_COMMENT).params(pi, pageNo, id).build();

        assertEquals(pi, manager.parseParameter(template, url, "pi"));
        assertEquals(pageNo, manager.parseParameter(template, url, "pageNo"));
        assertEquals(id, manager.parseParameter(template, url, "id"));
    }

}
