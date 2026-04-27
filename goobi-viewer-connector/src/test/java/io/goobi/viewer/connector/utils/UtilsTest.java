/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.utils;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.enums.Verb;

class UtilsTest extends AbstractTest {

    /**
     * @see Utils#splitIdentifierAndLanguageCode(String,int)
     * @verifies split identifier correctly
     */
    @Test
    void splitIdentifierAndLanguageCode_shouldSplitIdentifierCorrectly() throws Exception {
        {
            String[] result = Utils.splitIdentifierAndLanguageCode("id_eng", 3);
            Assertions.assertEquals(2, result.length);
            Assertions.assertEquals("id", result[0]);
            Assertions.assertEquals("eng", result[1]);
        }
        {
            String[] result = Utils.splitIdentifierAndLanguageCode("id", 3);
            Assertions.assertEquals(2, result.length);
            Assertions.assertEquals("id", result[0]);
            Assertions.assertNull(result[1]);
        }

    }

    /**
     * @see Utils#convertDate(long)
     * @verifies convert time correctly
     */
    @Test
    void convertDate_shouldConvertTimeCorrectly() throws Exception {
        Assertions.assertEquals("2016-05-23T10:40:00Z", Utils.convertDate(1464000000000L));
    }
    
    /**
     * @see Utils#parseDate(Object)
     * @verifies parse dates correctly
     */
    @Test
    void parseDate_shouldParseDatesCorrectly() throws Exception {
        Assertions.assertEquals("2016-05-23T10:40:00Z", Utils.parseDate(1464000000000L));
    }

    /**
     * @see Utils#getCurrentUTCTime(long)
     * @verifies format time correctly
     */
    @Test
    void getCurrentUTCTime_shouldFormatTimeCorrectly() throws Exception {
        Assertions.assertEquals("2020-09-07T14:30:00Z", Utils.getCurrentUTCTime(LocalDateTime.of(2020, 9, 7, 14, 30, 00)));
    }
    

    /**
     * @see Utils#getCurrentUTCTime(LocalDateTime)
     * @verifies truncate to seconds
     */
    @Test
    void getCurrentUTCTime_shouldTruncateToSeconds() throws Exception {
        Assertions.assertEquals("2020-09-07T14:30:00Z", Utils.getCurrentUTCTime(LocalDateTime.of(2020, 9, 7, 14, 30, 00, 1000000)));
    }

    /**
     * @see Utils#cleanUpTimestamp(String)
     * @verifies clean up timestamp correctly
     */
    @Test
    void cleanUpTimestamp_shouldCleanUpTimestampCorrectly() throws Exception {
        Assertions.assertEquals("20201028133500", Utils.cleanUpTimestamp("2020-10-28T13:35:00"));
        Assertions.assertEquals("20201028133500", Utils.cleanUpTimestamp("2020-10-28T13:35:00.000"));
    }

    /**
     * @see Utils#filterDatestampFromRequest(RequestHandler)
     * @verifies contain from timestamp
     */
    @Test
    void filterDatestampFromRequest_shouldContainFromTimestamp() throws Exception {
        RequestHandler rh = new RequestHandler();
        rh.setFrom("2022-11-04T16:00:00Z");
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(rh);
        Assertions.assertEquals("20221104160000", datestamp.get("from"));
    }

    /**
     * @see Utils#filterDatestampFromRequest(RequestHandler)
     * @verifies contain until timestamp
     */
    @Test
    void filterDatestampFromRequest_shouldContainUntilTimestamp() throws Exception {
        RequestHandler rh = new RequestHandler();
        rh.setUntil("2022-11-04T16:59:59Z");
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(rh);
        Assertions.assertEquals("20221104165959", datestamp.get("until"));
    }

    /**
     * @see Utils#filterDatestampFromRequest(RequestHandler)
     * @verifies contain set
     */
    @Test
    void filterDatestampFromRequest_shouldContainSet() throws Exception {
        RequestHandler rh = new RequestHandler();
        rh.setSet("varia");
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(rh);
        Assertions.assertEquals("varia", datestamp.get("set"));
    }

    /**
     * @see Utils#filterDatestampFromRequest(RequestHandler)
     * @verifies contain metadataPrefix
     */
    @Test
    void filterDatestampFromRequest_shouldContainMetadataPrefix() throws Exception {
        RequestHandler rh = new RequestHandler();
        rh.setMetadataPrefix(Metadata.OAI_DC);
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(rh);
        Assertions.assertEquals(Metadata.OAI_DC.getMetadataPrefix(), datestamp.get("metadataPrefix"));
    }

    /**
     * @see Utils#filterDatestampFromRequest(RequestHandler)
     * @verifies contain verb
     */
    @Test
    void filterDatestampFromRequest_shouldContainVerb() throws Exception {
        RequestHandler rh = new RequestHandler();
        rh.setVerb(Verb.GETRECORD);
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(rh);
        Assertions.assertEquals(Verb.GETRECORD.getTitle(), datestamp.get("verb"));
    }

    /**
     * @see Utils#formatVersionString(String)
     * @verifies format string correctly
     */
    //@Test
    //TODO
    void formatVersionString_shouldFormatStringCorrectly() throws Exception {
        Assertions.assertTrue(Utils.formatVersionString(Utils.getVersion()).startsWith("Goobi viewer Connector"));
    }
}
