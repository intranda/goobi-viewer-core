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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.model.viewer.CollectionView.BrowseDataProvider;

public class CollectionViewTest extends AbstractDatabaseEnabledTest {

    List<String> collections;

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseEnabledTest.setUpClass();
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        collections = Arrays.asList(new String[] { "a", "a.b", "a.b.c", "a.b.d", "b", "b.a", "b.b", "c", "c.a", "c.b", "c.c", "c.c.a", "c.c.b", "c.d",
                "c.d.a", "c.d.b", "c.e", "d" });
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void test() throws IndexUnreachableException {
        CollectionView collection = new CollectionView(SolrConstants.DC, getTestProvider());
        collection.populateCollectionList();
        List<HierarchicalBrowseDcElement> topElements = new ArrayList<>(collection.getVisibleDcElements());
        System.out.println(topElements);
        assertTrue(topElements.size() == 4);
        assertTrue(topElements.get(0).getName() == "a");
        assertTrue(topElements.get(1).getName() == "c");
        assertTrue(topElements.get(2).getName() == "b");
        assertTrue(topElements.get(3).getName() == "d");

        collection.showAll();
        List<HierarchicalBrowseDcElement> allElements = collection.getVisibleDcElements();
        for (HierarchicalBrowseDcElement element : allElements) {
            System.out.println(element.getName());
        }
        assertTrue(allElements.size() == 18);
        assertTrue(allElements.get(3).getName() == "a.b.d");
        assertTrue(allElements.get(14).getName() == "b");
        assertTrue(allElements.get(5).getName() == "c.c");
        assertTrue(allElements.get(8).getName() == "c.a");
        assertTrue(allElements.get(11).getName() == "c.d.b");
        assertTrue(allElements.get(12).getName() == "c.d.a");
    }

    @Test
    public void testExpandCollection() throws IndexUnreachableException {
        CollectionView collection = new CollectionView(SolrConstants.DC, getTestProvider());
        collection.setBaseElementName("c.c");
        collection.populateCollectionList();
        List<HierarchicalBrowseDcElement> topElements = new ArrayList<>(collection.getVisibleDcElements());
        System.out.println(topElements);
    }

    /**
     * @return
     */
    private BrowseDataProvider getTestProvider() {
        return new BrowseDataProvider() {

            @Override
            public Map<String, Long> getData() {
                Map<String, Long> map = new HashMap<>();
                for (String string : collections) {
                    map.put(string, 1l);
                }
                return map;
            }
        };
    }

}
