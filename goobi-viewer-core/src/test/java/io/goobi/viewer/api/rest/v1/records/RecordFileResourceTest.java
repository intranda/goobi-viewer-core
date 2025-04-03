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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_SOURCE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TEI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author florian
 *
 */
class RecordFileResourceTest extends AbstractRestApiTest {

    private static final String PI = "PPN743674162";
    private static final String FILENAME = "00000010";

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getAlto(java.lang.String)}.
     */
    @Test
    void testGetAlto() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_ALTO).params(PI, FILENAME + ".xml").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getPlaintext(java.lang.String)}.
     */
    @Test
    void testGetPlaintext() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT).params(PI, FILENAME + ".xml").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordFileResource#getTEI(java.lang.String)}.
     */
    @Test
    void testGetTEI() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_TEI).params(PI, FILENAME + ".xml").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.TEXT_XML)
                .get()) {
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
            String data = response.readEntity(String.class);
            assertTrue(StringUtils.isNotBlank(data));
        }
    }

    @Test
    void testGetSourceFile() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "text.txt").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(200, response.getStatus(), response.getStatusInfo().getReasonPhrase());
            String contentType = response.getHeaderString("Content-Type");
            String entity = response.readEntity(String.class);
            assertEquals("text/plain", contentType);
            assertEquals("apples", entity.trim());
        }
    }

    @Test
    void testGetMissingSourceFile() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "bla.txt").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(404, response.getStatus(), response.getStatusInfo().getReasonPhrase());
        }
    }

    @Test
    void testGetSourceFilePathTraversalAttack() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_SOURCE).params(PI, "/../../../../..//etc/passwd").build();
        try (Response response = target(url)
                .request()
                .get()) {
            assertEquals(404, response.getStatus(), "Should return status 404");
        }
    }
}
