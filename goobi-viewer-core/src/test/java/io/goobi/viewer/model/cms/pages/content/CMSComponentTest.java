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

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.jsf.JsfComponent;

class CMSComponentTest {

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
}
