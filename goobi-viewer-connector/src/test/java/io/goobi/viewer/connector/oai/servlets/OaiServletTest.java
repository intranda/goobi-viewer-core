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
package io.goobi.viewer.connector.oai.servlets;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractTest;

class OaiServletTest extends AbstractTest {
    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return false if from is not well formed
     */
    @Test
    void checkDatestamps_shouldReturnFalseIfFromIsNotWellFormed() {
        Assertions.assertFalse(OaiServlet.checkDatestamps("2015-09-30T15:00:00X", "2015-09-30T15:00:01"));
        Assertions.assertFalse(OaiServlet.checkDatestamps("2015-09-2X", "2015-09-30"));
    }

    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return false if until is not well formed
     */
    @Test
    void checkDatestamps_shouldReturnFalseIfUntilIsNotWellFormed() {
        Assertions.assertFalse(OaiServlet.checkDatestamps("2015-09-30T15:00:00", "2015-09-30T15:00:01Z"));
        Assertions.assertFalse(OaiServlet.checkDatestamps("2015-09-30", "2015-09-31"));
    }

    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return false if from after until
     */
    @Test
    void checkDatestamps_shouldReturnFalseIfFromAfterUntil() {
        Assertions.assertFalse(OaiServlet.checkDatestamps("2015-09-30T15:00:01", "2015-09-30T15:00:00"));
        Assertions.assertFalse(OaiServlet.checkDatestamps("2015-09-30", "2015-09-29"));
    }

    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return true if from and until correct
     */
    @Test
    void checkDatestamps_shouldReturnTrueIfFromAndUntilCorrect() {
        Assertions.assertTrue(OaiServlet.checkDatestamps("2015-09-30T15:00:00Z", "2015-09-30T15:00:01Z"));
        Assertions.assertTrue(OaiServlet.checkDatestamps("2015-09-29", "2015-09-30"));
    }

    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return false if from and until different types
     */
    @Test
    void checkDatestamps_shouldReturnFalseIfFromAndUntilDifferentTypes() {
        Assertions.assertFalse(OaiServlet.checkDatestamps("2015-09-30", "2015-09-30T15:00:00Z"));
        Assertions.assertFalse(OaiServlet.checkDatestamps("2015-09-29T15:00:00:Z", "2015-09-30"));
    }

    /**
     * @see OaiServlet#checkDatestamps(String,String)
     * @verifies return true if only one datestamp given
     */
    @Test
    void checkDatestamps_shouldReturnTrueIfOnlyOneDatestampGiven() {
        Assertions.assertTrue(OaiServlet.checkDatestamps("2015-09-30T15:00:00Z", null));
        Assertions.assertTrue(OaiServlet.checkDatestamps(null, "2015-09-30"));
    }

    /**
     * @see OaiServlet#isClientAbort(Throwable)
     * @verifies return true when exception class name contains ClientAbortException
     */
    @Test
    void isClientAbort_shouldReturnTrueWhenExceptionClassNameContainsClientAbortException() {
        // Use a locally defined subclass whose simple name matches the Tomcat exception so the
        // class-name based heuristic can be exercised without pulling Tomcat onto the test classpath.
        Assertions.assertTrue(OaiServlet.isClientAbort(new ClientAbortException("upstream gone")));
    }

    /**
     * @see OaiServlet#isClientAbort(Throwable)
     * @verifies return true when message contains broken pipe
     */
    @Test
    void isClientAbort_shouldReturnTrueWhenMessageContainsBrokenPipe() {
        Assertions.assertTrue(OaiServlet.isClientAbort(new IOException("Broken pipe")));
    }

    /**
     * @see OaiServlet#isClientAbort(Throwable)
     * @verifies return true when message contains connection reset
     */
    @Test
    void isClientAbort_shouldReturnTrueWhenMessageContainsConnectionReset() {
        Assertions.assertTrue(OaiServlet.isClientAbort(new IOException("Connection reset by peer")));
    }

    /**
     * @see OaiServlet#isClientAbort(Throwable)
     * @verifies return false for generic IOException
     */
    @Test
    void isClientAbort_shouldReturnFalseForGenericIOException() {
        Assertions.assertFalse(OaiServlet.isClientAbort(new IOException("Disk full")));
    }

    /**
     * @see OaiServlet#isClientAbort(Throwable)
     * @verifies be case insensitive
     */
    @Test
    void isClientAbort_shouldBeCaseInsensitive() {
        Assertions.assertTrue(OaiServlet.isClientAbort(new IOException("BROKEN PIPE")));
        Assertions.assertTrue(OaiServlet.isClientAbort(new IOException("Connection RESET")));
    }

    /**
     * @see OaiServlet#extractClientIp(String, String)
     * @verifies return remote address when forwarded for is null
     */
    @Test
    void extractClientIp_shouldReturnRemoteAddressWhenForwardedForIsNull() {
        Assertions.assertEquals("10.0.0.1", OaiServlet.extractClientIp(null, "10.0.0.1"));
    }

    /**
     * @see OaiServlet#extractClientIp(String, String)
     * @verifies return forwarded for when no comma present
     */
    @Test
    void extractClientIp_shouldReturnForwardedForWhenNoCommaPresent() {
        Assertions.assertEquals("203.0.113.5", OaiServlet.extractClientIp("203.0.113.5", "10.0.0.1"));
    }

    /**
     * @see OaiServlet#extractClientIp(String, String)
     * @verifies return first ip when forwarded for contains comma
     */
    @Test
    void extractClientIp_shouldReturnFirstIpWhenForwardedForContainsComma() {
        Assertions.assertEquals("203.0.113.5", OaiServlet.extractClientIp("203.0.113.5,198.51.100.7,10.0.0.1", "10.0.0.1"));
    }

    /**
     * @see OaiServlet#extractClientIp(String, String)
     * @verifies trim whitespace from first forwarded for entry
     */
    @Test
    void extractClientIp_shouldTrimWhitespaceFromFirstForwardedForEntry() {
        Assertions.assertEquals("203.0.113.5", OaiServlet.extractClientIp("  203.0.113.5  , 198.51.100.7", "10.0.0.1"));
    }

    /**
     * @see OaiServlet#isSolrUrlMismatch(String, String)
     * @verifies return false when either url is null
     */
    @Test
    void isSolrUrlMismatch_shouldReturnFalseWhenEitherUrlIsNull() {
        Assertions.assertFalse(OaiServlet.isSolrUrlMismatch(null, "http://localhost:8983/solr"));
        Assertions.assertFalse(OaiServlet.isSolrUrlMismatch("http://localhost:8983/solr", null));
        Assertions.assertFalse(OaiServlet.isSolrUrlMismatch(null, null));
    }

    /**
     * @see OaiServlet#isSolrUrlMismatch(String, String)
     * @verifies return false when urls are equal
     */
    @Test
    void isSolrUrlMismatch_shouldReturnFalseWhenUrlsAreEqual() {
        Assertions.assertFalse(OaiServlet.isSolrUrlMismatch("http://localhost:8983/solr", "http://localhost:8983/solr"));
    }

    /**
     * @see OaiServlet#isSolrUrlMismatch(String, String)
     * @verifies return false when urls differ only by trailing slash
     */
    @Test
    void isSolrUrlMismatch_shouldReturnFalseWhenUrlsDifferOnlyByTrailingSlash() {
        Assertions.assertFalse(OaiServlet.isSolrUrlMismatch("http://localhost:8983/solr/", "http://localhost:8983/solr"));
        Assertions.assertFalse(OaiServlet.isSolrUrlMismatch("http://localhost:8983/solr", "http://localhost:8983/solr/"));
    }

    /**
     * @see OaiServlet#isSolrUrlMismatch(String, String)
     * @verifies return true when urls differ
     */
    @Test
    void isSolrUrlMismatch_shouldReturnTrueWhenUrlsDiffer() {
        Assertions.assertTrue(OaiServlet.isSolrUrlMismatch("http://localhost:8983/solr", "http://other-host:8983/solr"));
    }

    /**
     * Local stand-in for org.apache.catalina.connector.ClientAbortException so the simple-class-name
     * heuristic in isClientAbort can be exercised without depending on Tomcat at test time.
     */
    private static class ClientAbortException extends IOException {
        private static final long serialVersionUID = 1L;

        ClientAbortException(String msg) {
            super(msg);
        }
    }
}
