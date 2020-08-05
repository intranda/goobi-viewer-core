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
package io.goobi.viewer.api.rest.v1.records.media;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE_PDF;
import static org.junit.Assert.*;
import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

import javax.ws.rs.core.Response;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.api.rest.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

/**
 * @author florian
 *
 */
public class ViewerRecordPDFResourceTest extends AbstractRestApiTest{
    private static final String PI_ACCESS_PAST_MOVING_WALL = "13473260X";
    private static final String PI_ACCESS_RESTRICTED = "557335825";
    private static final String PI = "02008031921530";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetPdf() {
        String url = urls.path(RECORDS_RECORD, RECORDS_PDF).params(PI).build();
        try (Response response = target(url)
                .request()
                .header("x-forwarded-for", "1.2.3.4")
                .accept("application/pdf")
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            byte[] entity = response.readEntity(byte[].class);
            String contentDisposition = response.getHeaderString("Content-Disposition");
            assertEquals("attachment; filename=\"" + PI + ".pdf" + "\"", contentDisposition);
            assertTrue(entity.length >= 5 * 5 * 8 * 3); //entity is at least as long as the image data
        }
    }
    
    @Test
    public void testGetPdf_refuseAccess() {
        String url = urls.path(RECORDS_RECORD, RECORDS_PDF).params(PI_ACCESS_RESTRICTED).build();
        try (Response response = target(url)
                .request()
                .header("x-forwarded-for", "1.2.3.4")
                .accept("application/pdf")
                .get()) {
            assertEquals("Should return status 403", 403, response.getStatus());
        }
    }

}
