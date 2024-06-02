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
package io.goobi.viewer.api.rest.v1.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.Equator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.media.CMSMediaItemMetadata;
import io.goobi.viewer.model.cms.media.MediaItem;
import io.goobi.viewer.model.cms.media.MediaList;

/**
 * @author florian
 *
 */
class CMSMediaResourceTest extends AbstractDatabaseEnabledTest {

    List<CMSMediaItem> items = new ArrayList<CMSMediaItem>();
    CMSMediaResource resource;
    CMSCategory category = new CMSCategory("unitTest");
    int numItems = 10;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        DataManager.getInstance().getDao().addCategory(category);
        resource = new CMSMediaResource(DataManager.getInstance().getDao());

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
    @AfterEach
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
    void testGetAllItems() throws DAOException {
        MediaList list = resource.getAllMedia(null, null, null, false);
        assertTrue(numItems < list.getMediaItems().size(),
                "Expect to get more than " + numItems + " items, but only got " + list.getMediaItems().size());

    }

    @Test
    void testGetItemsByCategory() throws DAOException {
        MediaList list = resource.getAllMedia("unitTest", null, null, false);
        assertEquals(numItems, list.getMediaItems().size());
        assertEquals("1", getLabel(list, 0));
        assertEquals("2", getLabel(list, 1));
        assertEquals("3", getLabel(list, 2));
    }

    @Test
    void testGetItemsRandom() throws DAOException {

        List<MediaList> resultsOrdered = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            MediaList list = resource.getAllMedia("unitTest", null, null, false);
            if (resultsOrdered.isEmpty() || isEquals(list, resultsOrdered.get(0), new ListEquator())) {
                resultsOrdered.add(list);
            }
        }
        assertEquals(20, resultsOrdered.size());

        List<MediaList> resultsRandom = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            MediaList list = resource.getAllMedia("unitTest", null, null, true);
            if (resultsRandom.isEmpty() || isEquals(list, resultsRandom.get(0), new ListEquator())) {
                resultsRandom.add(list);
            }
        }
        assertEquals(1, resultsRandom.size());
    }

    /**
     * 
     * @param list1
     * @param list2
     * @param equator
     * @return boolean
     */
    private static boolean isEquals(MediaList list1, MediaList list2, ListEquator equator) {
        if (list1.getMediaItems().size() == list2.getMediaItems().size()) {
            for (int i = 0; i < list1.getMediaItems().size(); i++) {
                boolean same = getLabel(list1, i).equals(getLabel(list2, i));
                if (!same) {
                    return false;
                }
            }
        }
        return true;
    }

    private class ListEquator implements Equator<MediaItem> {

        @Override
        public boolean equate(MediaItem o1, MediaItem o2) {
            return o1.getLabel().getValue("en").equals(o2.getLabel().getValue("en"));
        }

        @Override
        public int hash(MediaItem o) {
            return o.getId().hashCode();
        }

    }

    @Test
    void testGetItemsLimitCount() throws DAOException {
        MediaList list = resource.getAllMedia("unitTest", 5, null, false);
        assertEquals(5, list.getMediaItems().size());
    }

    @Test
    void testGet2PriorityItems() throws DAOException {
        MediaList list = resource.getAllMedia("unitTest", 2, 2, false);
        assertEquals(2, list.getMediaItems().stream().filter(MediaItem::isImportant).count());

    }

    @Test
    void testGet1PriorityItem() throws DAOException {
        MediaList list = resource.getAllMedia("unitTest", 2, 1, false);
        assertEquals(1, list.getMediaItems().stream().filter(MediaItem::isImportant).count());
    }

    @Test
    void testDisplayOrder() throws DAOException {
        CMSMediaItem item1 = DataManager.getInstance().getDao().getCMSMediaItemByFilename("3.jpg");
        CMSMediaItem item2 = DataManager.getInstance().getDao().getCMSMediaItemByFilename("7.jpg");
        item1.setDisplayOrder(2);
        item2.setDisplayOrder(1);
        DataManager.getInstance().getDao().updateCMSMediaItem(item1);
        DataManager.getInstance().getDao().updateCMSMediaItem(item2);

        MediaList list = resource.getAllMedia("unitTest", 4, 0, true);

        assertEquals(item1.getId(), list.getMediaItems().get(1).getId());
        assertEquals(item2.getId(), list.getMediaItems().get(0).getId());

    }

    /**
     * @param list
     * @param index
     * @return
     */
    private static String getLabel(MediaList list, int index) {
        return list.getMediaItems().get(index).getLabel().getValue("en").orElse("");
    }
}
