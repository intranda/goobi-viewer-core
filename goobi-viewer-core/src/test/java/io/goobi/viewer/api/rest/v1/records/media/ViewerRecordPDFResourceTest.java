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
package io.goobi.viewer.api.rest.v1.records.media;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PDF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;

/**
 * @author florian
 *
 */
class ViewerRecordPDFResourceTest extends AbstractRestApiTest {
    private static final String PI_ACCESS_PAST_MOVING_WALL = "13473260X";
    private static final String PI_ACCESS_RESTRICTED = "557335825";
    private static final String PI = "02008031921530";

    /**
     * <p>setUp.</p>
     *
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * <p>tearDown.</p>
     *
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testGetPdf() {
        String url = urls.path(RECORDS_RECORD, RECORDS_PDF).params(PI).build();
        try (Response response = target(url)
                .request()
                .header("x-forwarded-for", "1.2.3.4")
                .accept("application/pdf")
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            byte[] entity = response.readEntity(byte[].class);
            String contentDisposition = response.getHeaderString("Content-Disposition");
            assertEquals("attachment; filename=\"" + PI + ".pdf" + "\"", contentDisposition);
            assertTrue(entity.length >= 5 * 5 * 8 * 3); //entity is at least as long as the image data
        }
    }

    @Test
    void testGetPdf_refuseAccess() {
        String url = urls.path(RECORDS_RECORD, RECORDS_PDF).params(PI_ACCESS_RESTRICTED).build();
        try (Response response = target(url)
                .request()
                .header("x-forwarded-for", "1.2.3.4")
                .accept("application/pdf")
                .get()) {
            assertEquals(403, response.getStatus(), "Should return status 403");
        }
    }
}
