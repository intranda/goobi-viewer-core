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
package io.goobi.viewer.model.export;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.solr.SolrConstants;

class RISExportTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see RISExport#RISExport()
     * @verifies set fileName correctly
     */
    @Test
    void RISExport_shouldSetFileNameCorrectly() throws Exception {
        RISExport export = new RISExport();
        Assertions.assertNotNull(export.getFileName());
    }

    /**
     * @see RISExport#executeSearch(String,String,List,List,Map,Map,Locale,int,HttpServletRequest,HttpServletResponse)
     * @verifies execute search correctly
     */
    @Test
    void executeSearch_shouldExecuteSearchCorrectly() throws Exception {
        RISExport export = new RISExport();
        export.executeSearch(SolrConstants.PI + ":" + PI_KLEIUNIV, null, null, null, null, Locale.ENGLISH, 0);
        Assertions.assertEquals(1, export.getSearchHits().size());
    }

    /**
     * @see RISExport#isHasResults()
     * @verifies return correct value
     */
    @Test
    void isHasResults_shouldReturnCorrectValue() throws Exception {
        RISExport export = new RISExport();
        Assertions.assertFalse(export.isHasResults());
        export.executeSearch(SolrConstants.PI + ":" + PI_KLEIUNIV, null, null, null, null, Locale.ENGLISH, 0);
        Assertions.assertTrue(export.isHasResults());
    }
}