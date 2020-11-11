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
package io.goobi.viewer.dao.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.model.misc.TranslatedText;

/**
 * @author florian
 *
 */
public class TranslatedTextConverterTest {

    private static final String ENGLISHVALUE = "This is the <span font='bold'>value</span> for the \"English\" language";
    private static final String GERMANVALUE = "Deutsch";
    private static final String JSON = "{\"de\":[\""+ StringEscapeUtils.escapeHtml4(GERMANVALUE) + "\"],\"en\":[\"" + StringEscapeUtils.escapeHtml4(ENGLISHVALUE) + "\"]}";
    private static final String EXPECTED_JSON = "{\"de\":[\"Deutsch\"],\"en\":[\"This is the <span font='bold'>value</span> for the \\\"English\\\" language\"]}";

    private final List<Locale> locales = Arrays.asList(Locale.GERMAN, Locale.ENGLISH, Locale.FRANCE, Locale.CHINESE);
    private final TranslatedTextConverter converter = new TranslatedTextConverter(locales);
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCorrectlyConvertToString() {
        TranslatedText value = new TranslatedText(locales, Locale.GERMAN);
        value.setText(GERMANVALUE, Locale.GERMAN);
        value.setText(ENGLISHVALUE, Locale.ENGLISH);
        
        String json = converter.convertToDatabaseColumn(value);
        Assert.assertEquals(EXPECTED_JSON, json);
        
    }
    
    @Test
    public void testCorrectlConvertFromString() {
        
        TranslatedText value = converter.convertToEntityAttribute(JSON);
        
        Assert.assertEquals(locales.size(), value.getValues().size());
        Assert.assertEquals(GERMANVALUE, value.getText(Locale.GERMAN));
        Assert.assertEquals(ENGLISHVALUE, value.getText(Locale.ENGLISH));
        Assert.assertEquals("", value.getText(Locale.CHINESE));

        
    }
    
    @Test
    public void testConvertSingleValueCurrectly() {
        TranslatedText value = new TranslatedText(locales, Locale.GERMAN);
        value.setText(GERMANVALUE, Locale.GERMAN);

        String json = converter.convertToDatabaseColumn(value);
        TranslatedText restoredValue = converter.convertToEntityAttribute(json);
        
        Assert.assertEquals(GERMANVALUE, restoredValue.getValue(Locale.GERMAN).orElse(null));
        Assert.assertNull(restoredValue.getValue(Locale.ENGLISH).orElse(null));


    }

}
