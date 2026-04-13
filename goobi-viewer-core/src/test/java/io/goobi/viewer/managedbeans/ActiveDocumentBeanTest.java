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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.exceptions.IllegalUrlParameterException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.ViewManager;

class ActiveDocumentBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(ActiveDocumentBeanTest.class);

    private ActiveDocumentBean adb;
    private NavigationHelper navigationHelper;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        //        ServletContext servletContext = Mockito.mock(ServletContext.class);
        //        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);

        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        //        Mockito.when(externalContext.getContext()).thenReturn(servletContext);
        //        Mockito.when(facesContext.getViewRoot()).thenReturn(viewRoot);
        //        Mockito.when(viewRoot.getLocale()).thenReturn(Locale.GERMAN);

        this.navigationHelper = Mockito.mock(NavigationHelper.class);
        Mockito.when(navigationHelper.getCurrentPage()).thenReturn("viewImage_value");
        Mockito.when(navigationHelper.getCurrentView()).thenReturn("viewImage_value");
        Mockito.when(navigationHelper.getPreferredView()).thenReturn("viewImage_value");

        adb = new ActiveDocumentBean();
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies create ViewManager correctly
     */
    @Test
    void update_shouldCreateViewManagerCorrectly() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();
        Assertions.assertNotNull(adb.getViewManager());
        assertEquals(PI_KLEIUNIV, adb.getPersistentIdentifier());
        assertEquals(PI_KLEIUNIV, adb.getViewManager().getPi());
        assertEquals(iddocKleiuniv, adb.getTopDocumentIddoc());
        assertEquals(iddocKleiuniv, adb.getViewManager().getTopStructElementIddoc());
        Assertions.assertNotEquals(iddocKleiuniv, adb.getViewManager().getCurrentStructElementIddoc());
        Assertions.assertNotNull(adb.getViewManager().getTopStructElement());
        assertEquals(adb.getTopDocument(), adb.getViewManager().getTopStructElement());
        Assertions.assertNotNull(adb.getViewManager().getCurrentStructElement());
        assertEquals(adb.getCurrentElement(), adb.getViewManager().getCurrentStructElement());
        assertEquals("", adb.getViewManager().getLogId());
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies set TOC on ViewManager after update
     */
    @Test
    void update_shouldSetTocOnViewManagerAfterUpdate() throws Exception {
        // TOC is now built outside the synchronized block (post-lock, via tocTarget).
        // Verify that the resulting TOC is published to the ViewManager before update() returns.
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();
        Assertions.assertNotNull(adb.getViewManager());
        Assertions.assertNotNull(adb.getViewManager().getToc(),
                "ViewManager.getToc() must be non-null after update() completes");
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies update ViewManager correctly if LOGID has changed
     */
    @Test
    void update_shouldUpdateViewManagerCorrectlyIfLOGIDHasChanged() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();
        Assertions.assertNotEquals(iddocKleiuniv, adb.getViewManager().getCurrentStructElementIddoc());
        ViewManager oldViewManager = adb.getViewManager();
        Assertions.assertSame(oldViewManager, adb.getViewManager());

        adb.setLogid("LOG_0003");
        adb.update();
        assertEquals(PI_KLEIUNIV, adb.getViewManager().getPi());
        assertEquals(iddocKleiuniv, adb.getViewManager().getTopStructElementIddoc());
        Assertions.assertNotEquals(iddocKleiuniv, adb.getViewManager().getCurrentStructElementIddoc());
        // assertEquals("LOG_0003", adb.getViewManager().getLogId());
        Assertions.assertNotSame(oldViewManager, adb.getViewManager());
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies not override topDocumentIddoc if LOGID has changed
     */
    @Test
    void update_shouldNotOverrideTopDocumentIddocIfLOGIDHasChanged() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();

        adb.setLogid("LOG_0005");
        try {
            adb.update();
        } catch (IDDOCNotFoundException e) {
        }
        assertEquals(iddocKleiuniv, adb.topDocumentIddoc);
    }

    /**
     * @see ActiveDocumentBean#setPersistentIdentifier(String)
     * @verifies determine currentElementIddoc correctly
     */
    @Test
    void setPersistentIdentifier_shouldDetermineCurrentElementIddocCorrectly() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        assertEquals(iddocKleiuniv, adb.topDocumentIddoc);
    }

    /**
     * @see ActiveDocumentBean#setPersistentIdentifier(String)
     * @verifies preserve lastReceivedIdentifier after reset when identifier not found
     */
    @Test
    void setPersistentIdentifier_shouldPreserveLastReceivedIdentifierWhenNotFound() throws Exception {
        // When an unknown identifier is set, reset() is called internally.
        // lastReceivedIdentifier must be restored afterwards so that update() throws
        // RecordNotFoundException with the actual identifier instead of '???'.
        String unknownPi = "this_identifier_does_not_exist";
        adb.setPersistentIdentifier(unknownPi);
        assertNull(adb.topDocumentIddoc);
        RecordNotFoundException ex = org.junit.jupiter.api.Assertions.assertThrows(RecordNotFoundException.class, () -> adb.update());
        assertEquals(unknownPi, ex.getMessage());
    }

    /**
     * @see ActiveDocumentBean#getNextUrl(String,int)
     * @verifies increase image number by given step
     */
    @Test
    void getNextUrl_shouldIncreaseImageNumberByGivenStep() throws Exception {
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("10");
        adb.update();
        assertEquals("/viewImage_value/PPN517154005/13/", adb.getPageUrlRelativeToCurrentPage(3));
    }

    /**
     * @see ActiveDocumentBean#getNextUrl(String,int)
     * @verifies go no higher than last page
     */
    @Test
    void getNextUrl_shouldGoNoHigherThanLastPage() throws Exception {
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("15");
        adb.update();
        assertEquals("/viewImage_value/PPN517154005/" + adb.getViewManager().getPageLoader().getLastPageOrder() + "/",
                adb.getPageUrlRelativeToCurrentPage(3));
    }

    /**
     * @see ActiveDocumentBean#getPrevUrl(String,int)
     * @verifies decrease image number by given step
     */
    @Test
    void getPrevUrl_shouldDecreaseImageNumberByGivenStep() throws Exception {
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("10");
        adb.update();
        assertEquals("/viewImage_value/PPN517154005/7/", adb.getPreviousPageUrl(3));
    }

    /**
     * @see ActiveDocumentBean#getPrevUrl(String,int)
     * @verifies go no lower than first page order
     */
    @Test
    void getPrevUrl_shouldGoNoLowerThanFirstPageOrder() throws Exception {
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("2");
        try {
            adb.update();
        } catch (IDDOCNotFoundException e) {
        }
        assertEquals("/viewImage_value/PPN517154005/" + adb.getViewManager().getPageLoader().getFirstPageOrder() + "/",
                adb.getPreviousPageUrl(4));
    }

    /**
     * @see ActiveDocumentBean#getPageUrl(String,int)
     * @verifies construct url correctly
     */
    @Test
    void getUrl_shouldConstructUrlCorrectly() throws Exception {
        adb.setNavigationHelper(navigationHelper);
        navigationHelper.setLocaleString("en");
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();
        assertEquals("/viewImage_value/PPN517154005/12/", adb.getPageUrl("12"));
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies load records that have been released via moving wall
     */
    @Test
    void update_shouldLoadRecordsThatHaveBeenReleasedViaMovingWall() throws Exception {
        // Record has been unlocked via a moving wall functionality and should load withour throwing a RecordNotFoundException.
        // The public release year metadata is only available in docstruct and page Solr docs. Here, it is important that none of the
        // record's docs match the conditional query of the license type anyway.
        adb.setPersistentIdentifier("1045513032");
        adb.setImageToShow("1");

        // Override config setting so that localhost doesn't get full access
        DataManager.getInstance().getConfiguration().overrideValue("accessConditions.fullAccessForLocalhost", false);
        Assertions.assertFalse(DataManager.getInstance().getConfiguration().isFullAccessForLocalhost());

        adb.update();
        assertTrue(adb.isRecordLoaded());
    }

    /**
     * @see ActiveDocumentBean#update()
     * @verifies throw RecordNotFoundException if listing not allowed by default
     */
    @Test
    void update_shouldThrowRecordNotFoundExceptionIfListingNotAllowedByDefault() throws Exception {
        // Record will be released by a moving wall in 2041
        adb.setPersistentIdentifier("557335825");
        adb.setImageToShow("1");

        // Override config setting so that localhost doesn't get full access
        DataManager.getInstance().getConfiguration().overrideValue("accessConditions.fullAccessForLocalhost", false);
        Assertions.assertFalse(DataManager.getInstance().getConfiguration().isFullAccessForLocalhost());

        Assertions.assertThrows(RecordNotFoundException.class, () -> adb.update());
    }

    /**
     * @see ActiveDocumentBean#getPageUrlRelativeToCurrentPage(int)
     * @verifies return correct page in single page mode
     */
    @Test
    void getPageUrl_shouldReturnCorrectPageInSinglePageMode() throws Exception {
        adb.setPersistentIdentifier(AbstractSolrEnabledTest.PI_KLEIUNIV);
        adb.setImageToShow("2");
        adb.update();
        assertTrue(adb.isRecordLoaded());

        // Next page (2 -> 3)
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/3/",
                adb.getPageUrlRelativeToCurrentPage(1));
        // Previous page (2 -> 1)
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/1/",
                adb.getPageUrlRelativeToCurrentPage(-1));
    }

    /**
     * @see ActiveDocumentBean#getPageUrlRelativeToCurrentPage(int)
     * @verifies return correct range in double page mode if currently showing one page
     */
    @Test
    void getPageUrl_shouldReturnCorrectRangeInDoublePageModeIfCurrentlyShowingOnePage() throws Exception {
        adb.setPersistentIdentifier(AbstractSolrEnabledTest.PI_KLEIUNIV);

        adb.setImageToShow("1");
        adb.update();
        assertTrue(adb.isRecordLoaded());
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/2/",
                adb.getPageUrlRelativeToCurrentPage(1));

        // Next page (1 -> 2-3)
        adb.setImageToShow("1-1");
        adb.update();
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/2-3/",
                adb.getPageUrlRelativeToCurrentPage(1));

        // Previous page (16 -> 14-15)
        adb.setImageToShow("16");
        adb.update();
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/15/",
                adb.getPageUrlRelativeToCurrentPage(-1));

        // Same in right-to-left
        adb.getViewManager().getTopStructElement().setRtl(true);
        // Next page (1 -> 2-3)
        adb.setImageToShow("1-1");
        adb.update();
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/2-3/",
                adb.getPageUrlRelativeToCurrentPage(1));
        // Previous page (16 -> 14-15)
        adb.setImageToShow("16-17");
        adb.update();
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/14-15/",
                adb.getPageUrlRelativeToCurrentPage(-1));

    }

    /**
     * @see ActiveDocumentBean#getPageUrlRelativeToCurrentPage(int)
     * @verifies return correct range in double page mode if currently showing two pages
     */
    @Test
    void getPageUrl_shouldReturnCorrectRangeInDoublePageModeIfCurrentlyShowingTwoPages() throws Exception {
        adb.setPersistentIdentifier(AbstractSolrEnabledTest.PI_KLEIUNIV);
        adb.setImageToShow("4-5");
        adb.update();
        assertTrue(adb.isRecordLoaded());

        adb.getViewManager().setDoublePageMode(true);

        // Next page (4-5 -> 6-7)
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/6-7/",
                adb.getPageUrlRelativeToCurrentPage(1));
        // Previous page (4-5 -> 2-3)
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/2-3/",
                adb.getPageUrlRelativeToCurrentPage(-1));
    }

    /**
     * @see ActiveDocumentBean#getPageUrlRelativeToCurrentPage(int)
     * @verifies return correct range in double page mode if current page double image
     */
    @Test
    void getPageUrl_shouldReturnCorrectRangeInDoublePageModeIfCurrentPageDoubleImage() throws Exception {
        adb.setPersistentIdentifier(AbstractSolrEnabledTest.PI_KLEIUNIV);
        adb.setImageToShow("3");
        adb.update();
        assertTrue(adb.isRecordLoaded());

        adb.getViewManager().setDoublePageMode(true);
        adb.getViewManager().getCurrentPage().setDoubleImage(true);

        // Next page (3 -> 4-5)
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/4-5/",
                adb.getPageUrlRelativeToCurrentPage(1));
        // Previous page (3 -> 1-2)
        assertEquals("/" + PageType.viewObject.getName() + "/" + AbstractSolrEnabledTest.PI_KLEIUNIV + "/1-2/",
                adb.getPageUrlRelativeToCurrentPage(-1));
    }

    /**
     * @see ActiveDocumentBean#reset()
     * @verifies reset lastReceivedIdentifier
     */
    @Test
    void reset_shouldResetLastReceivedIdentifier() throws Exception {
        adb.setLastReceivedIdentifier("PPN123");
        assertEquals("PPN123", adb.getLastReceivedIdentifier());
        adb.reset();
        Assertions.assertNull(adb.getLastReceivedIdentifier());
    }

    /**
     * @see ActiveDocumentBean#getRelativeUrlTags()
     * @verifies return empty string if no record loaded
     */
    @Test
    void getRelativeUrlTags_shouldReturnEmptyStringIfNoRecordLoaded() throws Exception {
        assertEquals("", adb.getRelativeUrlTags());
    }

    /**
     * @see ActiveDocumentBean#getRelativeUrlTags()
     * @verifies return empty string if navigationHelper null
     */
    @Test
    void getRelativeUrlTags_shouldReturnEmptyStringIfNavigationHelperNull() throws Exception {
        adb.setPersistentIdentifier(AbstractSolrEnabledTest.PI_KLEIUNIV);
        adb.setImageToShow("3");
        adb.update();
        adb.setNavigationHelper(null);
        assertTrue(adb.isRecordLoaded());
        assertEquals("", adb.getRelativeUrlTags());
    }

    /**
     * @see ActiveDocumentBean#getRelativeUrlTags()
     * @verifies generate tags correctly
     */
    @Test
    void getRelativeUrlTags_shouldGenerateTagsCorrectly() throws Exception {
        adb.setPersistentIdentifier(AbstractSolrEnabledTest.PI_KLEIUNIV);
        adb.setImageToShow("3");
        adb.update();
        assertTrue(adb.isRecordLoaded());

        adb.setNavigationHelper(new NavigationHelper());

        String linkCanonical = "\n<link rel=\"canonical\" href=\"";
        String linkAlternate = "\n<link rel=\"alternate\" href=\"";

        String result = adb.getRelativeUrlTags();
        assertTrue(StringUtils.isNotBlank(result));
    }

    /**
     * @see ActiveDocumentBean#getToc()
     * @verifies return null when viewManager is null
     */
    @Test
    void getToc_shouldReturnNullWhenViewManagerIsNull() throws Exception {
        // Fresh bean has viewManager == null
        assertNull(adb.getToc());
    }

    /**
     * @see ActiveDocumentBean#createTOC()
     * @verifies return empty TOC without NPE when viewManager is null
     */
    @Test
    void createTOC_shouldReturnEmptyTocWhenViewManagerIsNull() throws Exception {
        // createTOC() reads this.viewManager once into a local variable (TOCTOU fix).
        // Before the fix, reset() on another thread could null out viewManager between
        // the null-check and getMimeType() access, causing an NPE.  We cannot reproduce
        // that race deterministically, but we verify the boundary condition: with
        // viewManager == null (as it would be after reset()), createTOC() must return a
        // non-null empty TOC rather than throw.
        java.lang.reflect.Method m = ActiveDocumentBean.class.getDeclaredMethod("createTOC");
        m.setAccessible(true);
        // viewManager is null on a fresh bean — the condition after a concurrent reset()
        Object result = m.invoke(adb);
        Assertions.assertNotNull(result, "createTOC() must return a non-null empty TOC when viewManager is null");
    }

    /**
     * @see ActiveDocumentBean#setTocCurrentPage(String)
     * @verifies throw IllegalUrlParameterException for non-numeric value
     */
    @Test
    void setTocCurrentPage_shouldThrowIllegalUrlParameterExceptionForNonNumericValue() {
        // A record PI like "15849354_1940" (periodical volume) can end up in the
        // tocCurrentPage URL slot when a mismatched PrettyFaces pattern injects it.
        // The setter must reject it with IllegalUrlParameterException so that
        // MyExceptionHandler treats it as an invalid URL (WARN) rather than ERROR.
        Assertions.assertThrows(IllegalUrlParameterException.class, () -> adb.setTocCurrentPage("15849354_1940"));
        Assertions.assertThrows(IllegalUrlParameterException.class, () -> adb.setTocCurrentPage("15592719_2011_00"));
        Assertions.assertThrows(IllegalUrlParameterException.class, () -> adb.setTocCurrentPage("notANumber"));
    }

    /**
     * @see ActiveDocumentBean#getFullscreenImageUrl()
     * @verifies not throw NPE when getCurrentPage returns null in double page mode
     */
    @Test
    void getFullscreenImageUrl_shouldNotThrowNPEWhenCurrentPageIsNull() throws Exception {
        // Simulate a ViewManager in double-page mode where the page loader hasn't
        // populated the current page yet (getCurrentPage() == null).
        ViewManager mockViewManager = Mockito.mock(ViewManager.class);
        Mockito.when(mockViewManager.isDoublePageMode()).thenReturn(true);
        Mockito.when(mockViewManager.getCurrentPage()).thenReturn(null);

        adb.setNavigationHelper(navigationHelper);
        // viewManager is private; inject via reflection
        java.lang.reflect.Field vmField = ActiveDocumentBean.class.getDeclaredField("viewManager");
        vmField.setAccessible(true);
        vmField.set(adb, mockViewManager);

        // Must not throw NullPointerException
        Assertions.assertDoesNotThrow(() -> adb.getFullscreenImageUrl());
    }

    /**
     * Verify that unsynchronized read-only getters do not throw under concurrent access
     * after a record has been loaded.
     *
     * @see ActiveDocumentBean#getPersistentIdentifier()
     * @see ActiveDocumentBean#getTocCurrentPage()
     * @see ActiveDocumentBean#getLogid()
     * @see ActiveDocumentBean#getAction()
     * @see ActiveDocumentBean#getCurrentThumbnailPage()
     */
    @Test
    void getters_shouldBeThreadSafeAfterRecordLoad() throws Exception {
        adb.setNavigationHelper(navigationHelper);
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();

        int threadCount = 10;
        java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(exec.submit(() -> {
                // These must all return consistent, non-null values concurrently
                String pi = adb.getPersistentIdentifier();
                String tocPage = adb.getTocCurrentPage();
                String logid = adb.getLogid();
                String action = adb.getAction();
                int thumbPage = adb.getCurrentThumbnailPage();
                return pi + "|" + tocPage + "|" + logid + "|" + action + "|" + thumbPage;
            }));
        }

        exec.shutdown();
        exec.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        for (java.util.concurrent.Future<String> f : futures) {
            String result = f.get(); // propagates any exceptions thrown in reader threads
            Assertions.assertTrue(result.startsWith(PI_KLEIUNIV + "|"),
                    "getPersistentIdentifier() must return the loaded PI; got: " + result);
            Assertions.assertTrue(result.contains("|1|"),
                    "getTocCurrentPage() must return '1'; got: " + result);
        }
    }

    /**
     * @see ActiveDocumentBean#getGeoMap()
     * @verifies return non-null GeoMap when no record is loaded
     */
    @Test
    void getGeoMap_shouldReturnNonNullGeoMapWhenNoRecordLoaded() throws Exception {
        // Fresh bean has viewManager == null; getRecordGeoMap() returns an empty RecordGeoMap
        // whose getGeoMap() returns a new (empty) GeoMap — not null
        assertFalse(adb.isRecordLoaded());
        Assertions.assertNotNull(adb.getGeoMap());
    }

    /**
     * @see ActiveDocumentBean#getGeoMap()
     * @verifies return GeoMap for loaded record
     */
    @Test
    void getGeoMap_shouldReturnGeoMapForLoadedRecord() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.update();
        assertTrue(adb.isRecordLoaded());
        // GeoMap may be empty (no geo-coordinate fields configured in test), but must not be null
        Assertions.assertNotNull(adb.getGeoMap());
    }

    /**
     * @see ActiveDocumentBean#getRecordGeoMap()
     * @verifies cache result for same PI
     */
    @Test
    void getRecordGeoMap_shouldCacheResultForSamePI() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.update();
        assertTrue(adb.isRecordLoaded());
        // Two calls must return the same RecordGeoMap instance (cached after first build)
        io.goobi.viewer.model.maps.RecordGeoMap first = adb.getRecordGeoMap();
        io.goobi.viewer.model.maps.RecordGeoMap second = adb.getRecordGeoMap();
        Assertions.assertSame(first, second, "getRecordGeoMap() must return the cached instance on repeated calls");
    }

    /**
     * @see ActiveDocumentBean#isAllowUserComments()
     * @verifies return false when no record is loaded
     */
    @Test
    void isAllowUserComments_shouldReturnFalseWhenNoRecordLoaded() throws Exception {
        assertFalse(adb.isRecordLoaded());
        assertFalse(adb.isAllowUserComments());
    }

    /**
     * @see ActiveDocumentBean#isAllowUserComments()
     * @verifies return false when comments are disabled globally
     */
    @Test
    void isAllowUserComments_shouldReturnFalseWhenCommentsDisabledGlobally() throws Exception {
        // Test DB: core CommentGroup exists (core_type=1) but has enabled=0 → disabled globally
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.update();
        assertTrue(adb.isRecordLoaded());
        assertFalse(adb.isAllowUserComments());
    }

    /**
     * @see ActiveDocumentBean#isAllowUserComments()
     * @verifies not throw NPE if allowUserComments is reset concurrently
     */
    @Test
    void isAllowUserComments_shouldNotThrowNPEIfAllowUserCommentsResetConcurrently() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.update();
        assertTrue(adb.isRecordLoaded());
        // Prime the cache (returns false: comments globally disabled in test DB)
        assertFalse(adb.isAllowUserComments());
        // Simulate the race: resetAllowUserComments() sets allowUserComments back to null
        adb.resetAccess();
        // The next call must not throw NullPointerException (Boolean.TRUE.equals handles null).
        // Re-evaluates: still disabled globally → false.
        assertFalse(adb.isAllowUserComments());
    }

    /**
     * @see ActiveDocumentBean#setRepresentativeImage()
     * @verifies use default image "1" when no identifier is set
     */
    @Test
    void setRepresentativeImage_shouldUseDefaultImageWhenNoIdentifierSet() throws Exception {
        // lastReceivedIdentifier is null → skip Solr, image stays "1"
        Mockito.when(navigationHelper.getCurrentPageType()).thenReturn(io.goobi.viewer.model.viewer.PageType.viewImage);
        adb.setNavigationHelper(navigationHelper);
        adb.setRepresentativeImage();
        assertEquals("1", adb.getImageToShow());
    }

    /**
     * @see ActiveDocumentBean#setRepresentativeImage()
     * @verifies use default image "1" when identifier is the dash sentinel
     */
    @Test
    void setRepresentativeImage_shouldUseDefaultImageWhenIdentifierIsDashSentinel() throws Exception {
        // lastReceivedIdentifier == "-" is treated as "no record" → skip Solr, image stays "1"
        Mockito.when(navigationHelper.getCurrentPageType()).thenReturn(io.goobi.viewer.model.viewer.PageType.viewImage);
        adb.setNavigationHelper(navigationHelper);
        adb.setLastReceivedIdentifier("-");
        adb.setRepresentativeImage();
        assertEquals("1", adb.getImageToShow());
    }

    /**
     * @see ActiveDocumentBean#getRelativeUrlTags()
     * @verifies generate canonical URL without page number for first page
     */
    @Test
    void getRelativeUrlTags_shouldGenerateCanonicalWithoutPageNumberForFirstPage() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("1");
        adb.update();
        assertTrue(adb.isRecordLoaded());

        // Use a real NavigationHelper so getCurrentView() returns a consistent value
        NavigationHelper nh = new NavigationHelper();
        // viewImage page type; getName() returns the configured or raw name ("image")
        nh.setCurrentView(PageType.viewImage.getName());
        adb.setNavigationHelper(nh);

        String result = adb.getRelativeUrlTags();
        String linkCanonical = "\n<link rel=\"canonical\" href=\"";
        String linkEnd = "\" />";
        assertTrue(result.contains(linkCanonical), "Expected a canonical link tag in result, got: " + result);

        int start = result.indexOf(linkCanonical) + linkCanonical.length();
        int end = result.indexOf(linkEnd, start);
        String canonicalHref = result.substring(start, end);
        // Page 1 canonical must contain the PI but must not append the page number
        assertTrue(canonicalHref.contains(PI_KLEIUNIV), "Canonical URL must contain the PI");
        assertFalse(canonicalHref.endsWith("/1/"), "Canonical URL for page 1 must not end with /1/");
    }

    /**
     * @see ActiveDocumentBean#getRelativeUrlTags()
     * @verifies generate canonical URL with page number for non-first page
     */
    @Test
    void getRelativeUrlTags_shouldGenerateCanonicalWithPageNumberForNonFirstPage() throws Exception {
        adb.setPersistentIdentifier(PI_KLEIUNIV);
        adb.setImageToShow("3");
        adb.update();
        assertTrue(adb.isRecordLoaded());

        NavigationHelper nh = new NavigationHelper();
        nh.setCurrentView(PageType.viewImage.getName());
        adb.setNavigationHelper(nh);

        String result = adb.getRelativeUrlTags();
        String linkCanonical = "\n<link rel=\"canonical\" href=\"";
        String linkEnd = "\" />";
        assertTrue(result.contains(linkCanonical), "Expected a canonical link tag in result, got: " + result);

        int start = result.indexOf(linkCanonical) + linkCanonical.length();
        int end = result.indexOf(linkEnd, start);
        String canonicalHref = result.substring(start, end);
        // Page 3 canonical must end with the page number
        assertTrue(canonicalHref.endsWith("/3/"), "Canonical URL for page 3 must end with /3/, got: " + canonicalHref);
    }
}
