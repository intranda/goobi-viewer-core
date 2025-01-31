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
package io.goobi.viewer.faces.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.faces.component.UIComponent;

class LocalDateTimeConverterTest {

    private static final LocalDateTime DATETIME = LocalDateTime.of(2025, 01, 31, 13, 0, 0);

    private static final String DATETIME_GERMAN = "31.01.2025 13:00:00";
    private static final String DATETIME_ENGLISH = "01/31/2025 1:00 PM";
    private static final String DATETIME_GENERIC = "2025-01-31T13:00:00";

    private static final String PATTERN_GERMAN = "dd.MM.yyyy HH:mm:ss";
    private static final String PATTERN_ENGLISH = "MM/dd/yyyy h:mm a";
    private static final String PATTERN_GENERIC = "yyyy-MM-dd'T'HH:mm:ss";

    @Test
    void getAsObject_shouldConvertGermanDateTimeCorrectly() {
        Map<String, Object> attributes = new HashMap<>(2);
        attributes.put("pattern", PATTERN_GERMAN);
        attributes.put("timeZone", "UTC");
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(attributes);

        assertEquals(DATETIME, new LocalDateTimeConverter().setLocale(Locale.GERMAN).getAsObject(null, component, DATETIME_GERMAN));
    }
    
    @Test
    void getAsString_shouldConvertGermanDateTimeCorrectly() {
        Map<String, Object> attributes = new HashMap<>(2);
        attributes.put("pattern", PATTERN_GERMAN);
        attributes.put("timeZone", "UTC");
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(attributes);

        assertEquals(DATETIME_GERMAN, new LocalDateTimeConverter().setLocale(Locale.GERMAN).getAsString(null, component, DATETIME));
    }

    @Test
    void getAsObject_shouldConvertEnglishDateTimeCorrectly() {
        Map<String, Object> attributes = new HashMap<>(2);
        attributes.put("pattern", PATTERN_ENGLISH);
        attributes.put("timeZone", "UTC");
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(attributes);

        assertEquals(DATETIME, new LocalDateTimeConverter().setLocale(Locale.ENGLISH).getAsObject(null, component, DATETIME_ENGLISH));
    }
    
    
    @Test
    void getAsString_shouldConvertEnglishDateTimeCorrectly() {
        Map<String, Object> attributes = new HashMap<>(2);
        attributes.put("pattern", PATTERN_ENGLISH);
        attributes.put("timeZone", "UTC");
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(attributes);

        assertEquals(DATETIME_ENGLISH, new LocalDateTimeConverter().setLocale(Locale.ENGLISH).getAsString(null, component, DATETIME));
    }

    @Test
    void getAsObject_shouldConvertGenericDateTimeCorrectly() {
        Map<String, Object> attributes = new HashMap<>(2);
        attributes.put("pattern", PATTERN_GENERIC);
        attributes.put("timeZone", "UTC");
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(attributes);
        assertEquals(DATETIME, new LocalDateTimeConverter().setLocale(Locale.ENGLISH).getAsObject(null, component, DATETIME_GENERIC));
    }
    
    @Test
    void getAsString_shouldConvertGenericDateTimeCorrectly() {
        Map<String, Object> attributes = new HashMap<>(2);
        attributes.put("pattern", PATTERN_GENERIC);
        attributes.put("timeZone", "UTC");
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(attributes);

        assertEquals(DATETIME_GENERIC, new LocalDateTimeConverter().setLocale(Locale.ENGLISH).getAsString(null, component, DATETIME));
    }
}
