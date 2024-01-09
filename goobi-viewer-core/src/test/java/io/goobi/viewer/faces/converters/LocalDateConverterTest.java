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

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collections;

import javax.faces.component.UIComponent;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author florian
 *
 */
public class LocalDateConverterTest {

    private static final LocalDate DATE = LocalDate.of(2021, 12, 24);

    private static final String DATE_GERMAN = "24.12.2021";
    private static final String DATE_ENGLISH = "12/24/2021";
    private static final String DATE_GENERIC = "2021-12-24";

    private static final String PATTERN_GERMAN = "dd.MM.yyyy";
    private static final String PATTERN_ENGLISH = "MM/dd/yyyy";
    private static final String PATTERN_GENERIC = "yyyy-MM-dd";


    @Test
    public void testGerman() {
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(Collections.singletonMap("data-format", PATTERN_GERMAN));
        LocalDate date = new LocalDateConverter().getAsObject(null, component, DATE_GERMAN);
        assertEquals(DATE, date);
    }

    @Test
    public void testEnglish() {
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(Collections.singletonMap("data-format", PATTERN_ENGLISH));
        LocalDate date = new LocalDateConverter().getAsObject(null, component, DATE_ENGLISH);
        assertEquals(DATE, date);
    }

    @Test
    public void testGeneric() {
        UIComponent component = Mockito.mock(UIComponent.class);
        Mockito.when(component.getAttributes()).thenReturn(Collections.singletonMap("data-format", PATTERN_GENERIC));
        LocalDate date = new LocalDateConverter().getAsObject(null, component, DATE_GENERIC);
        assertEquals(DATE, date);
    }

}
