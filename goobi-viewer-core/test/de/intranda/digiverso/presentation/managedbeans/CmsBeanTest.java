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

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSStaticPage;
import de.intranda.digiverso.presentation.model.cms.CMSTemplateManager;

public class CmsBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final Logger logger = LoggerFactory.getLogger(CmsBeanTest.class);

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        File webContent = new File("WebContent/").getAbsoluteFile();
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
        String webContentPath = webContent.toURI().toString();
        //        if (webContentPath.startsWith("file:/")) {
        //            webContentPath = webContentPath.replace("file:/", "");
        //        }
        CMSTemplateManager.getInstance(webContentPath, null);
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
    public void testPage() throws DAOException {
        CMSPage page = new CMSPage();
        CmsBean bean = new CmsBean();
        bean.setSelectedPage(page);
        Assert.assertEquals(page, bean.getSelectedPage());
    }

    @Test
    public void testGetLuceneFields() {
        List<String> fields = CmsBean.getLuceneFields();
        Assert.assertTrue("Lucene field 'DC' is missing", fields.contains("DC"));
        Assert.assertTrue("Lucene field 'LABEL' is missing", fields.contains("LABEL"));
        Assert.assertTrue("Lucene field 'FILENAME' is missing", fields.contains("FILENAME"));
    }

    @Test
    public void testGetStaticPages() throws DAOException {
        CmsBean bean = new CmsBean();
        List<CMSStaticPage> staticPages = bean.getStaticPages();
        Assert.assertFalse(staticPages.isEmpty());
    }

    @Test
    public void testGetAvailableCmsPages() throws DAOException {
        CmsBean bean = new CmsBean();
        List<CMSPage> allPages = DataManager.getInstance().getDao().getAllCMSPages();
        List<CMSPage> availablePages = bean.getAvailableCmsPages(null);
        Assert.assertEquals(1, allPages.size() - availablePages.size());
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
        bean.saveStaticPages();

        staticPages = bean.getStaticPages();
        staticPage = staticPages.get(0);
        Assert.assertEquals(pageId, staticPage.getCmsPageOptional().map(p -> p.getId()).orElse(-1l));

        staticPage.setCmsPage(null);
        bean.saveStaticPages();

        staticPages = bean.getStaticPages();
        staticPage = staticPages.get(0);
        Assert.assertNull(staticPage.getCmsPageOptional().orElse(null));
    }

}
