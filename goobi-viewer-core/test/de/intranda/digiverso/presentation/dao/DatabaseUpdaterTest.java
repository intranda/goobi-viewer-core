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
package de.intranda.digiverso.presentation.dao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.model.cms.CMSCategory;

/**
 * @author florian
 *
 */
public class DatabaseUpdaterTest {

	/**
	 * Test method for {@link de.intranda.digiverso.presentation.dao.DatabaseUpdater#createCategories(java.util.Map)}.
	 */
	@Test
	public void testCreateCategories() {
		Map<String, Map<String, List<Long>>> map = new HashMap<>();
		
		Map<String, List<Long>> pageMap = new HashMap<>();
		pageMap.put("categoryA", Arrays.asList(new Long[] {1l,2l,3l}));
		pageMap.put("categoryB", Arrays.asList(new Long[] {2l,3l, 4l}));
		pageMap.put("categoryC", Arrays.asList(new Long[] {5l,6l}));
		map.put("page", pageMap );
		
		Map<String, List<Long>> contentMap = new HashMap<>();
		contentMap.put("categoryA::categoryB", Arrays.asList(new Long[] {1l,2l,3l}));
		contentMap.put("categoryC::categoryD", Arrays.asList(new Long[] {5l,6l}));
		contentMap.put("categoryE", Arrays.asList(new Long[] {2l,3l, 4l}));
		map.put("content", contentMap );
		
		Map<String, List<Long>> mediaMap = new HashMap<>();
		mediaMap.put("categoryE", Arrays.asList(new Long[] {1l,2l,3l}));
		mediaMap.put("categoryF", Arrays.asList(new Long[] {5l,6l}));
		map.put("page", mediaMap );
		
		List<CMSCategory> categories = new DatabaseUpdater(null).createCategories(map);
		Assert.assertEquals(6, categories.size());
		categories.forEach(cat -> {
			System.out.println(cat.getName());
		});
		Assert.assertTrue(categories.stream().anyMatch(cat -> cat.getName().equals("categoryC")));
	}

}
