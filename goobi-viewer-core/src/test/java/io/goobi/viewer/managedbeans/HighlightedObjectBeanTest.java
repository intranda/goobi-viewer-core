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
package io.goobi.viewer.managedbeans;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;

public class HighlightedObjectBeanTest extends AbstractDatabaseEnabledTest {

    HighlightedObjectBean bean;
    
    @Before
    public void setup() throws Exception {
        super.setUp();
        bean = new HighlightedObjectBean();
        bean.init();
    }
    
    @Test
    public void test_listObjecs() {
        
        assertEquals(3, bean.getDataProvider().getPaginatorList().size());
        assertEquals(3, bean.getDataProvider().getSizeOfDataList());
        assertEquals(1l, bean.getDataProvider().getPageNumberCurrent().longValue());
        assertEquals(0, bean.getDataProvider().getLastPageNumber());
        bean.getDataProvider().cmdMoveNext();
        assertEquals(1l, bean.getDataProvider().getPageNumberCurrent().longValue());
    }
    
    @Test
    public void test_filterList() {
        
        bean.getDataProvider().getFilter("name").setValue("Monat");
        assertEquals(2, bean.getDataProvider().getSizeOfDataList());
        
        bean.getDataProvider().getFilter("name").setValue("Januar");
        assertEquals(1, bean.getDataProvider().getSizeOfDataList());

        bean.getDataProvider().getFilter("name").setValue("");
        assertEquals(3, bean.getDataProvider().getSizeOfDataList());
    }

}
