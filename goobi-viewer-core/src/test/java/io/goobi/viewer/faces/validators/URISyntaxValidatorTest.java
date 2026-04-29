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
package io.goobi.viewer.faces.validators;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.faces.component.UIComponent;
import jakarta.faces.validator.ValidatorException;

class URISyntaxValidatorTest {

    private URISyntaxValidator validator;
    private UIComponent component;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        validator = new URISyntaxValidator();
        component = mock(UIComponent.class);
        attributes = new HashMap<>();
        when(component.getAttributes()).thenReturn(attributes);
    }

    /**
     * @see URISyntaxValidator#validate(jakarta.faces.context.FacesContext, UIComponent, String)
     * @verifies skip validation if not active
     */
    @Test
    void validate_shouldSkipValidationIfNotActive() {
        // validator_active defaults to false — plain text must not trigger an exception
        Assertions.assertDoesNotThrow(
                () -> validator.validate(null, component, "Foto des Salons der Amerika Gedenkbibliothek"));
    }

    /**
     * @see URISyntaxValidator#validate(jakarta.faces.context.FacesContext, UIComponent, String)
     * @verifies accept absolute URI when active
     */
    @Test
    void validate_shouldAcceptAbsoluteURIWhenActive() {
        attributes.put("validator_active", true);
        Assertions.assertDoesNotThrow(
                () -> validator.validate(null, component, "https://example.com/viewer/search"));
    }

    /**
     * @see URISyntaxValidator#validate(jakarta.faces.context.FacesContext, UIComponent, String)
     * @verifies accept relative URI when active and absolute not required
     */
    @Test
    void validate_shouldAcceptRelativeURIWhenActiveAndAbsoluteNotRequired() {
        attributes.put("validator_active", true);
        Assertions.assertDoesNotThrow(
                () -> validator.validate(null, component, "/viewer/search?q=test"));
    }

    /**
     * @see URISyntaxValidator#validate(jakarta.faces.context.FacesContext, UIComponent, String)
     * @verifies reject free text when active
     */
    @Test
    void validate_shouldRejectFreeTextWhenActive() {
        // Reproduces the production bug: description text stored in the link_url column
        attributes.put("validator_active", true);
        Assertions.assertThrows(ValidatorException.class,
                () -> validator.validate(null, component,
                        "Foto des Salons der Amerika Gedenkbibliothek Berlin mit Besucher*innen"));
    }

    /**
     * @see URISyntaxValidator#validate(jakarta.faces.context.FacesContext, UIComponent, String)
     * @verifies reject relative URI when absolute required
     */
    @Test
    void validate_shouldRejectRelativeURIWhenAbsoluteRequired() {
        attributes.put("validator_active", true);
        attributes.put("validator_requireAbsoluteURI", true);
        Assertions.assertThrows(ValidatorException.class,
                () -> validator.validate(null, component, "/viewer/search"));
    }

    /**
     * @see URISyntaxValidator#validate(jakarta.faces.context.FacesContext, UIComponent, String)
     * @verifies accept absolute URI when absolute required
     */
    @Test
    void validate_shouldAcceptAbsoluteURIWhenAbsoluteRequired() {
        attributes.put("validator_active", true);
        attributes.put("validator_requireAbsoluteURI", true);
        Assertions.assertDoesNotThrow(
                () -> validator.validate(null, component, "https://example.com/viewer"));
    }
}
