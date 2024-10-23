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

import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

public class ObjectComparatorBuilder {

    public static <T> Comparator<T> build(String sortOrder, Locale locale, Function<T, String> stringifier) {
        ITranslator<T> translator = locale == null ? new NoopTranslator<T>(stringifier) : new ResourceBundleTranslator<T>(stringifier);
        switch (sortOrder) {
            case "numerical":
            case "numerical_asc":
                return new NumericComparator<T>(stringifier);
            case "numerical_desc":
                return new NumericComparator<T>(false, stringifier);
            case "alphabetical":
            case "alphabetical_asc":
                return new AlphabeticComparator<T>(translator, locale, true);
            case "alphabetical_desc":
                return new AlphabeticComparator<T>(translator, locale, false);
            case "alphabetical_raw":
            case "alphabetical_raw_asc":
                return new AlphabeticComparator<T>(new NoopTranslator<T>(stringifier), null, true);
            case "alphabetical_raw_desc":
                return new AlphabeticComparator<T>(new NoopTranslator<T>(stringifier), null, false);
            case "alphanumerical":
            case "natural":
            case "natural_asc":
                return new AlphanumComparator<T>(true, locale, translator);
            case "alphanumerical_desc":
            case "natural_desc":
                return new AlphanumComparator<T>(false, locale, translator);
            default:
                return new NoopComparator<T>();
        }
    }

}
