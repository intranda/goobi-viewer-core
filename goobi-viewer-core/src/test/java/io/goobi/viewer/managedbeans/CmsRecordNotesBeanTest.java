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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
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
        super.tearDown();
    }

    @Test
    void testGetAllPaginatedData() {
        bean.getDataProvider().setEntriesPerPage(2);
        assertEquals(5, bean.getDataProvider().getSizeOfDataList());
        assertEquals(2, bean.getDataProvider().getPaginatorList().size());
        bean.getDataProvider().cmdMoveNext();
        assertEquals(2, bean.getDataProvider().getPaginatorList().size());
        bean.getDataProvider().cmdMoveNext();
        assertEquals(1, bean.getDataProvider().getPaginatorList().size());
    }

    @Test
    void testFilteredPaginatedData() {
        bean.getDataProvider().getFilter(CmsRecordNotesBean.PI_TITLE_FILTER).setValue("PI1");
        bean.getDataProvider().setEntriesPerPage(2);
        assertEquals(2, bean.getDataProvider().getSizeOfDataList());
        assertEquals(2, bean.getDataProvider().getPaginatorList().size());
    }

    @Test
    void testFilteredByTitlePaginatedData() {
        bean.getDataProvider().getFilter(CmsRecordNotesBean.PI_TITLE_FILTER).setValue("Bemerkungen 1");
        assertEquals(1, bean.getDataProvider().getSizeOfDataList());
        assertEquals(1, bean.getDataProvider().getPaginatorList().size());
    }

    @Test
    void testGetThumbnailUrl() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        CMSSingleRecordNote note = new CMSSingleRecordNote("PI1");
        String url = bean.getThumbnailUrl(note, 333, 444);
        String reference =
                urls.path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_IMAGE_IIIF).params("PI1", "full", "!333,444", "0", "default", "jpg").build();
        assertEquals(reference, url);
    }

}
