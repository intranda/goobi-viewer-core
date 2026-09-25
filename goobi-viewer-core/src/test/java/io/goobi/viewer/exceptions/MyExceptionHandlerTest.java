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
package io.goobi.viewer.exceptions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.faces.application.Application;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import jakarta.faces.event.PhaseId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.managedbeans.ContextMocker;

class MyExceptionHandlerTest {

    @AfterEach
    void tearDown() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            fc.release();
        }
    }

    /**
     * When the response is already committed, MyExceptionHandler must not access Flash at all.
     * Accessing flash.put() on a committed response causes Mojarra to attempt setting a Set-Cookie
     * header, triggering JSF1095 warnings.
     */
    @Test
    void handle_responseCommitted_doesNotUseFlash() throws Exception {
        FacesContext mockFc = ContextMocker.mockFacesContext();
        ExternalContext mockEc = mockFc.getExternalContext();

        when(mockFc.getCurrentPhaseId()).thenReturn(PhaseId.INVOKE_APPLICATION);
        when(mockEc.isResponseCommitted()).thenReturn(true);
        when(mockEc.getRequestMap()).thenReturn(new HashMap<>());

        MyExceptionHandler handler = new MyExceptionHandler(buildWrappedHandlerWith(mockFc, new RuntimeException("test error")));
        handler.handle();

        verify(mockEc, never()).getFlash();
        verify(mockFc).responseComplete();
    }

    /**
     * When the response is not yet committed, Flash must be used and a redirect must be triggered.
     */
    @Test
    void handle_responseNotCommitted_usesFlash() throws Exception {
        FacesContext mockFc = ContextMocker.mockFacesContext();
        ExternalContext mockEc = mockFc.getExternalContext();
        Flash mockFlash = mock(Flash.class);
        Application mockApp = mock(Application.class);
        NavigationHandler mockNav = mock(NavigationHandler.class);

        when(mockFc.getCurrentPhaseId()).thenReturn(PhaseId.INVOKE_APPLICATION);
        when(mockEc.isResponseCommitted()).thenReturn(false);
        when(mockEc.getRequestMap()).thenReturn(new HashMap<>());
        when(mockEc.getFlash()).thenReturn(mockFlash);
        when(mockFc.getApplication()).thenReturn(mockApp);
        when(mockApp.getNavigationHandler()).thenReturn(mockNav);

        MyExceptionHandler handler = new MyExceptionHandler(buildWrappedHandlerWith(mockFc, new RuntimeException("test error")));
        handler.handle();

        verify(mockEc).getFlash();
        verify(mockFlash).setKeepMessages(true);
        verify(mockFlash).put(eq("errorType"), any());
    }

    private static ExceptionHandler buildWrappedHandlerWith(FacesContext facesContext, Throwable throwable) {
        ExceptionQueuedEventContext eventContext = new ExceptionQueuedEventContext(facesContext, throwable);
        ExceptionQueuedEvent event = new ExceptionQueuedEvent(facesContext, eventContext);
        List<ExceptionQueuedEvent> queue = new ArrayList<>(List.of(event));

        ExceptionHandler wrappedHandler = mock(ExceptionHandler.class);
        when(wrappedHandler.getUnhandledExceptionQueuedEvents()).thenReturn(queue);
        return wrappedHandler;
    }
}
