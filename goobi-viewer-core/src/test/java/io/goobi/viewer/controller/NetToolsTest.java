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
package io.goobi.viewer.controller;

import java.net.InetAddress;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import jakarta.servlet.http.HttpServletRequest;

class NetToolsTest extends AbstractTest {

    /**
     * @see NetTools#parseMultipleIpAddresses(String)
     * @verifies return only the first IP address from a comma-separated list
     */
    @Test
    void parseMultipleIpAddresses_shouldReturnOnlyTheFirstIPAddressFromACommaSeparatedList() throws Exception {
        Assertions.assertEquals("1.1.1.1", NetTools.parseMultipleIpAddresses("1.1.1.1, 2.2.2.2, 3.3.3.3"));
    }

    /**
     * @see NetTools#scrambleEmailAddress(String)
     * @verifies replace domain middle part with asterisks keeping first three and last three characters
     */
    @Test
    void scrambleEmailAddress_shouldReplaceDomainMiddlePartWithAsterisksKeepingFirstThreeAndLastThreeCharacters() throws Exception {
        Assertions.assertEquals("foo***com", NetTools.scrambleEmailAddress("foo@bar.com"));
        Assertions.assertEquals("foo***com", NetTools.scrambleEmailAddress("foofoofoo@barbarbar.com"));
    }

    /**
     * @see NetTools#scrambleIpAddress(String)
     * @verifies replace last two octets of IP address with X
     */
    @Test
    void scrambleIpAddress_shouldReplaceLastTwoOctetsOfIPAddressWithX() throws Exception {
        Assertions.assertEquals("192.168.X.X", NetTools.scrambleIpAddress("192.168.0.1"));
    }

    /**
     * @see NetTools#buildClearCacheUrl(String,String,String)
     * @verifies compose cache API URL with correct query params for each cache type
     */
    @Test
    void buildClearCacheUrl_shouldComposeCacheAPIURLWithCorrectQueryParamsForEachCacheType() throws Exception {
        Assertions.assertEquals("https://example.com/api/v1/cache/PPN123/?content=true&thumbs=true&pdf=true",
                NetTools.buildClearCacheUrl(NetTools.PARAM_CLEAR_CACHE_ALL, "PPN123", "https://example.com/"));
        Assertions.assertEquals("https://example.com/api/v1/cache/PPN123/?content=true",
                NetTools.buildClearCacheUrl(NetTools.PARAM_CLEAR_CACHE_CONTENT, "PPN123", "https://example.com/"));
        Assertions.assertEquals("https://example.com/api/v1/cache/PPN123/?thumbs=true",
                NetTools.buildClearCacheUrl(NetTools.PARAM_CLEAR_CACHE_THUMBS, "PPN123", "https://example.com/"));
        Assertions.assertEquals("https://example.com/api/v1/cache/PPN123/?pdf=true",
                NetTools.buildClearCacheUrl(NetTools.PARAM_CLEAR_CACHE_PDF, "PPN123", "https://example.com/"));
    }

    /**
     * @see NetTools#isRedirectUrlAllowed(String, String)
     * @verifies return true if redirectUrl starts with application url
     */
    @Test
    void isRedirectUrlAllowed_shouldReturnTrueIfRedirectUrlStartsWithApplicationUrl() {
        Assertions.assertTrue(NetTools.isRedirectUrlAllowed("https://viewer.example.org/page/1", "https://viewer.example.org/"));
    }

    /**
     * @see NetTools#isRedirectUrlAllowed(String, String)
     * @verifies return true if redirectUrl host is whitelisted
     */
    @Test
    void isRedirectUrlAllowed_shouldReturnTrueIfRedirectUrlHostIsWhitelisted() {
        Assertions.assertTrue(NetTools.isRedirectUrlAllowed("https://trusted.example.org/callback", null));
        Assertions.assertTrue(NetTools.isRedirectUrlAllowed("https://sso.example.com/return?session=abc", null));
    }

    /**
     * @see NetTools#isRedirectUrlAllowed(String, String)
     * @verifies return false if redirectUrl host is not whitelisted
     */
    @Test
    void isRedirectUrlAllowed_shouldReturnFalseIfRedirectUrlHostIsNotWhitelisted() {
        Assertions.assertFalse(NetTools.isRedirectUrlAllowed("https://evil.example.com/phishing", null));
        Assertions.assertFalse(NetTools.isRedirectUrlAllowed("https://attacker.interact.sh", null));
    }

    /**
     * @see NetTools#isRedirectUrlAllowed(String, String)
     * @verifies return false if redirectUrl is malformed
     */
    @Test
    void isRedirectUrlAllowed_shouldReturnFalseIfRedirectUrlIsMalformed() {
        Assertions.assertFalse(NetTools.isRedirectUrlAllowed("not-a-url", null));
        Assertions.assertFalse(NetTools.isRedirectUrlAllowed("javascript:alert(1)", null));
    }

    /**
     * @see NetTools#isRedirectUrlAllowed(String, String)
     * @verifies return false if redirectUrl is null
     */
    @Test
    void isRedirectUrlAllowed_shouldReturnFalseIfRedirectUrlIsNull() {
        Assertions.assertFalse(NetTools.isRedirectUrlAllowed(null, "https://viewer.example.org/"));
    }

    /**
     * @verifies parse ip address
     */
    @Test
    void getIpAddress_shouldParseIpAddress() {
        {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("143.34.255.931, 127.0.0.1");
            Assertions.assertEquals("143.34.255.931", NetTools.getIpAddress(request));
        }

        {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("2001:db8:85a3:8d3:1319:8a2e:370:7348");
            Assertions.assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348", NetTools.getIpAddress(request));
        }

        {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 2001:db8:85a3:8d3:1319:8a2e:370:7348");
            Assertions.assertEquals("203.0.113.195", NetTools.getIpAddress(request));
        }

        {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("2001:db8:85a3:8d3:1319:8a2e:370:7348, 203.0.113.195");
            Assertions.assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348", NetTools.getIpAddress(request));
        }

        {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("143.34.255.931");
            Assertions.assertEquals("143.34.255.931", NetTools.getIpAddress(request));
        }

        {
            HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            Assertions.assertEquals("127.0.0.1", NetTools.getIpAddress(request));
        }
    }

    // --- SSRF validation tests ---

    /**
     * @see NetTools#validateOutboundUrl(String)
     * @verifies reject null url
     */
    @Test
    void validateOutboundUrl_shouldRejectNullUrl() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> NetTools.validateOutboundUrl(null));
    }

    /**
     * @see NetTools#validateOutboundUrl(String)
     * @verifies reject blank url
     */
    @Test
    void validateOutboundUrl_shouldRejectBlankUrl() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> NetTools.validateOutboundUrl("  "));
    }

    /**
     * @see NetTools#validateOutboundUrl(String)
     * @verifies reject non-http scheme
     */
    @Test
    void validateOutboundUrl_shouldRejectNonHttpScheme() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> NetTools.validateOutboundUrl("file:///etc/passwd"));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> NetTools.validateOutboundUrl(
                        "ftp://example.com/file"));
    }

    /**
     * @see NetTools#validateOutboundUrl(String)
     * @verifies reject url without host
     */
    @Test
    void validateOutboundUrl_shouldRejectUrlWithoutHost() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> NetTools.validateOutboundUrl("http://"));
    }

    /**
     * @see NetTools#validateOutboundUrl(String)
     * @verifies reject private network address
     */
    @Test
    void validateOutboundUrl_shouldRejectPrivateNetworkAddress() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> NetTools.validateOutboundUrl(
                        "http://10.0.0.1/admin"));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> NetTools.validateOutboundUrl(
                        "http://192.168.1.1/admin"));
    }

    /**
     * @see NetTools#validateOutboundUrl(String)
     * @verifies reject link-local address
     */
    @Test
    void validateOutboundUrl_shouldRejectLinkLocalAddress() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> NetTools.validateOutboundUrl(
                        "http://169.254.169.254/latest/"
                                + "meta-data/"));
    }

    /**
     * @see NetTools#validateOutboundUrl(String)
     * @verifies allow implicitly whitelisted host
     */
    @Test
    void validateOutboundUrl_shouldAllowImplicitlyWhitelistedHost() {
        Assertions.assertDoesNotThrow(
                () -> NetTools.validateOutboundUrl(
                        "http://localhost:8089/solr/select"));
    }

    /**
     * @see NetTools#validateOutboundUrl(String)
     * @verifies allow public address
     */
    @Test
    void validateOutboundUrl_shouldAllowPublicAddress() {
        Assertions.assertDoesNotThrow(
                () -> NetTools.validateOutboundUrl(
                        "https://example.com/api"));
    }

    /**
     * @see NetTools#isBlockedAddress(InetAddress)
     * @verifies block loopback addresses
     */
    @Test
    void isBlockedAddress_shouldBlockLoopbackAddresses()
            throws Exception {
        Assertions.assertTrue(NetTools.isBlockedAddress(
                InetAddress.getByName("127.0.0.1")));
        Assertions.assertTrue(NetTools.isBlockedAddress(
                InetAddress.getByName("::1")));
    }

    /**
     * @see NetTools#isBlockedAddress(InetAddress)
     * @verifies block site-local addresses
     */
    @Test
    void isBlockedAddress_shouldBlockSiteLocalAddresses()
            throws Exception {
        Assertions.assertTrue(NetTools.isBlockedAddress(
                InetAddress.getByName("10.0.0.1")));
        Assertions.assertTrue(NetTools.isBlockedAddress(
                InetAddress.getByName("172.16.0.1")));
        Assertions.assertTrue(NetTools.isBlockedAddress(
                InetAddress.getByName("192.168.1.1")));
    }

    /**
     * @see NetTools#isBlockedAddress(InetAddress)
     * @verifies block link-local addresses
     */
    @Test
    void isBlockedAddress_shouldBlockLinkLocalAddresses()
            throws Exception {
        Assertions.assertTrue(NetTools.isBlockedAddress(
                InetAddress.getByName("169.254.169.254")));
    }

    /**
     * @see NetTools#isBlockedAddress(InetAddress)
     * @verifies allow public addresses
     */
    @Test
    void isBlockedAddress_shouldAllowPublicAddresses()
            throws Exception {
        Assertions.assertFalse(NetTools.isBlockedAddress(
                InetAddress.getByName("8.8.8.8")));
        Assertions.assertFalse(NetTools.isBlockedAddress(
                InetAddress.getByName("93.184.216.34")));
    }

    /**
     * @see NetTools#buildImplicitAllowlist()
     * @verifies contain configured hosts
     */
    @Test
    void buildImplicitAllowlist_shouldContainConfiguredHosts() {
        Assertions.assertTrue(
                NetTools.buildImplicitAllowlist()
                        .contains("localhost"));
    }
}
