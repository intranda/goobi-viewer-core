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

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import jakarta.ws.rs.core.Response;

/**
 * @author florian
 *
 */
class ViewerRecordPDFResourceTest extends AbstractRestApiTest {
    private static final String PI_ACCESS_RESTRICTED = "557335825";
    private static final String PI = "02008031921530";

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        DataManager.getInstance()
                .injectConfiguration(new Configuration(new File("src/test/resources/config_viewer_no_local_access.test.xml").getAbsolutePath()));
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
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
            if (response.getStatus() >= 400) {
                String errorMessage = response.readEntity(String.class);
                Assertions.fail(errorMessage);
            } else {
                assertNotNull(response.getEntity(), "Should return user object as byte array");
                byte[] entity = response.readEntity(byte[].class);
                String contentDisposition = response.getHeaderString("Content-Disposition");
                assertEquals("attachment; filename=\"" + PI + ".pdf" + "\"", contentDisposition);
                assertTrue(entity.length >= 5 * 5 * 8 * 3); //entity is at least as long as the image data
            }
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
            String entity = response.readEntity(String.class);
            assertEquals(403, response.getStatus(), "Should return status 403; response: " + entity);
        }
    }
}
