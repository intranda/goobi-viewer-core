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
package io.goobi.viewer.model.viewer;

import java.util.Locale;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.solr.SolrConstants;

public class EventElementTest extends AbstractTest {

    /**
     * @see EventElement#EventElement(SolrDocument,Locale)
     * @verifies fill in missing dateStart from displayDate
     */
    @Test
    void EventElement_shouldFillInMissingDateStartFromDisplayDate() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTDATE, "2018-11-23");
        EventElement ee = new EventElement(doc, null, false);
        Assertions.assertNotNull(ee.getDateStart());
        Assertions.assertEquals("2018-11-23", DateTools.format(ee.getDateStart(), DateTools.formatterISO8601Date, false));
    }

    /**
     * @see EventElement#EventElement(SolrDocument,Locale)
     * @verifies fill in missing dateEnd from dateStart
     */
    @Test
    void EventElement_shouldFillInMissingDateEndFromDateStart() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTDATESTART, "2018-11-23");
        EventElement ee = new EventElement(doc, null, false);
        Assertions.assertNotNull(ee.getDateEnd());
        Assertions.assertEquals("2018-11-23", DateTools.format(ee.getDateEnd(), DateTools.formatterISO8601Date, false));
    }

    /**
     * @see EventElement#getLabel()
     * @verifies include type
     */
    @Test
    void getLabel_shouldIncludeType() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTTYPE, "Creation");
        EventElement ee = new EventElement(doc, null, false);
        Assertions.assertEquals("Creation", ee.getLabel());
    }

    /**
     * @see EventElement#getLabel()
     * @verifies not include date
     */
    @Test
    void getLabel_shouldNotIncludeDate() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTTYPE, "Creation");
        doc.setField(SolrConstants.EVENTDATESTART, "2021-09-17");
        EventElement ee = new EventElement(doc, null, false);
        Assertions.assertEquals("Creation", ee.getLabel());
    }

    /**
     * @see EventElement#EventElement(SolrDocument,Locale,boolean)
     * @verifies populate search hit metadata correctly
     */
    @Test
    void EventElement_shouldPopulateSearchHitMetadataCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTTYPE, "Creation");
        doc.setField(SolrConstants.EVENTDATESTART, "2021-09-17");
        EventElement ee = new EventElement(doc, null, true); // search mode
        Assertions.assertNotNull(ee.getSearchHitMetadata());
        Assertions.assertNull(ee.getMetadata());
        Assertions.assertNull(ee.getSidebarMetadata());
        // TODO test case with actual metadata values
    }

    /**
     * @see EventElement#EventElement(SolrDocument,Locale,boolean)
     * @verifies populate non search metadata correctly
     */
    @Test
    void EventElement_shouldPopulateNonSearchMetadataCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTTYPE, "Creation");
        doc.setField(SolrConstants.EVENTDATESTART, "2021-09-17");
        EventElement ee = new EventElement(doc, null, false); // non search mode
        Assertions.assertNull(ee.getSearchHitMetadata());
        Assertions.assertNotNull(ee.getMetadata());
        Assertions.assertNotNull(ee.getSidebarMetadata());
        // TODO test case with actual metadata values
    }
}
