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
package io.goobi.viewer.model.translations;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Represents a single translated value for a specific language tag.
 */
@MappedSuperclass
public class Translation {

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    protected Long id;

    /**
     * An additional optional field used to identify the purpose or categorization of a translation. Useful if an object has more than one
     * relationship with Translation entities and needs to distinguish them in some way
     **/
    @Column(name = "tag", nullable = true, columnDefinition = "LONGTEXT")
    protected String tag;

    @Column(name = "language")
    protected String language;

    @Column(name = "translation_value", nullable = true, columnDefinition = "LONGTEXT")
    protected String translationValue;

    /**
     * Creates a new Translation instance.
     */
    public Translation() {
    }

    /**
     * Creates a new Translation instance.
     *
     * @param language ISO language code for this translation
     * @param translationValue translated text value
     */
    public Translation(String language, String translationValue) {
        this.language = language;
        this.translationValue = translationValue;
    }

    /**
     * Creates a new Translation instance.
     *
     * @param language ISO language code for this translation
     * @param tag category or purpose label for this translation
     * @param translationValue translated text value
     */
    public Translation(String language, String tag, String translationValue) {
        this.language = language;
        this.tag = tag;
        this.translationValue = translationValue;
    }

    /**
     * Clones constructor.
     * 
     * @param t translation to copy
     */
    public Translation(Translation t) {
        this.id = t.id;
        this.language = t.language;
        this.translationValue = t.translationValue;
        this.tag = t.tag;
    }

    /**
     * getTranslation.
     *
     * @param tag category label to match against translation entries
     * @param lang ISO language code of the desired translation
     * @param translations list of translations to search in
     * @return the translation value for the given language and tag, or null if not found
     */
    public static String getTranslation(List<? extends Translation> translations, String lang, String tag) {
        return getTranslation(translations, lang, tag, false);
    }

    /**
     * getTranslation.
     *
     * @param tag category label to match against translation entries
     * @param lang ISO language code of the desired translation
     * @param useFallback if no translation for lang exists, use the application default language
     * @param translations list of translations to search in
     * @return the translation value for the given language and tag, falling back to default language if requested and necessary
     */
    public static String getTranslation(List<? extends Translation> translations, String lang, String tag, boolean useFallback) {
        if (tag == null || lang == null) {
            return null;
        }

        for (Translation translation : translations) {
            if (translation.getTag().equals(tag)
                    && translation.getLanguage().equals(lang)
                    && StringUtils.isNotBlank(translation.getTranslationValue())) {
                return translation.getTranslationValue();
            }
        }

        if (useFallback) {
            String defaultLanguage = Optional.of(BeanUtils.getDefaultLocale()).map(Locale::getLanguage).orElse("en");
            return getTranslation(translations, defaultLanguage, tag, false);
        }

        return "";
    }

    public static void setTranslation(List<Translation> translations, String lang, String translationValue, String tag) {
        if (lang == null) {
            throw new IllegalArgumentException("lang may not be null");
        }
        if (translationValue == null) {
            throw new IllegalArgumentException("translationValue may not be null");
        }

        for (Translation translation : translations) {
            if (translation.getTag().equals(tag) && translation.getLanguage().equals(lang)) {
                translation.setTranslationValue(translationValue);
                return;
            }
        }
        translations.add(new Translation(lang, tag, translationValue));
    }

    /**
     * Getter for the field <code>id</code>.
     *
     * @return the database identifier of this translation entry
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for the field <code>id</code>.
     *
     * @param id the database identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for the field <code>tag</code>.
     *
     * @return the message key or category tag identifying this translation
     */
    public String getTag() {
        return tag;
    }

    /**
     * Setter for the field <code>tag</code>.
     *
     * @param tag the message key or category tag identifying this translation to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Getter for the field <code>language</code>.
     *
     * @return the ISO 639-1 language code for this translation
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Setter for the field <code>language</code>.
     *
     * @param language the ISO 639-1 language code for this translation to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Getter for the field <code>translationValue</code>.
     *
     * @return the translated text value
     */
    public String getTranslationValue() {
        return translationValue;
    }

    /**
     * Setter for the field <code>translationValue</code>.
     *
     * @param translationValue the translated text value to set
     */
    public void setTranslationValue(String translationValue) {
        this.translationValue = translationValue;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return translationValue;
    }

    /**
     * Hash code is build from hashCode of language.
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        if (this.language != null) {
            return this.language.hashCode();
        }
        return 0;
    }

    /**
     * Two Translations are equal if they are of the same class and both tag and language match.
     *
     * @param obj the object to compare to this translation
     * @return true if the given object is equal to this instance, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            Translation other = (Translation) obj;
            return Strings.CS.equals(this.language, other.language) && Strings.CS.equals(this.tag, other.tag);
        }
        return false;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(getTranslationValue());
    }
}
