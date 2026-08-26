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
package io.goobi.viewer.model.cms.pages.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.jsf.JsfComponent;
import jakarta.faces.application.Application;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.html.HtmlPanelGroup;
import jakarta.faces.context.FacesContext;

class CMSComponentTest {

    /** Name of the thread that is held inside {@code Application.createComponent()} by the mocked context. */
    private static final String BLOCKING_THREAD_NAME = "blocking-builder";

    /**
     * Builds a bare component with the given template filename and no content items, avoiding any dependency
     * on the viewer configuration. The persistent component controls id and order, mirroring how a page's
     * components are set up in {@link io.goobi.viewer.model.cms.pages.CMSPage#initialiseCMSComponents}.
     */
    private static CMSComponent persistedComponent(String templateFilename, Long persistentId, int order) {
        CMSComponent template = new CMSComponent(new JsfComponent("cms/components/frontend/component", "slider"), "label", "desc",
                Collections.emptyList(), templateFilename, CMSComponentScope.PAGEHEADER, Collections.emptyMap(), Collections.emptyList(), null);
        PersistentCMSComponent persistent = new PersistentCMSComponent(template);
        persistent.setId(persistentId);
        CMSComponent component = new CMSComponent(template, Optional.of(persistent));
        // sortComponents() assigns the order via setOrder() in production; replicate that here so this.order
        // matches the rendered value.
        component.setOrder(order);
        return component;
    }

    /**
     * @see CMSComponent#getUniqueComponentIdSuffix()
     * @verifies append persistent component id when persisted
     */
    @Test
    void getUniqueComponentIdSuffix_shouldAppendPersistentComponentIdWhenPersisted() {
        CMSComponent component = persistedComponent("headerslider", 10L, 2);
        assertEquals("10_2", component.getUniqueComponentIdSuffix());
    }

    /**
     * @see CMSComponent#getUniqueComponentIdSuffix()
     * @verifies return order only when not persisted
     */
    @Test
    void getUniqueComponentIdSuffix_shouldReturnOrderOnlyWhenNotPersisted() {
        CMSComponent template = new CMSComponent(new JsfComponent("cms/components/frontend/component", "slider"), "label", "desc",
                Collections.emptyList(), "headerslider", CMSComponentScope.PAGEHEADER, Collections.emptyMap(), Collections.emptyList(), null);
        template.setOrder(5);
        assertEquals("5", template.getUniqueComponentIdSuffix());
    }

    /**
     * @see CMSComponent#getUniqueComponentIdSuffix()
     * @verifies produce distinct suffixes for components sharing an order
     */
    @Test
    void getUniqueComponentIdSuffix_shouldProduceDistinctSuffixesForComponentsSharingAnOrder() {
        // Reproduces the production bug: two components of the same template on the same page both had
        // order 2, which previously produced identical client ids (e.g. "cms_headerslider_2") and an
        // IllegalStateException in JSF's checkIdUniqueness. Distinct persistent ids must disambiguate them.
        CMSComponent component1 = persistedComponent("headerslider", 10L, 2);
        CMSComponent component2 = persistedComponent("headerslider", 11L, 2);
        assertNotEquals(component1.getUniqueComponentIdSuffix(), component2.getUniqueComponentIdSuffix());
    }

    /**
     * @see CMSComponent#getUiComponent()
     * @verifies return the same instance to concurrent callers
     */
    @Test
    void getUiComponent_shouldReturnTheSameInstanceToConcurrentCallers() throws Exception {
        // Reproduces the production bug: two requests of the same HTTP session render the same CMS page and
        // therefore call getUiComponent() on the same instance. The interleaving is forced by blocking the
        // first thread inside Application.createComponent(), so both threads pass the "not yet built" check.
        // Previously each thread stored its own panel group in the field and then used the field as the build
        // target, so the threads saw different components and both appended their composite child to whichever
        // panel group had been written last - resulting in two children with an identical JSF id.
        CMSComponent component = persistedComponent("headerslider", 176L, 2);

        CountDownLatch firstThreadIsBuilding = new CountDownLatch(1);
        CountDownLatch secondThreadFinished = new CountDownLatch(1);
        FacesContext context = mockFacesContext(firstThreadIsBuilding, secondThreadFinished);

        AtomicReference<UIComponent> firstResult = new AtomicReference<>();
        AtomicReference<UIComponent> secondResult = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        Thread first = new Thread(() -> firstResult.set(callGetUiComponent(component, context, error)), BLOCKING_THREAD_NAME);
        first.start();
        assertTrue(firstThreadIsBuilding.await(10, TimeUnit.SECONDS), "First thread did not reach the build step");

        Thread second = new Thread(() -> secondResult.set(callGetUiComponent(component, context, error)), "second-builder");
        second.start();
        second.join(10_000);
        secondThreadFinished.countDown();
        first.join(10_000);

        assertNull(error.get(), () -> "Building the component failed: " + error.get());
        assertNotNull(secondResult.get());
        assertSame(secondResult.get(), firstResult.get());
        assertSame(secondResult.get(), component.getUiComponent());
    }

    /**
     * Calls {@link CMSComponent#getUiComponent()} with the given context bound to the calling thread, recording
     * a failure instead of throwing, because a {@link Runnable} cannot propagate checked exceptions.
     */
    private static UIComponent callGetUiComponent(CMSComponent component, FacesContext context, AtomicReference<Throwable> error) {
        CurrentFacesContext.bind(context);
        try {
            return component.getUiComponent();
        } catch (PresentationException e) {
            error.compareAndSet(null, e);
            return null;
        } finally {
            CurrentFacesContext.bind(null);
        }
    }

    /**
     * Builds a Faces context that hands out a fresh {@link HtmlPanelGroup} per call and resolves no composite
     * component resource, so that {@link CMSComponent#getUiComponent()} runs without a Facelet environment. The
     * thread named {@link #BLOCKING_THREAD_NAME} is parked inside {@code createComponent()} until
     * {@code secondThreadFinished} is counted down, which forces the interleaving described in the test.
     */
    private static FacesContext mockFacesContext(CountDownLatch firstThreadIsBuilding, CountDownLatch secondThreadFinished) {
        FacesContext context = Mockito.mock(FacesContext.class);
        Application application = Mockito.mock(Application.class);
        ResourceHandler resourceHandler = Mockito.mock(ResourceHandler.class);
        Mockito.when(context.getApplication()).thenReturn(application);
        Mockito.when(context.getAttributes()).thenReturn(new HashMap<>());
        Mockito.when(application.getResourceHandler()).thenReturn(resourceHandler);
        Mockito.when(resourceHandler.createResource(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
        Mockito.when(application.createComponent(Mockito.anyString())).thenAnswer(invocation -> {
            if (BLOCKING_THREAD_NAME.equals(Thread.currentThread().getName())) {
                firstThreadIsBuilding.countDown();
                secondThreadFinished.await(10, TimeUnit.SECONDS);
            }
            return new HtmlPanelGroup();
        });
        return context;
    }

    /**
     * Gives the test access to {@link FacesContext#setCurrentInstance(FacesContext)}, which is protected and can
     * only be called from a subclass. Each test thread needs its own binding because the current instance is
     * held in a thread local.
     */
    private abstract static class CurrentFacesContext extends FacesContext {

        static void bind(FacesContext context) {
            setCurrentInstance(context);
        }
    }
}
