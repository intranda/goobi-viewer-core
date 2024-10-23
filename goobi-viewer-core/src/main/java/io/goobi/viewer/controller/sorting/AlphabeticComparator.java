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

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

public class AlphabeticComparator<T> implements Comparator<T> {

    private final int reverse;
    private final ITranslator<T> translator;
    private final Collator col;
    private final Locale locale;

    public AlphabeticComparator(String field, Locale locale, Function<T, String> stringifier) {
        this(field, locale, true, stringifier);
    }

    public AlphabeticComparator(String field, Locale locale, boolean asc, Function<T, String> stringifier) {
        this(ITranslator.getTranslatorForFacetField(field, stringifier), locale, asc);
    }

    public AlphabeticComparator(ITranslator<T> translator, Locale locale, boolean asc) {
        this.translator = translator;
        col = Collator.getInstance();
        col.setStrength(Collator.PRIMARY);
        this.reverse = asc ? 1 : -1;
        this.locale = locale;
    }

    private String getTranslatedLabel(T value) {
        return translator.translate(value, this.locale);
    }

    @Override
    public int compare(T o1, T o2) {
        String label1 = getTranslatedLabel(o1);
        String label2 = getTranslatedLabel(o2);

        return this.reverse * col.compare(label1, label2);
    }

}
