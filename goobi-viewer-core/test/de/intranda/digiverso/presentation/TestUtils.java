/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

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

}
