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

import java.util.Locale;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.model.viewer.ViewManager;

public class ActiveDocumentBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(ActiveDocumentBeanTest.class);

    private NavigationHelper navigationHelper;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        //        ServletContext servletContext = Mockito.mock(ServletContext.class);
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);

        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        //        Mockito.when(externalContext.getContext()).thenReturn(servletContext);
        Mockito.when(facesContext.getViewRoot()).thenReturn(viewRoot);
        Mockito.when(viewRoot.getLocale()).thenReturn(Locale.GERMAN);

        this.navigationHelper = Mockito.mock(NavigationHelper.class);
        Mockito.when(navigationHelper.getCurrentPage()).thenReturn("viewImage_value");
        Mockito.when(navigationHelper.getCurrentView()).thenReturn("viewImage_value");
        Mockito.when(navigationHelper.getPreferredView()).thenReturn("viewImage_value");
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies create ViewManager correctly
     */
    @Test
    public void update_shouldCreateViewManagerCorrectly() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setPersistentIdentifier("PPN517154005");
        adb.setImageToShow(0); // TODO an updated index will always have a as the first image number
        adb.update();
        Assert.assertNotNull(adb.getViewManager());
        Assert.assertEquals("PPN517154005", adb.getPersistentIdentifier());
        Assert.assertEquals("PPN517154005", adb.getViewManager().getPi());
        Assert.assertEquals(1387459019047L, adb.getTopDocumentIddoc());
        Assert.assertEquals(1387459019047L, adb.getViewManager().getTopDocumentIddoc());
        Assert.assertEquals(1387459019067L, adb.getViewManager().getCurrentDocumentIddoc());
        Assert.assertNotNull(adb.getViewManager().getTopDocument());
        Assert.assertEquals(adb.getTopDocument(), adb.getViewManager().getTopDocument());
        Assert.assertNotNull(adb.getViewManager().getCurrentDocument());
        Assert.assertEquals(adb.getCurrentElement(), adb.getViewManager().getCurrentDocument());
        Assert.assertEquals("", adb.getViewManager().getLogId());
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies update ViewManager correctly if LOGID has changed
     */
    @Test
    public void update_shouldUpdateViewManagerCorrectlyIfLOGIDHasChanged() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setPersistentIdentifier("PPN517154005");
        adb.setImageToShow(0); // TODO an updated index will always have a as the first image number
        adb.update();
        Assert.assertEquals(1387459019067L, adb.getViewManager().getCurrentDocumentIddoc());
        ViewManager oldViewManager = adb.getViewManager();
        Assert.assertTrue(oldViewManager == adb.getViewManager());

        adb.setLogid("LOG_0005");
        adb.update();
        Assert.assertEquals("PPN517154005", adb.getViewManager().getPi());
        Assert.assertEquals(1387459019047L, adb.getViewManager().getTopDocumentIddoc());
        Assert.assertEquals(1387459019070L, adb.getViewManager().getCurrentDocumentIddoc());
        //        Assert.assertEquals("LOG_0005", adb.getViewManager().getLogId());
        Assert.assertFalse(oldViewManager == adb.getViewManager());
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies not override topDocumentIddoc if LOGID has changed
     */
    @Test
    public void update_shouldNotOverrideTopDocumentIddocIfLOGIDHasChanged() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setPersistentIdentifier("PPN517154005");
        adb.setImageToShow(1);
        adb.update();

        adb.setLogid("LOG_0005");
        adb.update();
        Assert.assertEquals(1387459019047L, adb.topDocumentIddoc);
    }

    /**
     * @see ActiveDocumentBean#setPersistentIdentifier(String)
     * @verifies determine currentElementIddoc correctly
     */
    @Test
    public void setPersistentIdentifier_shouldDetermineCurrentElementIddocCorrectly() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setPersistentIdentifier("PPN517154005");
        Assert.assertEquals(1387459019047L, adb.topDocumentIddoc);
    }

    /**
     * @see ActiveDocumentBean#getNextUrl(String,int)
     * @verifies increase image number by given step
     */
    @Test
    public void getNextUrl_shouldIncreaseImageNumberByGivenStep() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier("PPN517154005");
        adb.setImageToShow(10);
        adb.update();
        Assert.assertEquals("/viewImage_value/PPN517154005/13/", adb.getNextPageUrl(3));
    }

    /**
     * @see ActiveDocumentBean#getNextUrl(String,int)
     * @verifies go no higher than last page
     */
    @Test
    public void getNextUrl_shouldGoNoHigherThanLastPage() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier("PPN517154005");
        adb.setImageToShow(15);
        adb.update();
        Assert.assertEquals("/viewImage_value/PPN517154005/" + adb.getViewManager().getPageLoader().getLastPageOrder() + "/", adb.getNextPageUrl(3));
    }

    /**
     * @see ActiveDocumentBean#getPrevUrl(String,int)
     * @verifies decrease image number by given step
     */
    @Test
    public void getPrevUrl_shouldDecreaseImageNumberByGivenStep() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier("PPN517154005");
        adb.setImageToShow(10);
        adb.update();
        Assert.assertEquals("/viewImage_value/PPN517154005/7/", adb.getPreviousPageUrl(3));
    }

    /**
     * @see ActiveDocumentBean#getPrevUrl(String,int)
     * @verifies go no lower than first page order
     */
    @Test
    public void getPrevUrl_shouldGoNoLowerThanFirstPageOrder() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier("PPN517154005");
        adb.setImageToShow(2);
        adb.update();
        Assert.assertEquals("/viewImage_value/PPN517154005/" + adb.getViewManager().getPageLoader().getFirstPageOrder() + "/", adb.getPreviousPageUrl(
                4));
    }

    /**
     * @see ActiveDocumentBean#getPageUrl(String,int)
     * @verifies construct url correctly
     */
    @Test
    public void getUrl_shouldConstructUrlCorrectly() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier("PPN517154005");
        adb.setImageToShow(1);
        adb.update();
        Assert.assertEquals("/viewImage_value/PPN517154005/12/", adb.getPageUrl(12));
    }
}