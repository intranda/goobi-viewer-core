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
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.recordnotes.CMSRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSSingleRecordNote;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * @author florian
 *
 */
class CmsRecordNoteEditBeanTest extends AbstractDatabaseEnabledTest {

    CmsRecordNoteEditBean bean;

    String englishText = "ENGLISH";
    String germanText = "GERMAN";

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        bean = new CmsRecordNoteEditBean();
        bean.setSelectedLocale(Locale.ENGLISH);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testChangeLanguage() {
        CMSSingleRecordNote note = new CMSSingleRecordNote("PI1");
        bean.setNote(note);

        note.getNoteText().setText(englishText);
        //set note title as well so the note has valid english translations.
        //Otherwise bean.setSelectedLocale(Locale.GERMAN) won't change locale
        note.getNoteTitle().setText(englishText);

        assertEquals(englishText, note.getNoteText().getText(Locale.ENGLISH));
        assertEquals("", note.getNoteText().getText(Locale.GERMAN));

        bean.setSelectedLocale(Locale.GERMAN);
        assertEquals("", note.getNoteText().getText());
        note.getNoteText().setText(germanText);
        assertEquals(germanText, note.getNoteText().getText());
        assertEquals(germanText, note.getNoteText().getText(Locale.GERMAN));
        assertEquals(englishText, note.getNoteText().getText(Locale.ENGLISH));
    }

    @Test
    void testMustFillDefaultLanguageFirst() {

        //default language is en
        assertEquals(Locale.ENGLISH, BeanUtils.getDefaultLocale());

        //session language is en
        bean.setSelectedLocale(Locale.GERMAN);

        //note only has german text
        CMSSingleRecordNote note = new CMSSingleRecordNote("PI1");
        note.getNoteText().setText(germanText, Locale.GERMAN);
        note.getNoteTitle().setText(germanText, Locale.GERMAN);

        //when setting note, selected locale should switch to english because english texts are missing
        bean.setNote(note);
        assertEquals(Locale.ENGLISH, bean.getSelectedLocale());
        assertEquals("", bean.getNote().getNoteText().getText());

        //reset bean
        bean.setNote(null);
        bean.setSelectedLocale(Locale.GERMAN);

        //note now has english and german texts
        note.getNoteText().setText(englishText, Locale.ENGLISH);
        note.getNoteTitle().setText(englishText, Locale.ENGLISH);

        //now the selected language remains german because the set note has english texts
        bean.setNote(note);
        assertEquals(Locale.GERMAN, bean.getSelectedLocale());
        assertEquals(germanText, bean.getNote().getNoteText().getText());

    }

    @Test
    void testCreateTitleLabelMultiLanguage() {
        IMetadataValue md = new MultiLanguageMetadataValue();
        md.setValue("deutsch", "de");
        md.setValue("english", "en");
        md.setValue("francais", "fr");
        md.setValue("deutsch", "_default");

        TranslatedText text = bean.createRecordTitle(md);

        String data = new TranslatedTextConverter().convertToDatabaseColumn(text);
        String[] expected = { "\"en\":[\"english\"]", "\"de\":[\"deutsch\"]", "\"fr\":[\"francais\"]" };
        assertTrue(data.contains(expected[0]), "data String is " + data);
        assertTrue(data.contains(expected[1]), "data String is " + data);
        assertTrue(data.contains(expected[2]), "data String is " + data);
    }

    @Test
    void testCreateTitleLabelSingleValue() {
        IMetadataValue md = new SimpleMetadataValue("default");

        TranslatedText text = bean.createRecordTitle(md);

        String data = new TranslatedTextConverter().convertToDatabaseColumn(text);
        assertEquals("default", data);
    }

    @Test
    void testTranslationEmpyIfNoFieldsFilled() {
        CMSSingleRecordNote note = new CMSSingleRecordNote("PI1");
        TranslatedText title = note.getNoteTitle();
        TranslatedText text = note.getNoteText();
        bean.setNote(note);

        //default language is en
        assertEquals(Locale.ENGLISH, BeanUtils.getDefaultLocale());
        title.setText(englishText, Locale.ENGLISH);
        text.setText(englishText, Locale.ENGLISH);

        assertFalse(bean.isValid(Locale.GERMAN));
        assertFalse(bean.isComplete(Locale.GERMAN));
        assertTrue(bean.isEmpty(Locale.GERMAN));
    }

    @Test
    void testTranslationNotCompleteIfNotAllFieldsFilled() {
        CMSRecordNote note = new CMSSingleRecordNote("PI1");
        TranslatedText title = note.getNoteTitle();
        TranslatedText text = note.getNoteText();
        bean.setNote(note);

        //default language is en
        assertEquals(Locale.ENGLISH, BeanUtils.getDefaultLocale());

        title.setText(englishText, Locale.ENGLISH);
        text.setText(englishText, Locale.ENGLISH);
        text.setText(germanText, Locale.GERMAN);

        assertFalse(bean.isComplete(Locale.GERMAN));
        assertFalse(bean.isEmpty(Locale.GERMAN));
    }

    @Test
    void testTranslationCompleteIfSameFieldsFillesAsInDefaultLanguage() {
        CMSSingleRecordNote note = new CMSSingleRecordNote("PI1");
        TranslatedText title = note.getNoteTitle();
        TranslatedText text = note.getNoteText();
        bean.setNote(note);

        //default language is en
        assertEquals(Locale.ENGLISH, BeanUtils.getDefaultLocale());

        text.setText(englishText, Locale.ENGLISH);
        text.setText(germanText, Locale.GERMAN);

        assertTrue(bean.isComplete(Locale.GERMAN));
        assertFalse(bean.isEmpty(Locale.GERMAN));
    }

    @Test
    void testTranslationCompleteIfAllFieldsFilled() {
        CMSSingleRecordNote note = new CMSSingleRecordNote("PI1");
        TranslatedText title = note.getNoteTitle();
        TranslatedText text = note.getNoteText();
        bean.setNote(note);

        //default language is en
        assertEquals(Locale.ENGLISH, BeanUtils.getDefaultLocale());

        title.setText(englishText, Locale.ENGLISH);
        text.setText(englishText, Locale.ENGLISH);
        text.setText(germanText, Locale.GERMAN);
        title.setText(germanText, Locale.GERMAN);

        assertTrue(bean.isComplete(Locale.GERMAN));
        assertFalse(bean.isEmpty(Locale.GERMAN));
    }
}
