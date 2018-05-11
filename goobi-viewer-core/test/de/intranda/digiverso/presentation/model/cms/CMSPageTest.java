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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.TestUtils;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.CmsBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem.CMSContentItemType;
import de.intranda.digiverso.presentation.servlets.rest.cms.CMSContentResource;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(BeanUtils.class)
public class CMSPageTest extends AbstractDatabaseEnabledTest{

    private static final String[] CLASSIFICATIONS = new String[]{"A", "B", "C", "D"};
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
//        ImageDeliveryBean idb = new ImageDeliveryBean();
//        PowerMockito.mockStatic(BeanUtils.class);
//        BDDMockito.given(BeanUtils.getImageDeliveryBean()).willReturn(idb);
//        BDDMockito.given(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).willReturn("http://localhost:8080/viewer");
        FacesContext facesContext = TestUtils.mockFacesContext();
        ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
        Mockito.when(servletContext.getRealPath("/")).thenReturn("WebContent");
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
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
//            expecedUrl = expecedUrl.replace("//", "/");
            Assert.assertEquals(expecedUrl, url);
        } catch (IllegalRequestException e) {
            fail("Item not found");
        }

    }
    
    @Test
    public void testCMSPage() throws DAOException, IOException, ServletException, URISyntaxException {
        //setup
        CMSPage page = new CMSPage();
        String templateId = "template";
        page.setTemplateId(templateId);
        
        CMSPageLanguageVersion german = new CMSPageLanguageVersion();
        german.setLanguage("de");
        page.addLanguageVersion(german);
        CMSPageLanguageVersion global = new CMSPageLanguageVersion();
        global.setLanguage("global");
        page.addLanguageVersion(global);
        
        Date created = new Date();
        created.setYear(created.getYear()-2);
        Date updated = new Date();
        page.setDateCreated(created);
        page.setDateUpdated(updated);
        
        page.setClassifications(new ArrayList<String>(Arrays.asList(CLASSIFICATIONS)));
        
        String altUrl = "test/page/";
        page.setPersistentUrl(altUrl);
        
        String textContent = "Text";
        String textId = "text";
        CMSContentItem text = new CMSContentItem(CMSContentItemType.TEXT);
        text.setItemId(textId);
        text.setHtmlFragment(textContent);
        page.addContentItem(text);
        
        String htmlContent = "<div>Content</div>";
        String htmlId = "html";
        CMSContentItem html = new CMSContentItem(CMSContentItemType.HTML);
        html.setItemId(htmlId);
        html.setHtmlFragment(htmlContent);
        page.addContentItem(html);
        
        CMSMediaItem media = new CMSMediaItem();
        String mediaFilename = "image 01.jpg";
        media.setFileName(mediaFilename);
        String imageId = "image";
        CMSContentItem image = new CMSContentItem(CMSContentItemType.MEDIA);
        image.setItemId(imageId);
        image.setMediaItem(media);
        page.addContentItem(image);
        
        String componentName = "sampleComponent";
        String componentId = "component";
        CMSContentItem component = new CMSContentItem(CMSContentItemType.COMPONENT);
        component.setItemId(componentId);
        component.setComponent(componentName);
        page.addContentItem(component);
        
        DataManager.getInstance().getDao().addCMSMediaItem(media);
        Long mediaId = media.getId();
        DataManager.getInstance().getDao().addCMSPage(page);
        Long pageId = page.getId();
        
        german.generateCompleteContentItemList();
        
        //tests
        Assert.assertEquals(created, page.getDateCreated());
        Assert.assertEquals(updated, page.getDateUpdated());
        Assert.assertEquals(Arrays.asList(CLASSIFICATIONS), page.getClassifications());
        Assert.assertEquals(altUrl, page.getRelativeUrlPath(true));
        Assert.assertEquals("cms/" + pageId + "/", page.getRelativeUrlPath(false));
        
        Assert.assertEquals(textContent, page.getContent(textId));
        
        String htmlUrl = page.getContent(htmlId);
        Path htmlUrlPath = Paths.get(new URI(htmlUrl).getPath());
        String htmlResponse = new CMSContentResource().getContentHtml(Long.parseLong(htmlUrlPath.subpath(3, 4).toString()), htmlUrlPath.subpath(4, 5).toString(), htmlUrlPath.subpath(5, 6).toString());
        Assert.assertEquals("<span>" + htmlContent + "</span>", htmlResponse);
       
        String contentServerUrl = DataManager.getInstance().getConfiguration().getIiifUrl();

        String filePath = media.getImageURI();
        filePath = BeanUtils.escapeCriticalUrlChracters(filePath, true);
       
        String imageUrl = contentServerUrl + "image/-/"+filePath+"/full/max/0/default.jpg";
        Assert.assertEquals(imageUrl, page.getContent(imageId));
        Assert.assertEquals(componentName, page.getContent(componentId));
        
    }

}
