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
package de.intranda.digiverso.presentation.model.cms.tilegrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.CMSCategory;
import de.intranda.digiverso.presentation.model.cms.tilegrid.ImageGalleryTile.Priority;

public class TileGridBuilderTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCountTags() {
        List<String> itemTags = Arrays.asList(new String[] { "tag1", "tag2", "tag3", "other"});
        List<CMSCategory> categories = itemTags.stream().map(tag -> getCategory(tag)).collect(Collectors.toList());
        List<String> selectionTags = Arrays.asList(new String[] { "tag2", "tag3", "news" });
        List<CMSCategory> selectionCategories = selectionTags.stream().map(tag -> getCategory(tag)).collect(Collectors.toList());
        CMSMediaItem item = new CMSMediaItem();
        item.setCategories(categories);
        Assert.assertEquals(2, TileGridBuilder.countTags(item, selectionCategories.stream().map(c -> c.getName()).collect(Collectors.toList())));

    }

	/**
	 * Just create a temporary category to avoid having to set up the DAO
	 * @param tag
	 * @return
	 * @throws DAOException
	 */
	private CMSCategory getCategory(String tag) {
		CMSCategory cat = new CMSCategory(tag);
		return cat;
	}

    @Test
    public void testBuild() {
        List<ImageGalleryTile> items = new ArrayList<>();
        CMSMediaItem item1 = new CMSMediaItem();
        item1.setId(1l);
        item1.addCategory(getCategory("tag1"));
        item1.setPriority(Priority.IMPORTANT);
        item1.setFileName("file1");
        items.add(item1);
        CMSMediaItem item2 = new CMSMediaItem();
        item2.setId(2l);
        item2.addCategory(getCategory("tag2"));
        item2.setPriority(Priority.IMPORTANT);
        item2.setFileName("file2");
        items.add(item2);
        CMSMediaItem item3 = new CMSMediaItem();
        item3.setId(3l);
        item3.addCategory(getCategory("tag1"));
        item3.setPriority(Priority.DEFAULT);
        item3.setFileName("file3");
        items.add(item3);
        CMSMediaItem item4 = new CMSMediaItem();
        item4.setId(4l);
        item4.addCategory(getCategory("tag1"));
        item4.setPriority(Priority.DEFAULT);
        item4.setFileName("file4");
        items.add(item4);

        for (int i = 0; i < 20; i++) {
            TileGrid grid = new TileGridBuilder(null).size(2).reserveForHighPriority(1).tags("tag1").build(items);
            Assert.assertEquals(2, grid.getItems().size());
            Assert.assertTrue("Grid does not contain " + item1, contains(grid.getItems(), item1));
            Assert.assertTrue("Grid does not contain item 3 or 4: " + grid, contains(grid.getItems(), item3) || contains(grid.getItems(), item4));
        }
    }

    /**
     * @param items
     * @param mediaItem
     * @return
     */
    private static boolean contains(List<Tile> items, CMSMediaItem mediaItem) {
        for (Tile tile : items) {
            if (tile.getName().equals(mediaItem.getIconURI(0,0).toString())) {
                return true;
            }
        }
        return false;
    }

}
