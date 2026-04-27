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
package io.goobi.viewer.connector.oai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractTest;

class RequestHandlerTest extends AbstractTest {

    /**
     * @see RequestHandler#getFromTimestamp(String)
     * @verifies convert date to timestamp correctly
     */
    @Test
    void getFromTimestamp_shouldConvertDateToTimestampCorrectly() throws Exception {
        String from = "2015-01-01T00:00:00Z";
        Assertions.assertEquals(1420070400000L, RequestHandler.getFromTimestamp(from));
    }

    /**
     * @see RequestHandler#getFromTimestamp(String)
     * @verifies set time to 000000 if none given
     */
    @Test
    void getFromTimestamp_shouldSetTimeTo000000IfNoneGiven() throws Exception {
        String from = "2015-01-01";
        Assertions.assertEquals(1420070400000L, RequestHandler.getFromTimestamp(from));
    }

    /**
     * @see RequestHandler#getUntilTimestamp(String)
     * @verifies convert date to timestamp correctly
     */
    @Test
    void getUntilTimestamp_shouldConvertDateToTimestampCorrectly() throws Exception {
        String until = "2015-01-01T00:00:00Z";
        Assertions.assertEquals(1420070400999L, RequestHandler.getUntilTimestamp(until));
    }

    /**
     * @see RequestHandler#getUntilTimestamp(String)
     * @verifies set time to 235959 if none given
     */
    @Test
    void getUntilTimestamp_shouldSetTimeTo235959IfNoneGiven() throws Exception {
        String until = "2015-01-01";
        Assertions.assertEquals(1420156799999L, RequestHandler.getUntilTimestamp(until));
    }
}