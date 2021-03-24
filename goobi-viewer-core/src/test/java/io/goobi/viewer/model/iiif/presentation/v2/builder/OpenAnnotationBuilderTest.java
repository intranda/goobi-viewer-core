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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import static org.junit.Assert.*;

import org.apache.solr.common.SolrDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.iiif.presentation.v2.builder.OpenAnnotationBuilder;

/**
 * @author florian
 *
 */
public class OpenAnnotationBuilderTest {

    final static String PI = "1234";
    final static Integer PAGENO = 27;
    final static String FRAGMENT = "xywh=742,2230,2322,342";
    

    OpenAnnotationBuilder builder;
    SolrDocument solrDocument;

    @BeforeClass
    public static void setUpClass() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
    }

    @Before
    public void SetUp() {
        builder = new OpenAnnotationBuilder(new ApiUrls("http://localhost:8080/viewer/rest")) {
        };
    }
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
    }

}
