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

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Tests a condition on a collection of values, passing either if any value matches of if none matches, depending on the matchIfEqual property
 */
public class AnyMatchCondition<T> extends Condition<Collection<T>> {

    public static <T> AnyMatchCondition<T> of(Collection<T> value, boolean matchIfEqual) {
        if (value == null || value.isEmpty()) {
            return new AnyMatchCondition<T>(Collections.emptyList(), true);
        } else {
            return new AnyMatchCondition<T>(value, matchIfEqual);
        }
    }

    protected AnyMatchCondition(Collection<T> value, boolean matchIfEqual) {
        super(value, matchIfEqual);
    }

    public boolean matches(Collection<T> testValue) {
        if (this.getValue() == null || this.getValue().isEmpty()) {
            return true;
        } else if (this.isMatchIfEqual()) {
            return CollectionUtils.containsAny(this.getValue(), testValue);
        } else {
            return !CollectionUtils.containsAny(this.getValue(), testValue);
        }
    }

    @Override
    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    public static AnyMatchCondition<? extends Object> none() {
        return new AnyMatchCondition<Object>(Collections.emptyList(), true);
    }

}
