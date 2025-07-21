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
 * <p>
 * Abstract Translation class.
 * </p>
 */
@MappedSuperclass
public class Translation {

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    protected Long id;

    /**
     * An additional optional field used to identify the purpose or categorization of a translation. Usefull if an object has more than one
     * relationship with Translation entities and needs to distinguish them in some way
     **/
    @Column(name = "tag", nullable = true, columnDefinition = "LONGTEXT")
    protected String tag;

    @Column(name = "language")
    protected String language;

    @Column(name = "translation_value", nullable = true, columnDefinition = "LONGTEXT")
    protected String translationValue;

    /**
     * <p>
     * Constructor for Translation.
     * </p>
     */
    public Translation() {
    }

    /**
     * <p>
     * Constructor for Translation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @param translationValue a {@link java.lang.String} object.
     */
    public Translation(String language, String translationValue) {
        this.language = language;
        this.translationValue = translationValue;
    }

    /**
     * <p>
     * Constructor for Translation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @param tag a {@link java.lang.String} object.
     * @param translationValue a {@link java.lang.String} object.
     */
    public Translation(String language, String tag, String translationValue) {
        this.language = language;
        this.tag = tag;
        this.translationValue = translationValue;
    }

    /**
     * Clone constructor
     * 
     * @param t
     */
    public Translation(Translation t) {
        this.id = t.id;
        this.language = t.language;
        this.translationValue = t.translationValue;
        this.tag = t.tag;
    }

    /**
     * <p>
     * getTranslation.
     * </p>
     *
     * @param tag a {@link java.lang.String} object.
     * @param lang a {@link java.lang.String} object.
     * @param translations a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getTranslation(List<? extends Translation> translations, String lang, String tag) {
        return getTranslation(translations, lang, tag, false);
    }

    /**
     * <p>
     * getTranslation.
     * </p>
     *
     * @param tag a {@link java.lang.String} object.
     * @param lang a {@link java.lang.String} object.
     * @param useFallback if no translation for lang exists, use the application default language
     * @param translations a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
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
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>tag</code>.
     * </p>
     *
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * <p>
     * Setter for the field <code>tag</code>.
     * </p>
     *
     * @param tag the tag to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * <p>
     * Getter for the field <code>language</code>.
     * </p>
     *
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * <p>
     * Setter for the field <code>language</code>.
     * </p>
     *
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * <p>
     * Getter for the field <code>translationValue</code>.
     * </p>
     *
     * @return the translationValue
     */
    public String getTranslationValue() {
        return translationValue;
    }

    /**
     * <p>
     * Setter for the field <code>translationValue</code>.
     * </p>
     *
     * @param translationValue the translationValue to set
     */
    public void setTranslationValue(String translationValue) {
        this.translationValue = translationValue;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return translationValue;
    }

    /**
     * Hash code is build from hashCode of language
     */
    @Override
    public int hashCode() {
        if (this.language != null) {
            return this.language.hashCode();
        }
        return 0;
    }

    /**
     * Two Translations are equal if they are of the same class and both tag and language match
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
