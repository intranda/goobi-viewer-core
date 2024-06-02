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
package io.goobi.viewer.model.job.download;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.dao.impl.JPADAO;

class DownloadJobTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see DownloadJob#isExpired()
     * @verifies return correct value
     */
    @Test
    void isExpired_shouldReturnCorrectValue() throws Exception {
        DownloadJob job = new PDFDownloadJob("PI_3", null, LocalDateTime.now(), 0);
        Thread.sleep(5);
        Assertions.assertTrue(job.isExpired());
        job.setTtl(30000);
        Assertions.assertFalse(job.isExpired());
    }

    /**
     * @see DownloadJob#generateDownloadJobId(String[])
     * @verifies generate same id from same criteria
     */
    @Test
    void generateDownloadJobId_shouldGenerateSameIdFromSameCriteria() throws Exception {
        String hash = "07319d093ea0e44a618cdf3accb9576009025f7ea7ed3b6765192f1ddca6a801";
        String crit1 = "PPN123456789";
        String crit2 = "LOG_0000";
        Assertions.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
        Assertions.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
        Assertions.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
    }

    /**
     * @see JPADAO#generateDownloadJobId(long)
     * @verifies throw IllegalArgumentException if type or pi or downloadIdentifier null
     */
    @Test
    void generateDownloadJobId_shouldThrowIllegalArgumentExceptionIfTypeOrPiOrDownloadIdentifierNull() throws Exception {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DownloadJob.checkDownload(null, null, "PPN123", null, "foo", 5));
        assertEquals("type may not be null", e.getMessage());

        e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DownloadJob.checkDownload(PDFDownloadJob.LOCAL_TYPE, null, null, null, "foo", 5));
        assertEquals("pi may not be null", e.getMessage());

        e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DownloadJob.checkDownload(PDFDownloadJob.LOCAL_TYPE, null, "PPN123", null, null, 5));
        assertEquals("downloadIdentifier may not be null", e.getMessage());
    }

    /**
     * @see JPADAO#generateDownloadJobId(long)
     * @verifies throw IllegalArgumentException if downloadIdentifier mismatches pattern
     */
    @Test
    void generateDownloadJobId_shouldThrowIllegalArgumentExceptionIfDownloadIdentifierMismatchesPattern() throws Exception {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DownloadJob.checkDownload(PDFDownloadJob.LOCAL_TYPE, null, "PPN123", null, "foo", 5));
        assertEquals("wrong downloadIdentifier", e.getMessage());
    }

    /**
     * @see JPADAO#generateDownloadJobId(long)
     * @verifies throw IllegalArgumentException if type unknown
     */
    @Test
    void generateDownloadJobId_shouldThrowIllegalArgumentExceptionIfTypeUnknown() throws Exception {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DownloadJob.checkDownload("OTHER", null, "PPN123", null,
                        "b252906130dc48aa1410e349826e81192d3dbfe68c163dec5c474d17b3d8b0eb", 5));
        assertEquals("Unknown type: OTHER", e.getMessage());
    }

    @Test
    void testPutDownloadJobAnswer() throws JsonProcessingException {
        String pi = "18979459_1830";
        String logid = "LOG_0004";
        DownloadJob job = new PDFDownloadJob(pi, logid, LocalDateTime.now(), 1000);
        job.setMessage("Some message");
        job.getObservers().add("me@he.re");
        String jobString = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(job);
        JSONObject json = new JSONObject(jobString);
        assertEquals("pdf", json.get("type"));
        assertEquals(pi, json.get("pi"));
        assertEquals(logid, json.get("logId"));
    }
}
