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

import java.util.Collection;
import java.util.Locale;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Interface for objects containing translations for a set of languages. Used to construct tab panels to switch beween languages
 *
 * @author florian
 *
 */
public interface IPolyglott {

    /**
     * If this returns true, an associated language tab should have the 'already-translated' class, otherwise the '-partly-translated' class unless
     * {@link #isEmpty(Locale)} also returns true
     *
     * @param locale
     * @return true if {@link #isValid(Locale)} returns true for the given locale and all fields contain a value which have a value in the default
     *         locale. For the default locale, {@link #isComplete(Locale)} and {@link #isValid(Locale)} are identical. For implementations with only
     *         one field, both methods are also always identical
     */
    public boolean isComplete(Locale locale);

    /**
     * Only meaningfull for the default language for which all required fields must be filled
     *
     * @param locale
     * @return true if all required fields contain a value in the given locale
     */
    public boolean isValid(Locale locale);

    /**
     * If this returns true, an associated language tab should have neither the 'already-translated' nor the '-partly-translated' class
     *
     * @param locale
     * @return true if no fields are filled for the given locale
     */
    public boolean isEmpty(Locale locale);

    /**
     *
     * @return the locale currently set by {@link #setSelectedLocale(Locale)}
     */
    public Locale getSelectedLocale();

    /**
     * Set the locale to use for display and editing
     * 
     * @param locale
     */
    public void setSelectedLocale(Locale locale);

    /**
     * Convenience method. Calls {@link #setSelectedLocale(Locale)} with the {@link Locale} given by the passed argument
     *
     * @param language
     */
    public default void setSelectedLocale(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (locale != null) {
            this.setSelectedLocale(locale);
        } else {
            throw new IllegalArgumentException("'" + language + "' is not a valid language tag");
        }
    }

    /**
     *
     * @return true if the currently selected locale is also the default locale
     */
    public default boolean isDefaultLocaleSelected() {
        return getSelectedLocale() != null && getSelectedLocale().equals(getDefaultLocale());
    }

    /**
     *
     * @param locale
     * @return return true if the currently selected locale is the given locale
     */
    public default boolean isSelected(Locale locale) {
        return locale != null && locale.equals(getSelectedLocale());
    }

    /**
     * Get a list of all locales configured in the faces-configuration file.
     *
     * @return Collection<Locale>
     */
    public default Collection<Locale> getLocales() {
        return IPolyglott.getLocalesStatic();
    }

    /**
     * Get a list of all locales configured in the faces-configuration file
     *
     * @return Collection<Locale>
     */
    public static Collection<Locale> getLocalesStatic() {
        return ViewerResourceBundle.getAllLocales();
    }

    /**
     * @return the default locale configured in the faces-configuration file
     */
    public static Locale getDefaultLocale() {
        return ViewerResourceBundle.getDefaultLocale();
    }

    /**
     *
     * @return the locale set in the current faces context
     */
    public static Locale getCurrentLocale() {
        try {
            return BeanUtils.getInitialLocale();
        } catch (NullPointerException e) {
            return Locale.ENGLISH;
        }
    }

}
