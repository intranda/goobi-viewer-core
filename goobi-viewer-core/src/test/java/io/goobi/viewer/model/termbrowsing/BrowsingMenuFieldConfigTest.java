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
package io.goobi.viewer.model.termbrowsing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

public class BrowsingMenuFieldConfigTest {

    /**
     * @see BrowsingMenuFieldConfig#setDocstructFilterString(String)
     * @verifies create filter query correctly
     */
    @Test
    public void setDocstructFilterString_shouldCreateFilterQueryCorrectly() throws Exception {
        BrowsingMenuFieldConfig bmfc =
                new BrowsingMenuFieldConfig("MD_TITLE", "SORT_TITLE", "+(DOCSTRCT:monograph DOCSTRCT:manuscript)", false, false, false);
        Assertions.assertEquals(2, bmfc.getFilterQueries().size());
        Assertions.assertEquals("+(DOCSTRCT:monograph DOCSTRCT:manuscript)", bmfc.getFilterQueries().get(0));
    }

    /**
     * @see BrowsingMenuFieldConfig#setRecordsAndAnchorsOnly(boolean)
     * @verifies create filter query correctly
     */
    @Test
    public void setRecordsAndAnchorsOnly_shouldCreateFilterQueryCorrectly() throws Exception {
        BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig("MD_TITLE", "SORT_TITLE", null, false, true, false);
        Assertions.assertEquals(2, bmfc.getFilterQueries().size());
        Assertions.assertEquals(SearchHelper.ALL_RECORDS_QUERY, bmfc.getFilterQueries().get(0));
    }

    /**
     * @see BrowsingMenuFieldConfig#addDoctypeFilterQuery()
     * @verifies add doctype filter if field MD or MD2
     */
    @Test
    public void addDoctypeFilterQuery_shouldAddDoctypeFilterIfFieldMDOrMD2() throws Exception {
        {
            BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig("MD_FOO", null, null, false, false, false);
            Assertions.assertEquals(1, bmfc.getFilterQueries().size());
            Assertions.assertEquals("+" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name(), bmfc.getFilterQueries().get(0));
        }
        {
            BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig("MD2_FOO", null, null, false, false, false);
            Assertions.assertEquals(1, bmfc.getFilterQueries().size());
            Assertions.assertEquals("+" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name(), bmfc.getFilterQueries().get(0));
        }
    }

    /**
     * @see BrowsingMenuFieldConfig#addDoctypeFilterQuery()
     * @verifies add doctype filter if field DC
     */
    @Test
    public void addDoctypeFilterQuery_shouldAddDoctypeFilterIfFieldDC() throws Exception {
        BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig(SolrConstants.DC, null, null, false, false, false);
        Assertions.assertEquals(1, bmfc.getFilterQueries().size());
        Assertions.assertEquals("+" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name(), bmfc.getFilterQueries().get(0));
    }

    /**
     * @see BrowsingMenuFieldConfig#addDoctypeFilterQuery()
     * @verifies add doctype filter if field DOCSTRCT
     */
    @Test
    public void addDoctypeFilterQuery_shouldAddDoctypeFilterIfFieldDOCSTRCT() throws Exception {
        BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig(SolrConstants.DOCSTRCT, null, null, false, false, false);
        Assertions.assertEquals(1, bmfc.getFilterQueries().size());
        Assertions.assertEquals("+" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name(), bmfc.getFilterQueries().get(0));
    }

    /**
     * @see BrowsingMenuFieldConfig#addDoctypeFilterQuery()
     * @verifies not add doctype filter if field NE
     */
    @Test
    public void addDoctypeFilterQuery_shouldNotAddDoctypeFilterIfFieldNE() throws Exception {
        BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig("NE_FOO", null, null, false, false, false);
        Assertions.assertEquals(0, bmfc.getFilterQueries().size());
    }
}
