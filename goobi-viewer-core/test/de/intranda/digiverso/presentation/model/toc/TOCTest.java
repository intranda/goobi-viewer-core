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
package de.intranda.digiverso.presentation.model.toc;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.model.toc.metadata.SimpleMetadataValue;

public class TOCTest {
    
    @Before
    public void setUp() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see TOC#getNumPages()
     * @verifies calculate number correctly
     */
    @Test
    public void getNumPages_shouldCalculateNumberCorrectly() throws Exception {
        TOC toc = new TOC();
        toc.setTotalTocSize(70);
        Assert.assertEquals(7, toc.getNumPages());
        toc.setTotalTocSize(77);
        Assert.assertEquals(8, toc.getNumPages());
    }

    /**
     * @see TOC#getLabel(String)
     * @verifies return correct label
     */
    @Test
    public void getLabel_shouldReturnCorrectLabel() throws Exception {
        TOC toc = new TOC();
        toc.setTocElementMap(new HashMap<>());
        toc.getTocElementMap().put(TOC.DEFAULT_GROUP, new ArrayList<>(3));
        toc.getTocElementMap().get(TOC.DEFAULT_GROUP).add(new TOCElement(new SimpleMetadataValue("one"), "0", null, "1", "LOG_0000", 0, "PPN_anchor", null, false, true, null,
                "periodical", null));
        toc.getTocElementMap().get(TOC.DEFAULT_GROUP).add(new TOCElement(new SimpleMetadataValue("two"), "1", null, "2", "LOG_0001", 1, "PPN_volume", null, false, true, null,
                "periodical_volume", null));
        toc.getTocElementMap().get(TOC.DEFAULT_GROUP).add(new TOCElement(new SimpleMetadataValue("three"), "1", null, "3", "LOG_0002", 2, "PPN_volume", null, false, true,
                null, "article", null));

        Assert.assertEquals("one", toc.getLabel("PPN_anchor"));
        Assert.assertEquals("two", toc.getLabel("PPN_volume"));
    }
}