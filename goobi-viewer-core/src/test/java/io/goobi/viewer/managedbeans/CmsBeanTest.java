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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.search.HitType;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.search.SearchHitFactory;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrConstants;

class CmsBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final Logger logger = LogManager.getLogger(CmsBeanTest.class); //NOSONAR Sometimes used for debugging

    private CMSTemplateManager templateManager;
    private NavigationHelper navigationHelper;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        File webContent = new File("WebContent/").getAbsoluteFile();
        String webContentPath = webContent.toURI().toString();
        templateManager = new CMSTemplateManager(webContentPath, null);
        navigationHelper = new NavigationHelper();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @verifies page
     */
    @Test
    void getCurrentPage_shouldPage() {
        CMSPage page = new CMSPage();
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        bean.setCurrentPage(page);
        Assertions.assertEquals(page, bean.getCurrentPage());
    }

    /**
     * @verifies return true for given input
     * @see CmsBean#getLuceneFields()
     */
    @Test
    void getLuceneFields_shouldReturnTrueForGivenInput() {
        List<String> fields = new CmsBean().getLuceneFields();
        Assertions.assertTrue(fields.contains("DC"), "Lucene field 'DC' is missing");
        Assertions.assertTrue(fields.contains("LABEL"), "Lucene field 'LABEL' is missing");
        Assertions.assertTrue(fields.contains("FILENAME"), "Lucene field 'FILENAME' is missing");
    }

    /**
     * @verifies return non empty collection for given input
     * @see CmsBean#getStaticPages()
     */
    @Test
    void getStaticPages_shouldReturnNonEmptyCollectionForGivenInput() throws DAOException {
        CmsBean bean = new CmsBean();
        List<CMSStaticPage> staticPages = bean.getStaticPages();
        Assertions.assertFalse(staticPages.isEmpty());
    }

    /**
     * @verifies return 2 for given input
     */
    @Test
    void getAvailableCmsPages_shouldReturn2ForGivenInput() throws DAOException {
        CmsBean bean = new CmsBean();
        List<CMSPage> allPages = DataManager.getInstance().getDao().getAllCMSPages();
        List<CMSPage> availablePages = bean.getAvailableCmsPages(null);
        Assertions.assertEquals(2, allPages.size() - availablePages.size());
    }

    /**
     * @verifies save cms pages
     * @see CmsBean#CMSPage
     */
    @Test
    void CMSPage_shouldSaveCmsPages() throws DAOException {
        CmsBean bean = new CmsBean();

        CMSPage page = new CMSPage();
        assertTrue(DataManager.getInstance().getDao().addCMSPage(page));

        List<CMSStaticPage> staticPages = bean.getStaticPages();
        CMSStaticPage staticPage = staticPages.get(0);
        //    	Assertions.assertNull(staticPage.getCmsPage());
        staticPage.setCmsPage(page);
        bean.saveStaticPages();

        staticPages = bean.getStaticPages();
        staticPage = staticPages.get(0);
        Assertions.assertEquals(page.getId(), staticPage.getCmsPageOptional().map(p -> p.getId()).orElse(-1l));

        staticPage.setCmsPage(null);
        bean.saveStaticPages();

        staticPages = bean.getStaticPages();
        staticPage = staticPages.get(0);
        Assertions.assertNull(staticPage.getCmsPageOptional().orElse(null));
    }

    /**
     * @verifies return collection with 3 elements
     * @see CmsBean#getGroupedQueryResults(List<SearchHit>, String)
     */
    @Test
    void getGroupedQueryResults_shouldReturnCollectionWith3Elements() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        CmsBean bean = new CmsBean();

        String groupField = "GROUPING";
        String value1 = "VALUE1";
        String value2 = "VALUE2";
        String value3 = "VALUE3";

        List<SearchHit> results = new ArrayList<>();
        results.add(createSearchHit(groupField, value1, value2));
        results.add(createSearchHit(groupField, value1, value2));
        results.add(createSearchHit(groupField, value1, value3));

        List<Entry<String, List<SearchHit>>> hitMap = bean.getGroupedQueryResults(results, groupField);
        Assertions.assertEquals(3, hitMap.size());
        Assertions.assertEquals(3, hitMap.get(0).getValue().size());
        Assertions.assertEquals(2, hitMap.get(1).getValue().size());
        Assertions.assertEquals(1, hitMap.get(2).getValue().size());

        //Test for no valid grouping field
        hitMap = bean.getGroupedQueryResults(results, "bla");
        Assertions.assertEquals(1, hitMap.size());
        Assertions.assertEquals(3, hitMap.get(0).getValue().size());
    }

    /**
     *
     * @param field Metadata field name
     * @param values Metadata field values
     * @return A mock SearchHit
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    private static SearchHit createSearchHit(String field, String... values)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        SolrDocument doc = new SolrDocument();
        String iddoc = Long.toString(System.nanoTime());
        doc.addField(field, Arrays.asList(values));
        doc.addField(SolrConstants.IDDOC, iddoc);
        doc.addField(SolrConstants.PI_TOPSTRUCT, UUID.randomUUID());
        doc.addField("LABEL", doc.getFieldValue(SolrConstants.PI_TOPSTRUCT));
        SearchHit hit =
                new SearchHitFactory(null, null, null, 0, null, Locale.GERMAN).createSearchHit(doc, null, null, HitType.DOCSTRCT);
        hit.getBrowseElement().setLabelShort(new SimpleMetadataValue(iddoc));
        // logger.debug("labelShort: {}", hit.getBrowseElement().getLabelShort());
        hit.setSolrDoc(doc);
        return hit;
    }

    /**
     * @verifies return pages in specified order
     */
    @Test
    void createStaticPageList_shouldReturnPagesInSpecifiedOrder() throws Exception {
        List<PageType> pageTypes = PageType.getTypesHandledByCms(); // Order specified in the enum
        List<CMSStaticPage> pages = CmsBean.createStaticPageList();
        Assertions.assertEquals(pageTypes.size(), pages.size());
        for (int i = 0; i < pageTypes.size(); ++i) {
            Assertions.assertEquals(pageTypes.get(i).getName(), pages.get(i).getPageName());
        }
    }

    /**
     * @see CmsBean#getPossibleSortFields()
     * @verifies add relevance and random values at beginning
     */
    @Test
    void getPossibleSortFields_shouldAddRelevanceAndRandomValuesAtBeginning() throws Exception {
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        List<String> fields = bean.getPossibleSortFields();
        Assertions.assertTrue(fields.size() > 2);
        Assertions.assertEquals(SolrConstants.SORT_RELEVANCE, fields.get(0));
        Assertions.assertEquals(SolrConstants.SORT_RANDOM, fields.get(1));
    }

    /**
     * @see CmsBean#determineWorkDefaultView(String, String, boolean, boolean)
     * @verifies return object when work has images
     */
    @Test
    void determineWorkDefaultView_shouldReturnObjectWhenWorkHasImages() {
        String result = CmsBean.determineWorkDefaultView("monograph", "image/jpeg", false, true);
        Assertions.assertEquals("object", result);
    }

    /**
     * @see CmsBean#determineWorkDefaultView(String, String, boolean, boolean)
     * @verifies return metadata when work has no images
     */
    @Test
    void determineWorkDefaultView_shouldReturnMetadataWhenWorkHasNoImages() {
        String result = CmsBean.determineWorkDefaultView("monograph", "application/pdf", false, false);
        Assertions.assertEquals("metadata", result);
    }

    /**
     * @see CmsBean#determineWorkDefaultView(String, String, boolean, boolean)
     * @verifies return toc for anchor work
     */
    @Test
    void determineWorkDefaultView_shouldReturnTocForAnchorWork() {
        String result = CmsBean.determineWorkDefaultView("periodical", "image/jpeg", true, true);
        Assertions.assertEquals("toc", result);
    }

    /**
     * @verifies return current navigation view when no related work loaded
     */
    @Test
    void getRelatedWorkDefaultView_shouldReturnCurrentNavigationViewWhenNoRelatedWorkLoaded() throws Exception {
        NavigationHelper nav = new NavigationHelper();
        CmsBean bean = new CmsBean(templateManager, nav);
        CMSPage page = new CMSPage();
        bean.setCurrentPage(page);
        String result = bean.getRelatedWorkDefaultView();
        Assertions.assertEquals(nav.getCurrentView(), result);
    }

    /**
     * @see CmsBean#getEffectiveSidebarView()
     * @verifies return null when no current page
     */
    @Test
    void getEffectiveSidebarView_shouldReturnNullWhenNoCurrentPage() {
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        Assertions.assertNull(bean.getEffectiveSidebarView());
    }

    /**
     * @verifies return null when page has no related work
     */
    @Test
    void getEffectiveSidebarView_shouldReturnNullWhenPageHasNoRelatedWork() {
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        CMSPage page = new CMSPage();
        bean.setCurrentPage(page);
        Assertions.assertNull(bean.getEffectiveSidebarView());
    }

    /**
     * @verifies return false when current work pi is blank
     */
    @Test
    void isCmsWorkPageContext_shouldReturnFalseWhenCurrentWorkPiIsBlank() {
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        CMSPage page = new CMSPage();
        page.setRelatedPI("PI123");
        bean.setCurrentPage(page);
        Assertions.assertFalse(bean.isCmsWorkPageContext());
    }

    /**
     * @verifies return true when current work pi matches related PI
     */
    @Test
    void isCmsWorkPageContext_shouldReturnTrueWhenCurrentWorkPiMatchesRelatedPI() {
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        bean.setCurrentWorkPi("PI123");
        CMSPage page = new CMSPage();
        page.setRelatedPI("PI123");
        bean.setCurrentPage(page);
        Assertions.assertTrue(bean.isCmsWorkPageContext());
    }

    /**
     * @verifies return false when current work pi does not match related PI
     */
    @Test
    void isCmsWorkPageContext_shouldReturnFalseWhenCurrentWorkPiDoesNotMatchRelatedPI() {
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        bean.setCurrentWorkPi("PI123");
        CMSPage page = new CMSPage();
        bean.setCurrentPage(page);
        Assertions.assertFalse(bean.isCmsWorkPageContext());
    }

}
