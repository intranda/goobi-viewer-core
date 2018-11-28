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
package de.intranda.digiverso.presentation.servlets.rest.cms;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.CmsElementNotFoundException;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement;
import de.intranda.digiverso.presentation.model.cms.CMSTemplateManager;

public class CMSContentResourceTest extends AbstractDatabaseEnabledTest {

    CMSContentResource resource = new CMSContentResource();

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        File webContent = new File("WebContent/").getAbsoluteFile();
        Assert.assertTrue(webContent.isDirectory());
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

    /**
     * Test method for
     * {@link de.intranda.digiverso.presentation.servlets.rest.cms.CMSContentResource#getContentHtml(java.lang.Long, java.lang.String, java.lang.String)}.
     * 
     * @throws ServletException
     * @throws DAOException
     * @throws IOException
     */
    @Test
    public void testGetContentHtml() throws IOException, DAOException, ServletException {
        String output = resource.getContentHtml(1l, "de", "C1");
        String expectedOutput = "&lt;b&gt;Hello CMS&lt;/b&gt;";
        Assert.assertEquals(resource.wrap(expectedOutput), output);
    }

    /**
     * Test method for {@link de.intranda.digiverso.presentation.servlets.rest.cms.CMSContentResource#getSidebarElementHtml(java.lang.Long)}.
     * 
     * @throws ServletException
     * @throws DAOException
     * @throws IOException
     */
    @Test
    public void testGetSidebarElementHtml() throws IOException, DAOException, ServletException {
        String output = resource.getSidebarElementHtml(1l);
        String expectedOutput = "&lt;h1&gt;Hello Sidebar&lt;/h1&gt;";
        Assert.assertEquals(resource.wrap(expectedOutput), output);
    }

    /**
     * Test method for
     * {@link de.intranda.digiverso.presentation.servlets.rest.cms.CMSContentResource#getContentUrl(de.intranda.digiverso.presentation.model.cms.CMSContentItem)}.
     * 
     * @throws DAOException
     * @throws CmsElementNotFoundException 
     */
    @Test
    public void testGetContentUrl() throws DAOException, CmsElementNotFoundException {
        CMSPage page = DataManager.getInstance().getDao().getCMSPage(1l);
        CMSContentItem item = page.getContentItem("C1", "de");
        String url = CMSContentResource.getContentUrl(item);
        url = url.substring(0, url.indexOf("?"));

        Assert.assertEquals("/rest/cms/content/1/de/C1/", url);
    }

    /**
     * Test method for
     * {@link de.intranda.digiverso.presentation.servlets.rest.cms.CMSContentResource#getSidebarElementUrl(de.intranda.digiverso.presentation.model.cms.CMSSidebarElement)}.
     * 
     * @throws DAOException
     */
    @Test
    public void testGetSidebarElementUrl() throws DAOException {
        CMSSidebarElement element = DataManager.getInstance().getDao().getCMSSidebarElement(1);
        String url = CMSContentResource.getSidebarElementUrl(element);
        url = url.substring(0, url.indexOf("?"));
        Assert.assertEquals("/rest/cms/sidebar/1/", url);
    }

}
