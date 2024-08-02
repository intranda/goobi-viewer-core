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
package io.goobi.viewer.model.viewer.record.views;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Condition checker. Checks a given value against an internal value and passes the check if either the values match or don't match, depending on the
 * matchIfEqual property. Testing is always done via {@link Objects#equals(Object, Object)}
 * 
 * @param <T> The class of the values to test.
 */
public class Condition<T> {

    private final T value;
    private final boolean matchIfEqual;

    public static <T> Condition<T> of(T value, boolean matchIfEqual) {
        if (value == null || StringUtils.isBlank(value.toString())) {
            return new Condition<T>(null, true);
        } else {
            return new Condition<T>(value, matchIfEqual);
        }
    }

    protected Condition(T value, boolean matchIfEqual) {
        this.value = value;
        this.matchIfEqual = matchIfEqual;
    }

    public boolean matches(T testValue) {
        if (this.isEmpty()) {
            return true;
        }
        if (matchIfEqual) {
            return Objects.equals(this.value, testValue);
        } else {
            return !Objects.equals(this.value, testValue);
        }
    }

    public T getValue() {
        return value;
    }

    public boolean isMatchIfEqual() {
        return matchIfEqual;
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "NONE";
        }
        return String.valueOf(this.value) + ": " + this.matchIfEqual;
    }

    public boolean isEmpty() {
        return value == null || value.toString().isBlank();
    }

    public static Condition none() {
        return new Condition<Object>(null, true);
    }

}
