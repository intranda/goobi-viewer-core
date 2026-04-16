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
package io.goobi.viewer.model.translations.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.translations.admin.MessageEntry.TranslationStatus;

class MessageEntryTest extends AbstractTest {

    /**
     * @verifies create MessageEntry correctly
     */
    @Test
    void create_shouldCreateMessageEntryCorrectly() throws Exception {
        MessageEntry entry = MessageEntry.create(null, "foo", Arrays.asList(new Locale[] { Locale.ENGLISH, Locale.GERMAN }));
        Assertions.assertNotNull(entry);
        Assertions.assertEquals("foo", entry.getKey());
        Assertions.assertEquals(2, entry.getValues().size());
        Assertions.assertEquals("en", entry.getValues().get(0).getLanguage());
        Assertions.assertEquals("de", entry.getValues().get(1).getLanguage());
    }

    /**
     * @verifies order entries alphabetically by key
     */
    @Test
    void compareTo_shouldOrderEntriesAlphabeticallyByKey() throws Exception {
        MessageEntry entry1 = new MessageEntry("one", Collections.emptyList());
        MessageEntry entry2 = new MessageEntry("two", Collections.emptyList());
        Assertions.assertTrue(entry1.compareTo(entry2) < 0);
        Assertions.assertTrue(entry1.compareTo(entry1) == 0);
        Assertions.assertTrue(entry2.compareTo(entry1) > 0);
    }

    /**
     * @verifies return NONE when current value is empty
     */
    @Test
    void getTranslationStatus_shouldReturnNONEWhenCurrentValueIsEmpty() throws Exception {
        List<MessageValue> values = new ArrayList<>(2);
        values.add(new MessageValue("en", null, "value"));
        values.add(new MessageValue("de", null, "value"));
        MessageEntry entry = new MessageEntry("key", values);
        Assertions.assertEquals(2, entry.getValues().size());
        Assertions.assertEquals(TranslationStatus.NONE, entry.getTranslationStatus());
    }

    /**
     * @verifies return PARTIAL when current value differs from default value
     */
    @Test
    void getTranslationStatus_shouldReturnPARTIALWhenCurrentValueDiffersFromDefaultValue() throws Exception {
        {
            List<MessageValue> values = new ArrayList<>(2);
            values.add(new MessageValue("en", "value", "value"));
            values.add(new MessageValue("de", null, "wert"));
            MessageEntry entry = new MessageEntry("key", values);
            Assertions.assertEquals(2, entry.getValues().size());
            Assertions.assertEquals(TranslationStatus.PARTIAL, entry.getTranslationStatus());
        }
        {
            List<MessageValue> values = new ArrayList<>(2);
            values.add(new MessageValue("en", "value zzz", "value"));
            values.add(new MessageValue("de", "wert", "wert"));
            MessageEntry entry = new MessageEntry("key", values);
            Assertions.assertEquals(2, entry.getValues().size());
            Assertions.assertEquals(TranslationStatus.PARTIAL, entry.getTranslationStatus());
        }
    }

    /**
     * @verifies return FULL when current value matches default value
     */
    @Test
    void getTranslationStatus_shouldReturnFULLWhenCurrentValueMatchesDefaultValue() throws Exception {
        List<MessageValue> values = new ArrayList<>(2);
        values.add(new MessageValue("en", "value", "value"));
        values.add(new MessageValue("de", "wert", "wert"));
        MessageEntry entry = new MessageEntry("key", values);
        Assertions.assertEquals(2, entry.getValues().size());
        Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
    }

    /**
     * @verifies return correct status for language
     */
    @Test
    void getTranslationStatusForLanguage_shouldReturnCorrectStatusForLanguage() throws Exception {
        List<MessageValue> values = new ArrayList<>(2);
        values.add(new MessageValue("en", "value", "value"));
        values.add(new MessageValue("de", "wert zzz", "wert"));
        values.add(new MessageValue("fr", "", ""));
        MessageEntry entry = new MessageEntry("key", values);
        Assertions.assertEquals(3, entry.getValues().size());
        Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatusForLanguage("en"));
        Assertions.assertEquals(TranslationStatus.PARTIAL, entry.getTranslationStatusForLanguage("de"));
        Assertions.assertEquals(TranslationStatus.NONE, entry.getTranslationStatusForLanguage("fr"));
    }

    /**
     * @verifies trim suffix
     */
    @Test
    void getKey_shouldTrimSuffix() throws Exception {
        MessageEntry entry = new MessageEntry("foo", "bar   ", Collections.emptyList());
        Assertions.assertEquals("foo", entry.getKeyPrefix());
        Assertions.assertEquals("bar   ", entry.getKeySuffix());
        Assertions.assertEquals("foobar", entry.getKey());
    }
}
