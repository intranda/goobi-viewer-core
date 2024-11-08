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

import java.util.Locale;
import java.util.function.Function;

public class NoopTranslator<T> implements ITranslator<T> {

    private final Function<T, String> stringifier;

    public NoopTranslator(Function<T, String> stringifier) {
        this.stringifier = stringifier;
    }

    @Override
    public String translate(T value, Locale locale) {
        return this.stringifier.apply(value);
    }

}
