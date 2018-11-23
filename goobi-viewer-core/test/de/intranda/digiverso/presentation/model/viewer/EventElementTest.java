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
package de.intranda.digiverso.presentation.model.viewer;

import java.util.Locale;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.SolrConstants;

public class EventElementTest {

    /**
     * @see EventElement#EventElement(SolrDocument,Locale)
     * @verifies fill in missing dateStart from displayDate
     */
    @Test
    public void EventElement_shouldFillInMissingDateStartFromDisplayDate() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTDATE, "2018-11-23");
        EventElement ee = new EventElement(doc, null);
        Assert.assertNotNull(ee.getDateStart());
        Assert.assertEquals("2018-11-23", DateTools.formatterISO8601Date.print(ee.getDateStart().getTime()));
    }

    /**
     * @see EventElement#EventElement(SolrDocument,Locale)
     * @verifies fill in missing dateEnd from dateStart
     */
    @Test
    public void EventElement_shouldFillInMissingDateEndFromDateStart() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTDATESTART, "2018-11-23");
        EventElement ee = new EventElement(doc, null);
        Assert.assertNotNull(ee.getDateEnd());
        Assert.assertEquals("2018-11-23", DateTools.formatterISO8601Date.print(ee.getDateEnd().getTime()));
    }
}