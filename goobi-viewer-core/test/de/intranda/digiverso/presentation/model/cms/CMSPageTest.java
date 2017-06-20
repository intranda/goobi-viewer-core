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
package de.intranda.digiverso.presentation.model.cms;

import static org.junit.Assert.fail;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.intranda.digiverso.presentation.TestUtils;
import de.intranda.digiverso.presentation.managedbeans.CmsBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem.CMSContentItemType;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;

public class CMSPageTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        FacesContext facesContext = TestUtils.mockFacesContext();
        ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
        Mockito.when(servletContext.getRealPath("/")).thenReturn("WebContent");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetTileGridUrl() {

        String allowedTags = "a$b$cde";
        boolean preferImportant = true;
        int numTiles = 12;

        CMSPage page = new CMSPage();
        CMSPageLanguageVersion global = new CMSPageLanguageVersion();
        global.setLanguage("global");
        global.setOwnerPage(page);

        CMSPageLanguageVersion de = new CMSPageLanguageVersion();
        de.setLanguage("de");
        de.setOwnerPage(page);

        page.getLanguageVersions().add(global);
        page.getLanguageVersions().add(de);

        CMSContentItem gridItem = new CMSContentItem();
        gridItem.setAllowedTags(allowedTags);
        gridItem.setNumberOfImportantTiles(preferImportant ? numTiles : 0);
        gridItem.setNumberOfTiles(numTiles);
        gridItem.setId(1l);
        gridItem.setItemId("grid01");
        gridItem.setType(CMSContentItemType.TILEGRID);
        global.addContentItem(gridItem);

        try {
            String url = page.getTileGridUrl("grid01");
            String viewerUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
            String language = CmsBean.getCurrentLocale().getLanguage();
            String expecedUrl = viewerUrl + "/rest/tilegrid/" + language + "/" + numTiles + "/" + numTiles + "/" + allowedTags + "/";
            expecedUrl = expecedUrl.replace("//", "/");
            Assert.assertEquals(expecedUrl, url);
        } catch (IllegalRequestException e) {
            fail("Item not found");
        }

    }

}
