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
package io.goobi.viewer;

import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrSearchIndex;

/**
 * JUnit test classes that extend this class can use the embedded Solr server setup with a fixed viewer index.
 */
public abstract class AbstractSolrEnabledTest extends AbstractTest {

    protected static final String PI_KLEIUNIV = "PPN517154005";
    protected static long iddocKleiuniv = -1;

    private HttpSolrClient server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractTest.setUpClass();

    }

    @Before
    public void setUp() throws Exception {
        server = SolrSearchIndex.getNewHttpSolrClient();
        DataManager.getInstance().injectSearchIndex(new SolrSearchIndex(server));

        // Load current IDDOC for PPN517154005, which is used in many tests
        if (iddocKleiuniv == -1) {
            iddocKleiuniv = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(PI_KLEIUNIV);
        }
        Assert.assertNotEquals(0, iddocKleiuniv);
    }

    @After
    public void tearDown() throws Exception {
        server.close();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
}