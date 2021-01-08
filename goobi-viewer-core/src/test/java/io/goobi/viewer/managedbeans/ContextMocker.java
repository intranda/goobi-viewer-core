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
package io.goobi.viewer.managedbeans;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

import javax.faces.application.Application;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sun.faces.context.ExternalContextFactoryImpl;

public abstract class ContextMocker extends FacesContext {

    private ContextMocker() {
    }

    private static final Release RELEASE = new Release();

    private static class Release implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            setCurrentInstance(null);
            return null;
        }
    }

    public static FacesContext mockFacesContext() {
        return mockFacesContext(Locale.GERMAN, Locale.ENGLISH);
    }
    
    private static FacesContext mockFacesContext(Locale ...locales) {
        FacesContext facesContext = Mockito.mock(FacesContext.class);

        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        
        setCurrentInstance(facesContext);
        Mockito.doAnswer(RELEASE).when(facesContext).release();
        
        Application application = Mockito.mock(Application.class);
        Mockito.when(facesContext.getApplication()).thenReturn(application);
        Mockito.when(application.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Iterator<Locale> supportedLanguages = Arrays.asList(locales).iterator();
        Mockito.when(application.getSupportedLocales()).thenReturn(supportedLanguages);
        
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);
        Mockito.when(facesContext.getViewRoot()).thenReturn(viewRoot);
        Mockito.when(viewRoot.getLocale()).thenReturn(Locale.ENGLISH);
        
        return facesContext;
    }
}
