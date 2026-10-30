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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.archives.ArchiveManager;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.cms.recordnotes.CMSMultiRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSSingleRecordNote;

/**
 * @author florian
 *
 */
class CmsRecordNotesBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    CmsRecordNotesBean bean;
    AbstractApiUrlManager urls;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        urls = DataManager.getInstance().getRestApiManager().getContentApiManager().orElse(null);
        ImageDeliveryBean images = new ImageDeliveryBean();
        images.init(DataManager.getInstance().getConfiguration(), urls, urls);
        bean = new CmsRecordNotesBean(images);
        bean.init();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        DataManager.getInstance().injectArchiveManager(null);
        super.tearDown();
    }

    private static void injectArchive(String resourceId) {
        ArchiveManager archiveManager = mock(ArchiveManager.class);
        when(archiveManager.getArchive(resourceId)).thenReturn(new ArchiveResource(resourceId, resourceId, "", "1"));
        DataManager.getInstance().injectArchiveManager(archiveManager);
    }

    /**
     * @verifies get all paginated data
     * @see CmsRecordNotesBean#getDataProvider
     */
    @Test
    void getDataProvider_shouldGetAllPaginatedData() {
        bean.getDataProvider().setEntriesPerPage(2);
        assertEquals(5, bean.getDataProvider().getSizeOfDataList());
        assertEquals(2, bean.getDataProvider().getPaginatorList().size());
        bean.getDataProvider().cmdMoveNext();
        assertEquals(2, bean.getDataProvider().getPaginatorList().size());
        bean.getDataProvider().cmdMoveNext();
        assertEquals(1, bean.getDataProvider().getPaginatorList().size());
    }

    /**
     * @verifies return filtered paginated data
     * @see CmsRecordNotesBean#getDataProvider
     */
    @Test
    void getDataProvider_shouldReturnFilteredPaginatedData() {
        bean.getDataProvider().getFilter(CmsRecordNotesBean.PI_TITLE_FILTER).setValue("PI1");
        bean.getDataProvider().setEntriesPerPage(2);
        assertEquals(2, bean.getDataProvider().getSizeOfDataList());
        assertEquals(2, bean.getDataProvider().getPaginatorList().size());
    }

    /**
     * @verifies return filtered by title paginated data
     * @see CmsRecordNotesBean#getDataProvider
     */
    @Test
    void getDataProvider_shouldReturnFilteredByTitlePaginatedData() {
        bean.getDataProvider().getFilter(CmsRecordNotesBean.PI_TITLE_FILTER).setValue("Bemerkungen 1");
        assertEquals(1, bean.getDataProvider().getSizeOfDataList());
        assertEquals(1, bean.getDataProvider().getPaginatorList().size());
    }

    /**
     * @verifies return expected value for given input
     * @see CmsRecordNotesBean#getThumbnailUrl(CMSSingleRecordNote)
     */
    @Test
    void getThumbnailUrl_shouldReturnExpectedValueForGivenInput() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        CMSSingleRecordNote note = new CMSSingleRecordNote(AbstractSolrEnabledTest.PI_KLEIUNIV);
        String url = bean.getThumbnailUrl(note, 333, 444);
        String reference =
                urls.path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_IMAGE_IIIF)
                        .params(AbstractSolrEnabledTest.PI_KLEIUNIV, "full", "!333,444", "0", "default", "jpg")
                        .build()
                        .replace("representative", "files/images/00000001.tif"); // TODO replace hack
        assertEquals(reference, url);
    }

    /**
     * @verifies resolve variables in link targets and text
     * @see CmsRecordNotesBean#resolveNoteVariables
     */
    @Test
    void resolveNoteVariables_shouldResolveVariablesInLinkTargetsAndText() {
        String text = "<p>Seite {PAGE_URL}: <a href=\"https://order.example/form.do?id=12&amp;card_link={IMAGE_URL}\">Bestellen</a>"
                + " <a href=\"{PAGE_URL}\">Ansehen</a></p>";

        String resolved = bean.resolveNoteVariables(text, "https://viewer.example/image/PPN1/57/",
                "https://viewer.example/api/v1/records/PPN1/files/images/00000057.jpg/full/!1252,2048/0/default.jpg");

        assertEquals("<p>Seite https://viewer.example/image/PPN1/57/: <a href=\"https://order.example/form.do?id=12&amp;card_link="
                + "https%3A%2F%2Fviewer.example%2Fapi%2Fv1%2Frecords%2FPPN1%2Ffiles%2Fimages%2F00000057.jpg%2Ffull%2F!1252%2C2048%2F0%2Fdefault.jpg\">"
                + "Bestellen</a> <a href=\"https://viewer.example/image/PPN1/57/\">Ansehen</a></p>", resolved);
    }

    /**
     * @verifies resolve url encoded variables in link targets
     * @see CmsRecordNotesBean#resolveNoteVariables
     */
    @Test
    void resolveNoteVariables_shouldResolveUrlEncodedVariablesInLinkTargets() {
        String resolved = bean.resolveNoteVariables("<a href=\"https://order.example/?card_link=%7bIMAGE_URL%7D\">Bestellen</a>", "",
                "https://viewer.example/image.jpg");

        assertEquals("<a href=\"https://order.example/?card_link=https%3A%2F%2Fviewer.example%2Fimage.jpg\">Bestellen</a>", resolved);
    }

    /**
     * @verifies return null if a variable has no value
     * @see CmsRecordNotesBean#resolveNoteVariables
     */
    @Test
    void resolveNoteVariables_shouldReturnNullIfAVariableHasNoValue() {
        assertNull(bean.resolveNoteVariables("<a href=\"https://order.example/?card_link={IMAGE_URL}\">Bestellen</a>",
                "https://viewer.example/image/PPN1/1/", ""));
        assertNull(bean.resolveNoteVariables("<p>Seite: {PAGE_URL}</p>", " ", "https://viewer.example/image.jpg"));
    }

    /**
     * @verifies return text without variables unchanged
     * @see CmsRecordNotesBean#resolveNoteVariables
     */
    @Test
    void resolveNoteVariables_shouldReturnTextWithoutVariablesUnchanged() {
        String text = "<p>Hinweis<br>ohne <b>Variable</b></p>";
        assertSame(text, bean.resolveNoteVariables(text, "https://viewer.example/", "https://viewer.example/image.jpg"));
    }

    /**
     * @verifies keep variables for the client and hide notes with variables elsewhere when values are missing
     * @see CmsRecordNotesBean#resolveNoteText
     */
    @Test
    void resolveNoteText_shouldKeepVariablesForTheClientAndHideNotesWithVariablesElsewhereWhenValuesAreMissing() {
        String text = "<a href=\"https://order.example/?card_link={IMAGE_URL}\">Bestellen</a>";
        String staticText = "<a href=\"https://help.example/\">Hilfe</a>";

        assertSame(text, bean.resolveNoteText(text, "client"));
        assertNull(bean.resolveNoteText(text, "none"));
        assertNull(bean.resolveNoteText(text, "page"));
        assertSame(staticText, bean.resolveNoteText(staticText, "none"));
    }

    /**
     * @verifies return true only for single notes whose identifier is an archive
     * @see CmsRecordNotesBean#isArchiveNote
     */
    @Test
    void isArchiveNote_shouldReturnTrueOnlyForSingleNotesWhoseIdentifierIsAnArchive() {
        injectArchive("ARCHIVE_1");

        assertTrue(bean.isArchiveNote(new CMSSingleRecordNote("ARCHIVE_1")));
        assertFalse(bean.isArchiveNote(new CMSSingleRecordNote("PI1")));
        assertFalse(bean.isArchiveNote(new CMSMultiRecordNote("PI:ARCHIVE_1")));
        assertFalse(bean.isArchiveNote(null));
    }

    /**
     * @verifies return the archive url for a note of an archive
     * @see CmsRecordNotesBean#getRecordUrl
     */
    @Test
    void getRecordUrl_shouldReturnTheArchiveUrlForANoteOfAnArchive() throws Exception {
        injectArchive("ARCHIVE_1");
        NavigationHelper navigationHelper = mock(NavigationHelper.class);
        when(navigationHelper.getApplicationUrl()).thenReturn("http://localhost:8080/viewer/");
        FieldUtils.writeField(bean, "navigationHelper", navigationHelper, true);

        assertEquals("http://localhost:8080/viewer/archives/ARCHIVE_1/", bean.getRecordUrl(new CMSSingleRecordNote("ARCHIVE_1")));
    }

    /**
     * @verifies return an empty string if the archive has no tile image
     * @see CmsRecordNotesBean#getArchiveImageUrl
     */
    @Test
    void getArchiveImageUrl_shouldReturnAnEmptyStringIfTheArchiveHasNoTileImage() throws Exception {
        assertEquals("", bean.getArchiveImageUrl(new CMSSingleRecordNote("ARCHIVE_WITHOUT_CONFIG")));
        assertEquals("", bean.getArchiveImageUrl(new CMSMultiRecordNote("PI:ARCHIVE_1")));
    }

}
