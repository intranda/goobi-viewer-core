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
package io.goobi.viewer.managedbeans;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.ViewManager;

public class ActiveDocumentBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(ActiveDocumentBeanTest.class);

    private NavigationHelper navigationHelper;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        //        ServletContext servletContext = Mockito.mock(ServletContext.class);
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);

        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        //        Mockito.when(externalContext.getContext()).thenReturn(servletContext);
//        Mockito.when(facesContext.getViewRoot()).thenReturn(viewRoot);
//        Mockito.when(viewRoot.getLocale()).thenReturn(Locale.GERMAN);

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
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();
        Assert.assertNotNull(adb.getViewManager());
        Assert.assertEquals(PI_KLEIUNIV, adb.getPersistentIdentifier());
        Assert.assertEquals(PI_KLEIUNIV, adb.getViewManager().getPi());
        Assert.assertEquals(iddocKleiuniv, adb.getTopDocumentIddoc());
        Assert.assertEquals(iddocKleiuniv, adb.getViewManager().getTopDocumentIddoc());
        Assert.assertNotEquals(iddocKleiuniv, adb.getViewManager().getCurrentDocumentIddoc());
        Assert.assertNotNull(adb.getViewManager().getTopDocument());
        Assert.assertEquals(adb.getTopDocument(), adb.getViewManager().getTopDocument());
        Assert.assertNotNull(adb.getViewManager().getCurrentStructElement());
        Assert.assertEquals(adb.getCurrentElement(), adb.getViewManager().getCurrentStructElement());
        Assert.assertEquals("", adb.getViewManager().getLogId());
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies update ViewManager correctly if LOGID has changed
     */
    @Test
    public void update_shouldUpdateViewManagerCorrectlyIfLOGIDHasChanged() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();
        Assert.assertNotEquals(iddocKleiuniv, adb.getViewManager().getCurrentDocumentIddoc());
        ViewManager oldViewManager = adb.getViewManager();
        Assert.assertTrue(oldViewManager == adb.getViewManager());

        adb.setLogid("LOG_0003");
        adb.update();
        Assert.assertEquals(PI_KLEIUNIV, adb.getViewManager().getPi());
        Assert.assertEquals(iddocKleiuniv, adb.getViewManager().getTopDocumentIddoc());
        Assert.assertNotEquals(iddocKleiuniv, adb.getViewManager().getCurrentDocumentIddoc());
        // Assert.assertEquals("LOG_0003", adb.getViewManager().getLogId());
        Assert.assertFalse(oldViewManager == adb.getViewManager());
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies not override topDocumentIddoc if LOGID has changed
     */
    @Test
    public void update_shouldNotOverrideTopDocumentIddocIfLOGIDHasChanged() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();

        adb.setLogid("LOG_0005");
        try {
            adb.update();
        } catch (IDDOCNotFoundException e) {
        }
        Assert.assertEquals(iddocKleiuniv, adb.topDocumentIddoc);
    }

    /**
     * @see ActiveDocumentBean#setPersistentIdentifier(String)
     * @verifies determine currentElementIddoc correctly
     */
    @Test
    public void setPersistentIdentifier_shouldDetermineCurrentElementIddocCorrectly() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        Assert.assertEquals(iddocKleiuniv, adb.topDocumentIddoc);
    }

    /**
     * @see ActiveDocumentBean#getNextUrl(String,int)
     * @verifies increase image number by given step
     */
    @Test
    public void getNextUrl_shouldIncreaseImageNumberByGivenStep() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("10");
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
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("15");
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
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("15");
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
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("15");
        try {
            adb.update();
        } catch (IDDOCNotFoundException e) {
        }
        Assert.assertEquals("/viewImage_value/PPN517154005/" + adb.getViewManager().getPageLoader().getFirstPageOrder() + "/",
                adb.getPreviousPageUrl(4));
    }

    /**
     * @see ActiveDocumentBean#getPageUrl(String,int)
     * @verifies construct url correctly
     */
    @Test
    public void getUrl_shouldConstructUrlCorrectly() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("15");
        adb.update();
        Assert.assertEquals("/viewImage_value/PPN517154005/12/", adb.getPageUrl("12"));
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies load records that have been released via moving wall
     */
    @Test
    public void update_shouldLoadRecordsThatHaveBeenReleasedViaMovingWall() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();

        // Record has been unlocked via a moving wall functionality and should load withour throwing a RecordNotFoundException.
        // The public release year metadata is only available in docstruct and page Solr docs. Here, it is important that none of the
        // record's docs match the conditional query of the license type anyway.
        adb.setPersistentIdentifier("1045513032");
        adb.setImageToShow("1");

        // Override config setting so that localhost doesn't get full access
        DataManager.getInstance().getConfiguration().overrideValue("accessConditions.fullAccessForLocalhost", false);

        adb.update();
        Assert.assertTrue(adb.isRecordLoaded());
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies throw RecordNotFoundException if listing not allowed by default
     */
    @Test(expected = RecordNotFoundException.class)
    public void update_shouldThrowRecordNotFoundExceptionIfListingNotAllowedByDefault() throws Exception {
        ActiveDocumentBean adb = new ActiveDocumentBean();

        // Record will be released by a moving wall in 2041
        adb.setPersistentIdentifier("557335825");
        adb.setImageToShow("1");

        // Override config setting so that localhost doesn't get full access
        DataManager.getInstance().getConfiguration().overrideValue("accessConditions.fullAccessForLocalhost", false);

        adb.update();
        Assert.fail();
    }

}