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
package io.goobi.viewer.managedbeans.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpSession;

class BeanUtilsTest {

    /** Simulates a Weld CDI proxy: a subclass of the actual bean class. */
    static class ParentBean {
    }

    static class ProxyBean extends ParentBean {
    }

    /**
     * Fix 3: findInstanceInSessionAttributes must find an instance stored as a subclass
     * (as Weld CDI proxies are subclasses of the bean type).
     * <p>
     * With .getClass().equals(clazz) this FAILS because ProxyBean.class != ParentBean.class.
     * With clazz.isInstance(value) it correctly detects the subclass.
     */
    @Test
    void findInstanceInSessionAttributes_returnsSubclassStoredUnderInternalWeldKey() {
        ProxyBean weldProxy = new ProxyBean();

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttributeNames()).thenReturn(Collections.enumeration(List.of("WELD_S_abc123")));
        Mockito.when(session.getAttribute("WELD_S_abc123")).thenReturn(weldProxy);

        Optional<ParentBean> result = BeanUtils.findInstanceInSessionAttributes(session, ParentBean.class);

        assertTrue(result.isPresent(), "CDI proxy (subclass) must be found via isInstance check");
    }

    /**
     * Fix 2: getBeanFromSession must find an instance when the direct attribute lookup
     * returns null (Weld does not store under the @Named name) but the instance exists
     * as a subclass under an internal key.
     */
    @Test
    void getBeanFromSession_findsSubclassWhenDirectLookupReturnsNull() {
        ProxyBean weldProxy = new ProxyBean();

        HttpSession session = Mockito.mock(HttpSession.class);
        // Weld does NOT store under the bean name
        Mockito.when(session.getAttribute("parentBean")).thenReturn(null);
        Mockito.when(session.getAttributeNames()).thenReturn(Collections.enumeration(List.of("WELD_S_abc123")));
        Mockito.when(session.getAttribute("WELD_S_abc123")).thenReturn(weldProxy);

        Optional<ParentBean> result = BeanUtils.getBeanFromSession(session, "parentBean", ParentBean.class);

        assertTrue(result.isPresent(), "getBeanFromSession must find CDI proxy (subclass) stored under internal key");
    }

    /**
     * Sanity check: an exact class match must still work after the fix.
     */
    @Test
    void findInstanceInSessionAttributes_returnsExactClassMatch() {
        ParentBean bean = new ParentBean();

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttributeNames()).thenReturn(Collections.enumeration(List.of("someKey")));
        Mockito.when(session.getAttribute("someKey")).thenReturn(bean);

        Optional<ParentBean> result = BeanUtils.findInstanceInSessionAttributes(session, ParentBean.class);

        assertTrue(result.isPresent(), "Exact class match must still be found");
    }

    /**
     * Sanity check: a completely unrelated class must NOT be returned.
     */
    @Test
    void findInstanceInSessionAttributes_doesNotReturnUnrelatedClass() {
        Object unrelated = "just a string";

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttributeNames()).thenReturn(Collections.enumeration(List.of("someKey")));
        Mockito.when(session.getAttribute("someKey")).thenReturn(unrelated);

        Optional<ParentBean> result = BeanUtils.findInstanceInSessionAttributes(session, ParentBean.class);

        assertFalse(result.isPresent(), "Unrelated class must not be returned");
    }
}
