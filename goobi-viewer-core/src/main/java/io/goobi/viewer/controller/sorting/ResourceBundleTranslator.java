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
package io.goobi.viewer.controller.sorting;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import io.goobi.viewer.messages.ViewerResourceBundle;

public class ResourceBundleTranslator<T> implements ITranslator<T> {

    private final List<Locale> allLocales;
    private final Locale defaultLocale;
    private Function<T, String> stringifier;

    public ResourceBundleTranslator(Function<T, String> stringifier) {
        this(ViewerResourceBundle.getAllLocales(), ViewerResourceBundle.getDefaultLocale(), stringifier);
    }

    public ResourceBundleTranslator(List<Locale> allLocales, Locale defaultLocale, Function<T, String> stringifier) {
        super();
        this.allLocales = allLocales;
        this.defaultLocale = defaultLocale;
        this.stringifier = stringifier;
    }

    @Override
    public String translate(T value, Locale locale) {
        return ViewerResourceBundle.getTranslation(stringifier.apply(value), getLocaleToUse(locale));
    }

    private Locale getLocaleToUse(Locale locale) {
        if (locale != null && this.allLocales.contains(locale)) {
            return locale;
        } else {
            return this.defaultLocale;
        }
    }

}
