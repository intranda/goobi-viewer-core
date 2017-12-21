/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
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
package de.intranda.digiverso.presentation;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.mockito.Mockito;

import de.intranda.digiverso.presentation.managedbeans.ContextMocker;

public class TestUtils {

    /**
     * Creates a Mockito-created FacesContext with an ExternalContext, ServletContext and session map. It can then be extended by tests to return
     * beans, etc.
     * 
     * @return Mock FacesContext
     */
    public static FacesContext mockFacesContext() {
        FacesContext facesContext = ContextMocker.mockFacesContext();

        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);

        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(externalContext.getContext()).thenReturn(servletContext);

        Map<String, Object> sessionMap = new HashMap<>();
        Mockito.when(externalContext.getSessionMap()).thenReturn(sessionMap);

        return facesContext;
    }

    public static HttpServletRequest mockHttpRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        HttpSession session = new MockSession();
        Mockito.when(request.getSession()).thenReturn(session);

        return request;

        //        Map<String, Object> sessionMap = new HashMap<>();
        //        Mockito.when(session.getAttribute(SessionStoreBookshelfManager.BOOKSHELF_ATTRIBUTE_NAME)).thenReturn(sessionMap.get(SessionStoreBookshelfManager.BOOKSHELF_ATTRIBUTE_NAME));
        //        Mockito.when(session.setAttribute(arg0, arg1);)
    }

    public static class MockSession implements HttpSession {

        Map<String, Object> attributes = new HashMap<>();

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
         */
        @Override
        public Object getAttribute(String key) {
            return attributes.get(key);
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
         */
        @Override
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);

        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
         */
        @Override
        public void removeAttribute(String key) {
            attributes.remove(key);

        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getAttributeNames()
         */
        @Override
        public Enumeration<String> getAttributeNames() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getCreationTime()
         */
        @Override
        public long getCreationTime() {
            return 0;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getId()
         */
        @Override
        public String getId() {
            return "TEST-SESSION-ID";
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getLastAccessedTime()
         */
        @Override
        public long getLastAccessedTime() {
            return 0;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
         */
        @Override
        public int getMaxInactiveInterval() {
            return 0;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getServletContext()
         */
        @Override
        public ServletContext getServletContext() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getSessionContext()
         */
        @Override
        public HttpSessionContext getSessionContext() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
         */
        @Override
        public Object getValue(String arg0) {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#getValueNames()
         */
        @Override
        public String[] getValueNames() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#invalidate()
         */
        @Override
        public void invalidate() {
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#isNew()
         */
        @Override
        public boolean isNew() {
            return false;
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
         */
        @Override
        public void putValue(String arg0, Object arg1) {
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
         */
        @Override
        public void removeValue(String arg0) {
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
         */
        @Override
        public void setMaxInactiveInterval(int arg0) {
        }

    }

}
