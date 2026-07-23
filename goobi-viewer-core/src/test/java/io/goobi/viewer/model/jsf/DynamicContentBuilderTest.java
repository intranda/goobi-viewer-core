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
package io.goobi.viewer.model.jsf;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;

import jakarta.faces.application.Application;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.managedbeans.ContextMocker;

class DynamicContentBuilderTest {

    @AfterEach
    void tearDown() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            fc.release();
        }
    }

    /**
     * Mojarra's development-mode component-nesting validator (ValidateComponentNesting) visits every
     * dynamically added component and calls getFamily().endsWith(...) on it. A null family throws a
     * NullPointerException there. createTag() is used by buildHead() to add a &lt;link&gt; tag directly
     * to the component tree (bypassing Facelets parsing), so its result must never have a null family.
     * @verifies not return a component with a null family
     */
    @Test
    void createTag_shouldNotReturnComponentWithNullFamily() {
        FacesContext mockFc = ContextMocker.mockFacesContext();
        Application mockApp = Mockito.mock(Application.class);
        Mockito.when(mockFc.getApplication()).thenReturn(mockApp);
        Mockito.when(mockFc.getAttributes()).thenReturn(new HashMap<>());

        DynamicContentBuilder builder = new DynamicContentBuilder();
        UIComponent tag = builder.createTag("link", Collections.emptyMap());

        assertNotNull(tag.getFamily());
    }
}
