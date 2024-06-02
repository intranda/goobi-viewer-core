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
package io.goobi.viewer.managedbeans;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * <p>Abstract ContextMocker class.</p>
 *
 */
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

    /**
     * <p>mockFacesContext.</p>
     *
     * @return a {@link javax.faces.context.FacesContext} object
     */
    public static FacesContext mockFacesContext() {
        FacesContext context = Mockito.mock(FacesContext.class);

        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        Mockito.when(context.getExternalContext()).thenReturn(externalContext);

        setCurrentInstance(context);
        Mockito.doAnswer(RELEASE).when(context).release();
        return context;
    }

}
