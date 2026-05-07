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
package io.goobi.viewer.filters;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;

class SessionCounterFilterTest extends AbstractTest {

    /**
     * Reproduces the ConcurrentModificationException that occurs when JSF modifies the
     * LogicalViewMap while SessionCounterFilter iterates over its values.
     * @verifies not throw when logical view map throws concurrent modification exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void doFilter_shouldNotThrowWhenLogicalViewMapThrowsConcurrentModificationException() throws Exception {
        // A LogicalViewMap whose values() iterator throws CME, simulating concurrent JSF modification
        Map<Object, Map> cmeMap = new LinkedHashMap<>() {
            @Override
            public Collection<Map> values() {
                return new AbstractCollection<>() {
                    @Override
                    public Iterator<Map> iterator() {
                        return new Iterator<>() {
                            @Override
                            public boolean hasNext() {
                                return true;
                            }

                            @Override
                            public Map next() {
                                throw new ConcurrentModificationException("simulated concurrent JSF modification");
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return 1;
                    }
                };
            }
        };
        cmeMap.put("logicalView1", new LinkedHashMap<>());

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getId()).thenReturn("test-session-id");
        Mockito.when(session.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(session.getMaxInactiveInterval()).thenReturn(1800);
        Mockito.when(session.getAttribute("com.sun.faces.renderkit.ServerSideStateHelper.LogicalViewMap"))
                .thenReturn(cmeMap);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getSession()).thenReturn(session);
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        SessionCounterFilter filter = new SessionCounterFilter();
        Assertions.assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }
}
