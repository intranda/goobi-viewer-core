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
package io.goobi.viewer.api.rest.v1.cms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.api.rest.model.MediaItem;
import io.goobi.viewer.api.rest.v1.cms.CMSMediaResource.MediaList;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSMediaItemMetadata;

/**
 * @author florian
 *
 */
public class CMSMediaResourceTest extends AbstractDatabaseEnabledTest {

    List<CMSMediaItem> items = new ArrayList<CMSMediaItem>();
    CMSMediaResource resource = new CMSMediaResource();
    CMSCategory category = new CMSCategory("unitTest");
    int numItems = 10;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        DataManager.getInstance().getDao().addCategory(category);

        for (long i = 1; i <= numItems; i++) {
            CMSMediaItem item = new CMSMediaItem();
            item.setFileName(i + ".jpg");
            CMSMediaItemMetadata md = new CMSMediaItemMetadata();
            md.setLanguage("en");
            md.setName(Long.toString(i));
            item.addMetadata(md);
            item.addCategory(category);
            items.add(item);
        }
        items.get(numItems / 2 - 1).setImportant(true);
        items.get(numItems - 1).setImportant(true);
        items.forEach(item -> {
            try {
                DataManager.getInstance().getDao().addCMSMediaItem(item);
            } catch (DAOException e) {
                fail(e.toString());
            }
        });
        //        
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        items.forEach(item -> {
            try {
                DataManager.getInstance().getDao().deleteCMSMediaItem(item);
            } catch (DAOException e) {
                fail(e.toString());
            }
        });
        DataManager.getInstance().getDao().deleteCategory(category);
        super.tearDown();
    }

    @Test
    public void testGetAllItems() throws DAOException {
        MediaList list = resource.getAllMedia(null, null, null, false);
        assertTrue("Expect to get more than " + numItems + " items, but only got " + list.getMediaItems().size(),
                numItems < list.getMediaItems().size());

    }

    @Test
    public void testGetItemsByCategory() throws DAOException {
        MediaList list = resource.getAllMedia("unitTest", null, null, false);
        assertEquals(numItems, list.getMediaItems().size());
        assertEquals("1", getLabel(list, 0));
        assertEquals("2", getLabel(list, 1));
        assertEquals("3", getLabel(list, 2));
    }

    @Test
    public void testGetItemsRandom() throws DAOException {
        MediaList list = resource.getAllMedia("unitTest", null, null, true);
        assertEquals(numItems, list.getMediaItems().size());
        assertFalse("order not random",
                "1".equals(getLabel(list, 0))
                        && "2".equals(getLabel(list, 1))
                        && "3".equals(getLabel(list, 2)));
    }


    @Test
    public void testGetItemsLimitCount() throws DAOException {
        MediaList list = resource.getAllMedia("unitTest", 5, null, false);
        assertEquals(5, list.getMediaItems().size());
    }
    
    @Test
    public void testGetPriorityItems() throws DAOException {
        MediaList list = resource.getAllMedia("unitTest", 2, 2, false);
        String index1 = "" + numItems / 2;
        String index2 = "" + numItems;
        assertTrue(list.getMediaItems().stream().anyMatch(item -> getLabel(item).equals(index1)));
        assertTrue(list.getMediaItems().stream().anyMatch(item -> getLabel(item).equals(index2)));

    }
    
    /**
     * @param list
     * @param index 
     * @return
     */
    private String getLabel(MediaList list, int index) {
        return list.getMediaItems().get(index).getLabel().getValue("en").orElse("");
    }
    
    private String getLabel(MediaItem item) {
        return item.getLabel().getValue("en").orElse("");
    }
    

}
