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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.IOException;
import java.util.List;

import javax.faces.context.FacesContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.TestUtils;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSPageTemplate;
import de.intranda.digiverso.presentation.model.cms.CMSStaticPage;
import de.intranda.digiverso.presentation.model.cms.CMSTemplateManager;

public class CmsBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        CMSTemplateManager.getInstance("WebContent/");
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
    public void testPage() {
        CMSPage page = new CMSPage();
        CmsBean bean = new CmsBean();
        bean.setSelectedPage(page);
        Assert.assertEquals(page, bean.getSelectedPage());
    }

    @Test
    public void testGetLuceneFields() {
        List<String> fields = CmsBean.getLuceneFields();
        for (String string : fields) {
            System.out.println(string);
        }
        Assert.assertTrue("Lucene field 'DC' is missing", fields.contains("DC"));
        Assert.assertTrue("Lucene field 'LABEL' is missing", fields.contains("LABEL"));
        Assert.assertTrue("Lucene field 'FILENAME' is missing", fields.contains("FILENAME"));
    }

    @Test
    public void testGetStaticPages() throws DAOException {
        CmsBean bean = new CmsBean();
        List<CMSStaticPage> staticPages = bean.getStaticPages();
        Assert.assertEquals(2, staticPages.size());
    }

    @Test
    public void testGetAvailableCmsPages() throws DAOException {
        CmsBean bean = new CmsBean();
        List<CMSPage> allPages = bean.getCreatedPages();
        List<CMSPage> availablePages = bean.getAvailableCmsPages(null);
        Assert.assertEquals(2, allPages.size() - availablePages.size());
    }

    @Test
    public void testSaveCMSPages() throws DAOException {
        Long pageId = 24l;
        CmsBean bean = new CmsBean();

        CMSPage page = new CMSPage();
        page.setId(pageId);
        List<CMSStaticPage> staticPages = bean.getStaticPages();
        CMSStaticPage staticPage = staticPages.get(0);

        //    	Assert.assertNull(staticPage.getCmsPage());
        staticPage.setCmsPage(page);
        bean.saveCMSPages();
        List<CMSPage> cmsPages = bean.loadCreatedPages();

        staticPages = bean.getStaticPages();
        staticPage = staticPages.get(0);
        Assert.assertEquals(pageId, staticPage.getCmsPage().getId());

        staticPage.setCmsPage(null);
        bean.saveCMSPages();
        cmsPages = bean.loadCreatedPages();

        staticPages = bean.getStaticPages();
        staticPage = staticPages.get(0);
        Assert.assertNull(staticPage.getCmsPage());
    }

    @Test
    public void testForwardToCMSPage() throws IOException, DAOException {

        //mock faces
        FacesContext facesContext = TestUtils.mockFacesContext();
        NavigationHelper navigationHelper = Mockito.mock(NavigationHelper.class);
        Mockito.when(navigationHelper.getCurrentPage()).thenReturn("index");
        facesContext.getExternalContext().getSessionMap().put("navigationHelper", navigationHelper);

        //mock bean
        CmsBean bean = Mockito.mock(CmsBean.class, Mockito.CALLS_REAL_METHODS);
        bean.setNavigationHelper(navigationHelper);
        Mockito.when(bean.getFacesContext()).thenReturn(facesContext);

        //mock pages
        CMSPageTemplate template = Mockito.mock(CMSPageTemplate.class, Mockito.CALLS_REAL_METHODS);
        template.setHtmlFileName("test.html");
        CMSPage cmsPage = Mockito.mock(CMSPage.class);
        Mockito.when(cmsPage.getTemplate()).thenReturn(template);
        Mockito.when(cmsPage.getId()).thenReturn(new Long(7));
        Mockito.when(cmsPage.isPublished()).thenReturn(true);
        cmsPage.setPublished(true);

        bean.getStaticPage("index").setCmsPage(cmsPage);
        bean.forwardToCMSPage();

        //check method call arguments
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(facesContext.getExternalContext()).dispatch(argument.capture());
        System.out.println("VALUE=" + argument.getValue());
        Assert.assertTrue(argument.getValue().endsWith("test.html"));

    }
}
