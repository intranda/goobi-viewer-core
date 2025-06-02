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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import jakarta.servlet.http.HttpServletRequest;

class NetToolsTest extends AbstractTest {

    /**
     * @see NetTools#parseMultipleIpAddresses(String)
     * @verifies filter multiple addresses correctly
     */
    @Test
    void parseMultipleIpAddresses_shouldFilterMultipleAddressesCorrectly() throws Exception {
        Assertions.assertEquals("1.1.1.1", NetTools.parseMultipleIpAddresses("1.1.1.1, 2.2.2.2, 3.3.3.3"));
    }

    /**
     * @see NetTools#scrambleEmailAddress(String)
     * @verifies modify string correctly
     */
    @Test
    void scrambleEmailAddress_shouldModifyStringCorrectly() throws Exception {
        Assertions.assertEquals("foo***com", NetTools.scrambleEmailAddress("foo@bar.com"));
        Assertions.assertEquals("foo***com", NetTools.scrambleEmailAddress("foofoofoo@barbarbar.com"));
    }

    /**
     * @see NetTools#scrambleIpAddress(String)
     * @verifies modify string correctly
     */
    @Test
    void scrambleIpAddress_shouldModifyStringCorrectly() throws Exception {
        Assertions.assertEquals("192.168.X.X", NetTools.scrambleIpAddress("192.168.0.1"));
    }

    /**
     * @see NetTools#buildClearCacheUrl(String,String,String)
     * @verifies build url correctly
     */
    @Test
    void buildClearCacheUrl_shouldBuildUrlCorrectly() throws Exception {
        Assertions.assertEquals("https://example.com/api/v1/cache/PPN123/?token=test&content=true&thumbs=true&pdf=true",
                NetTools.buildClearCacheUrl(NetTools.PARAM_CLEAR_CACHE_ALL, "PPN123", "https://example.com/", "test"));
        Assertions.assertEquals("https://example.com/api/v1/cache/PPN123/?token=test&content=true",
                NetTools.buildClearCacheUrl(NetTools.PARAM_CLEAR_CACHE_CONTENT, "PPN123", "https://example.com/", "test"));
        Assertions.assertEquals("https://example.com/api/v1/cache/PPN123/?token=test&thumbs=true",
                NetTools.buildClearCacheUrl(NetTools.PARAM_CLEAR_CACHE_THUMBS, "PPN123", "https://example.com/", "test"));
        Assertions.assertEquals("https://example.com/api/v1/cache/PPN123/?token=test&pdf=true",
                NetTools.buildClearCacheUrl(NetTools.PARAM_CLEAR_CACHE_PDF, "PPN123", "https://example.com/", "test"));
    }

    @Test
    void test_parseIpAddress() {
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
}
