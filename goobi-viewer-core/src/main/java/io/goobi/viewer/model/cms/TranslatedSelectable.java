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
package io.goobi.viewer.model.cms;

import java.util.Locale;

/**
 * A {@link io.goobi.viewer.model.cms.Selectable} which may also hold a locale to indicate the currently visible language/translation
 *
 * @author florian
 * @param <T>
 */
public class TranslatedSelectable<T> extends Selectable<T> {

    private static final long serialVersionUID = -8860896349160943598L;

    private Locale locale;

    /**
     * <p>
     * Constructor for TranslatedSelectable.
     * </p>
     *
     * @param value a T object.
     * @param selected a boolean.
     * @param defaultLocale a {@link java.util.Locale} object.
     */
    public TranslatedSelectable(T value, boolean selected, Locale defaultLocale) {
        super(value, selected);
        this.locale = defaultLocale;

    }

    /**
     * <p>
     * Getter for the field <code>locale</code>.
     * </p>
     *
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * <p>
     * Setter for the field <code>locale</code>.
     * </p>
     *
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * <p>
     * getLanguage.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLanguage() {
        return locale.getLanguage();
    }

    /**
     * <p>
     * setLanguage.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     */
    public void setLanguage(String language) {
        this.locale = Locale.forLanguageTag(language);
    }

}
