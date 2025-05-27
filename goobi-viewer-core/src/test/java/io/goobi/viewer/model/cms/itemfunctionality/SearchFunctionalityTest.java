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
package io.goobi.viewer.model.cms.itemfunctionality;

import java.net.URI;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.solr.SolrTools;

class SearchFunctionalityTest extends AbstractSolrEnabledTest {

    /**
     * @see SolrTools#getFieldValueMap(SolrDocument)
     * @verifies include five parameters
     */
    @Test
    void getParameterPath_shouldIncludeFiveParameters() {
        SearchFunctionality sf = new SearchFunctionality(null, "https://example.com/search/");
        sf.setSearchBean(new SearchBean());
        sf.setActiveContext("monographs"); // Must be a configured result group
        sf.setQueryString("FOO:bar");
        sf.setPageNo(2);
        sf.setSortString("RELEVANCE");
        sf.setFacetString("DC:varia");

        URI uri = sf.getParameterPath();
        Assertions.assertNotNull(uri);
        Assertions.assertEquals("monographs/FOO:bar/2/RELEVANCE/DC:varia;;", uri.getPath());
    }
    
    /**
     * @see SolrTools#getUrlPrefix()
     * @verifies construct url prefix correctly
     */
    @Test
    void getUrlPrefix_shouldConstructUrlPrefixCorrectly() {
        SearchFunctionality sf = new SearchFunctionality(null, "https://example.com/search/");
        sf.setSearchBean(new SearchBean());
        sf.setActiveContext("monographs"); // Must be a configured result group
        sf.setQueryString("FOO:bar");
        sf.setPageNo(2);

        String result = sf.getUrlPrefix();
        Assertions.assertEquals("https://example.com/search/monographs/FOO%3Abar/", result);
    }
}
