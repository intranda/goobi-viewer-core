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
package io.goobi.viewer.dao.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.dao.update.CMSCategoryUpdate;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSPage;

/**
 * @author florian
 *
 */
public class CMSCategoryUpdateTest {

	CMSCategoryUpdate update = new CMSCategoryUpdate();
	
	@Before
	public void setup() {

		update.entityMap = new HashMap<>();
		update.categories = new ArrayList<>();
		update.content = new ArrayList<>();
		update.media = new ArrayList<>();
		update.pages = new ArrayList<>();
		
		Map<String, List<Long>> pageMap = new HashMap<>();
		pageMap.put("categoryA", Arrays.asList(new Long[] {1l}));
		pageMap.put("categoryB", Arrays.asList(new Long[] {2l}));
		pageMap.put("categoryC", Arrays.asList(new Long[] {2l,3l}));
		update.entityMap.put("page", pageMap );
		
		Map<String, List<Long>> contentMap = new HashMap<>();
		contentMap.put("categoryA", Arrays.asList(new Long[] {1l,2l}));
		contentMap.put("categoryD", Arrays.asList(new Long[] {3l}));
		update.entityMap.put("content", contentMap );
		
		Map<String, List<Long>> mediaMap = new HashMap<>();
		mediaMap.put("categoryD", Arrays.asList(new Long[] {1l,2l}));
		mediaMap.put("categoryE", Arrays.asList(new Long[] {3l,4l}));
		update.entityMap.put("media", mediaMap );
		
		CMSPage p1 = new CMSPage();
		p1.setId(1l);
		update.pages.add(p1);
		CMSPage p2 = new CMSPage();
		p2.setId(2l);
		update.pages.add(p2);
		CMSPage p3 = new CMSPage();
		p3.setId(1l);
		update.pages.add(p3);

		CMSContentItem item1 = new CMSContentItem();
		item1.setId(1l);
		update.content.add(item1);
		CMSContentItem item2 = new CMSContentItem();
		item2.setId(2l);
		update.content.add(item2);
		CMSContentItem item3 = new CMSContentItem();
		item3.setId(3l);
		update.content.add(item3);
		
		CMSMediaItem media1 = new CMSMediaItem();
		media1.setId(1l);
		update.media.add(media1);
		CMSMediaItem media2 = new CMSMediaItem();
		media2.setId(2l);
		update.media.add(media2);
		CMSMediaItem media3 = new CMSMediaItem();
		media3.setId(3l);
		update.media.add(media3);
		CMSMediaItem media4 = new CMSMediaItem();
		media4.setId(4l);
		update.media.add(media4);
	}
	
	@Test
	public void testAddCategoriesToPages() throws DAOException {
		update.convertData();
		CMSPage page = update.pages.stream().filter(p -> p.getId().equals(2l)).findFirst().get();
		Assert.assertEquals(2, page.getCategories().size());
		Assert.assertEquals("categoryb", page.getCategories().get(0).getName());
		Assert.assertEquals("categoryc", page.getCategories().get(1).getName());
	}
	
	@Test
	public void testAddCategoriesToMedia() throws DAOException {
		update.convertData();
		CMSMediaItem media = update.media.stream().filter(p -> p.getId().equals(1l)).findFirst().get();
		Assert.assertEquals(1, media.getCategories().size());
		Assert.assertEquals("categoryd", media.getCategories().get(0).getName());
	}
	
	@Test
	public void testAddCategoriesToContent() throws DAOException {
		update.convertData();
		CMSContentItem content = update.content.stream().filter(p -> p.getId().equals(3l)).findFirst().get();
		Assert.assertEquals(1, content.getCategories().size());
		Assert.assertEquals("categoryd", content.getCategories().get(0).getName());
	}
	
	/**
	 * Test method for {@link io.goobi.viewer.dao.update.DatabaseUpdater#createCategories(java.util.Map)}.
	 * @throws DAOException 
	 */
	@Test
	public void testCreateCategories() throws DAOException {

		update.convertData();
		Assert.assertEquals(5, update.categories.size());
		Assert.assertTrue(update.categories.stream().anyMatch(cat -> cat.getName().equals("categoryc")));
	}

}
