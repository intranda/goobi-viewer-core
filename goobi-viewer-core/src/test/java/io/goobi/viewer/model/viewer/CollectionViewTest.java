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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CollectionViewBean;
import io.goobi.viewer.model.cms.collections.CMSCollection;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.CollectionView.BrowseDataProvider;
import io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement;
import io.goobi.viewer.solr.SolrConstants;

class CollectionViewTest extends AbstractDatabaseAndSolrEnabledTest {

    List<String> collections;

    /**
     * <p>setUpClass.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /** {@inheritDoc} */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        collections = Arrays.asList(new String[] { "a", "a.b", "a.b.c", "a.b.d", "b", "b.a", "b.b", "c", "c.a", "c.b", "c.c", "c.c.a", "c.c.b", "c.d",
                "c.d.a", "c.d.b", "c.e", "d" });
    }

    /** {@inheritDoc} */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void test() throws IndexUnreachableException, IllegalRequestException {
        CollectionView collection = new CollectionView(SolrConstants.DC, getTestProvider());
        collection.populateCollectionList();
        List<HierarchicalBrowseDcElement> topElements = new ArrayList<>(collection.getVisibleDcElements());
        assertEquals(4, topElements.size());
        assertEquals("a", topElements.get(0).getName());
        assertEquals("c", topElements.get(1).getName());
        assertEquals("b", topElements.get(2).getName());
        assertEquals("d", topElements.get(3).getName());

        collection.showAll();
        List<HierarchicalBrowseDcElement> allElements = collection.getVisibleDcElements();
        assertEquals(18, allElements.size());
        assertEquals("a.b.d", allElements.get(3).getName());
        assertEquals("b", allElements.get(14).getName());
        assertEquals("c.c", allElements.get(5).getName());
        assertEquals("c.a", allElements.get(8).getName());
        assertEquals("c.d.b", allElements.get(11).getName());
        assertEquals("c.d.a", allElements.get(12).getName());
    }

    //    @Test
    //    void testExpandCollection() throws IndexUnreachableException, IllegalRequestException {
    //        CollectionView collection = new CollectionView(SolrConstants.DC, getTestProvider());
    //        collection.setBaseElementName("c.c");
    //        collection.populateCollectionList();
    //        List<HierarchicalBrowseDcElement> topElements = new ArrayList<>(collection.getVisibleDcElements());
    //    }

    /**
     * @return
     */
    private BrowseDataProvider getTestProvider() {
        return new BrowseDataProvider() {

            @Override
            public Map<String, CollectionResult> getData() {
                Map<String, CollectionResult> map = new HashMap<>();
                for (String string : collections) {
                    map.put(string, new CollectionResult(string, 1l));
                }
                return map;
            }
        };
    }

    /**
     * @see CollectionView#getCollectionUrl(HierarchicalBrowseDcElement,String,String)
     * @verifies return identifier resolver url if single record and pi known
     */
    @Test
    void getCollectionUrl_shouldReturnIdentifierResolverUrlIfSingleRecordAndPiKnown() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("collections.redirectToWork", true);
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isAllowRedirectCollectionToWork());

        CollectionView col = new CollectionView("foo", getTestProvider());
        HierarchicalBrowseDcElement element =
                new HierarchicalBrowseDcElement("bar", 1, "foo", null, col.getSplittingChar(), col.getDisplayNumberOfVolumesLevel());
        element.setSingleRecordUrl("/object/PI123/1/LOG_0001/");
        Assertions.assertEquals("/object/PI123/1/LOG_0001/", col.getCollectionUrl(element));
    }

    /**
     * @see CollectionView#getCollectionUrl(HierarchicalBrowseDcElement,String,String)
     * @verifies escape critical url chars in collection name
     */
    @Test
    void getCollectionUrl_shouldEscapeCriticalUrlCharsInCollectionName() throws Exception {
        CollectionView col = new CollectionView("foo", getTestProvider());
        HierarchicalBrowseDcElement element =
                new HierarchicalBrowseDcElement("foo/bar", 2, SolrConstants.DC, null, col.getSplittingChar(), col.getDisplayNumberOfVolumesLevel());
        Assertions.assertEquals("/search/-/-/1/-/foo%3AfooU002Fbar/", col.getCollectionUrl(element));
    }

    @Test
    void loadCMSCollection_addCMSCollectionInfo() throws PresentationException, IndexUnreachableException, IllegalRequestException, DAOException {
        CMSPage page = new CMSPage();
        page.setId(1l);
        PersistentCMSComponent component = new PersistentCMSComponent();
        component.setOwningPage(page);
        CMSCollectionContent contentItem = new CMSCollectionContent();
        contentItem.setSolrField("DC");
        contentItem.setOwningComponent(component);

        CollectionViewBean collectionViewBean = new CollectionViewBean();
        CollectionView collection = collectionViewBean.getCollection(contentItem, 0, false, false, false);

        HierarchicalBrowseDcElement element =
                collection.getVisibleDcElements().stream().filter(ele -> ele.getName().equals("dcimage")).findAny().orElse(null);
        assertNotNull(element);
        assertNotNull(element.getInfo());
        assertEquals(CMSCollection.class, element.getInfo().getClass());
        CMSMediaItem mediaItem = DataManager.getInstance().getDao().getCMSMediaItem(1l);
        assertEquals(mediaItem.getIconURI(), element.getInfo().getIconURI());
    }

    /**
     * @see Configuration#getCollectionDefaultSortField(String)
     * @verifies give priority to exact matches
     */
    @Test
    void getCollectionDefaultSortField_shouldGivePriorityToExactMatches() throws Exception {
        Map<String, String> sortFields = DataManager.getInstance().getConfiguration().getCollectionDefaultSortFields(SolrConstants.DC);
        Assertions.assertEquals("SORT_TITLE", CollectionView.getCollectionDefaultSortField("collection1", sortFields));
    }

    /**
     * @see Configuration#getCollectionDefaultSortField(String)
     * @verifies return correct field for collection
     */
    @Test
    void getCollectionDefaultSortField_shouldReturnCorrectFieldForCollection() throws Exception {
        Map<String, String> sortFields = DataManager.getInstance().getConfiguration().getCollectionDefaultSortFields(SolrConstants.DC);
        Assertions.assertEquals("SORT_CREATOR", CollectionView.getCollectionDefaultSortField("collection1.sub1", sortFields));
    }

    /**
     * @see Configuration#getCollectionDefaultSortField(String)
     * @verifies return hyphen if collection not found
     */
    @Test
    void getCollectionDefaultSortField_shouldReturnHyphenIfCollectionNotFound() throws Exception {
        Map<String, String> sortFields = DataManager.getInstance().getConfiguration().getCollectionDefaultSortFields(SolrConstants.DC);
        Assertions.assertEquals("-", CollectionView.getCollectionDefaultSortField("nonexistingcollection", sortFields));
    }

}
