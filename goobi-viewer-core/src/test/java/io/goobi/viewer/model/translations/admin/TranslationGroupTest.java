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
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.translations.admin.MessageEntry.TranslationStatus;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;
import io.goobi.viewer.solr.SolrConstants;

public class TranslationGroupTest extends AbstractSolrEnabledTest {

    /**
     * @see TranslationGroup#getEntryCount()
     * @verifies return correct count
     */
    @Test
    public void getEntryCount_shouldReturnCorrectCount() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.SOLR_FIELD_NAMES, "group", null, 2);
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "DC", false));
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "PI", false));
        Assertions.assertEquals(2, group.getEntryCount());
    }

    /**
     * @see TranslationGroup#getFilteredEntries()
     * @verifies filter by key correctly
     */
    @Test
    public void getFilteredEntries_shouldFilterByKeyCorrectly() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.CORE_STRINGS, "group", null, 1);
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "FOO", false));
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "bar", false));
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "ufo", false));
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "unfortunate", false));

        group.setFilterString("fo");
        List<MessageEntry> filtered = group.getFilteredEntries();
        Assertions.assertEquals(3, filtered.size());
        Assertions.assertEquals("FOO", filtered.get(0).getKey());
        Assertions.assertEquals("ufo", filtered.get(1).getKey());
        Assertions.assertEquals("unfortunate", filtered.get(2).getKey());

    }

    /**
     * @see TranslationGroup#getFilteredEntries()
     * @verifies filter by value correctly
     */
    @Test
    public void getFilteredEntries_shouldFilterByValueCorrectly() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.CORE_STRINGS, "group", null, 1);
        TranslationGroupItem item = TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "item", false);
        group.getItems().add(item);
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("de", "eins", null));
            item.getEntries().add(new MessageEntry("1", values));
        }
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("de", "zwei", null));
            item.getEntries().add(new MessageEntry("2", values));
        }
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("de", "drei", null));
            item.getEntries().add(new MessageEntry("3", values));
        }
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("de", "vier", null));
            item.getEntries().add(new MessageEntry("4", values));
        }

        group.setFilterString("ei");
        List<MessageEntry> filtered = group.getFilteredEntries();
        Assertions.assertEquals(3, filtered.size());
        Assertions.assertEquals("1", filtered.get(0).getKey());
        Assertions.assertEquals("2", filtered.get(1).getKey());
        Assertions.assertEquals("3", filtered.get(2).getKey());
    }

    /**
     * @see TranslationGroup#selectEntry(int)
     * @verifies only select unfinished entries
     */
    @Test
    public void selectEntry_shouldOnlySelectUnfinishedEntries() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.SOLR_FIELD_NAMES, "group", null, 1);
        TranslationGroupItem item = TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "item", false);
        group.getItems().add(item);
        item.getEntries().clear();
        Assertions.assertEquals(0, item.getEntries().size());
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "one", null));
            values.add(new MessageValue("de", "eins zzz", null));
            MessageEntry entry = new MessageEntry("1", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.PARTIAL, entry.getTranslationStatus());
        }
        {
            // partially translated
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "two", null));
            values.add(new MessageValue("de", "zwei", null));
            MessageEntry entry = new MessageEntry("2", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            // partially translated
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "three", null));
            values.add(new MessageValue("de", "drei", null));
            MessageEntry entry = new MessageEntry("3", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "", null));
            values.add(new MessageValue("de", "vier", null));
            MessageEntry entry = new MessageEntry("4", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.PARTIAL, entry.getTranslationStatus());
        }
        Assertions.assertFalse(group.isAllFilteredEntriesFullyTranslated());

        group.setSelectedEntry(item.getEntries().get(0));
        Assertions.assertEquals("1", group.getSelectedEntry().getKey());
        // Next
        group.selectEntry(1);
        Assertions.assertEquals("4", group.getSelectedEntry().getKey());
        // Previous
        group.selectEntry(-1);
        Assertions.assertEquals("1", group.getSelectedEntry().getKey());

    }

    /**
     * @see TranslationGroup#selectEntry(int)
     * @verifies select fully translated entries if all are fully translated
     */
    @Test
    public void selectEntry_shouldSelectFullyTranslatedEntriesIfAllAreFullyTranslated() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.SOLR_FIELD_NAMES, "group", null, 1);
        TranslationGroupItem item = TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "item", false);
        group.getItems().add(item);
        item.getEntries().clear();
        Assertions.assertEquals(0, item.getEntries().size());
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "one", null));
            values.add(new MessageValue("de", "eins", null));
            MessageEntry entry = new MessageEntry("1", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            // partially translated
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "two", null));
            values.add(new MessageValue("de", "zwei", null));
            MessageEntry entry = new MessageEntry("2", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            // partially translated
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "three", null));
            values.add(new MessageValue("de", "drei", null));
            MessageEntry entry = new MessageEntry("3", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "four", null));
            values.add(new MessageValue("de", "vier", null));
            MessageEntry entry = new MessageEntry("4", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        Assertions.assertTrue(group.isAllFilteredEntriesFullyTranslated());

        group.setSelectedEntry(item.getEntries().get(0));
        Assertions.assertEquals("1", group.getSelectedEntry().getKey());
        // Next
        group.selectEntry(1);
        Assertions.assertEquals("2", group.getSelectedEntry().getKey());
        group.selectEntry(1);
        Assertions.assertEquals("3", group.getSelectedEntry().getKey());
        group.selectEntry(1);
        Assertions.assertEquals("4", group.getSelectedEntry().getKey());
        // Previous
        group.selectEntry(-1);
        Assertions.assertEquals("3", group.getSelectedEntry().getKey());
        group.selectEntry(-1);
        Assertions.assertEquals("2", group.getSelectedEntry().getKey());
        group.selectEntry(-1);
        Assertions.assertEquals("1", group.getSelectedEntry().getKey());
    }

    /**
     * @see TranslationGroup#selectEntry(int)
     * @verifies resume at the end when moving past first element
     */
    @Test
    public void selectEntry_shouldResumeAtTheEndWhenMovingPastFirstElement() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.SOLR_FIELD_NAMES, "group", null, 1);
        TranslationGroupItem item = TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "item", false);
        group.getItems().add(item);
        item.getEntries().clear();
        Assertions.assertEquals(0, item.getEntries().size());
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "one", null));
            values.add(new MessageValue("de", "eins", null));
            MessageEntry entry = new MessageEntry("1", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            // partially translated
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "two", null));
            values.add(new MessageValue("de", "zwei", null));
            MessageEntry entry = new MessageEntry("2", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            // partially translated
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "three", null));
            values.add(new MessageValue("de", "drei", null));
            MessageEntry entry = new MessageEntry("3", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        Assertions.assertTrue(group.isAllFilteredEntriesFullyTranslated());

        group.setSelectedEntry(item.getEntries().get(2));
        Assertions.assertEquals("3", group.getSelectedEntry().getKey());
        group.selectEntry(1);
        Assertions.assertEquals("1", group.getSelectedEntry().getKey());
    }

    /**
     * @see TranslationGroup#selectEntry(int)
     * @verifies resume at the beginning when moving past last element
     */
    @Test
    public void selectEntry_shouldResumeAtTheBeginningWhenMovingPastLastElement() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.SOLR_FIELD_NAMES, "group", null, 1);
        TranslationGroupItem item = TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "item", false);
        group.getItems().add(item);
        item.getEntries().clear();
        Assertions.assertEquals(0, item.getEntries().size());
        {
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "one", null));
            values.add(new MessageValue("de", "eins", null));
            MessageEntry entry = new MessageEntry("1", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            // partially translated
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "two", null));
            values.add(new MessageValue("de", "zwei", null));
            MessageEntry entry = new MessageEntry("2", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        {
            // partially translated
            List<MessageValue> values = new ArrayList<>();
            values.add(new MessageValue("en", "three", null));
            values.add(new MessageValue("de", "drei", null));
            MessageEntry entry = new MessageEntry("3", values);
            item.getEntries().add(entry);
            Assertions.assertEquals(TranslationStatus.FULL, entry.getTranslationStatus());
        }
        Assertions.assertTrue(group.isAllFilteredEntriesFullyTranslated());

        group.setSelectedEntry(item.getEntries().get(0));
        Assertions.assertEquals("1", group.getSelectedEntry().getKey());
        group.selectEntry(-1);
        Assertions.assertEquals("3", group.getSelectedEntry().getKey());
    }

    /**
     * @see TranslationGroup#isHasEntries()
     * @verifies return false if group has no entries
     */
    @Test
    public void isHasEntries_shouldReturnFalseIfGroupHasNoEntries() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.SOLR_FIELD_VALUES, "group", null, 2);
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_VALUES, "MD_FOO", false));
        Assertions.assertFalse(group.isHasEntries());
    }

    /**
     * @see TranslationGroup#isHasEntries()
     * @verifies return true if group has entries
     */
    @Test
    public void isHasEntries_shouldReturnTrueIfGroupHasEntries() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.SOLR_FIELD_VALUES, "group", null, 2);
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_VALUES, SolrConstants.DC, false));
        Assertions.assertTrue(group.isHasEntries());
    }
}
