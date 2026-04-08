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

import org.apache.commons.lang3.StringUtils;

/**
 * A numeric {@link Condition} that matches a test value by comparing it against a threshold,
 * either requiring the test value to be greater than or equal to the threshold, or strictly less.
 *
 * @param <T> the numeric type used for comparison
 */
public class ComparisonCondition<T extends Number> extends Condition<T> {

    protected ComparisonCondition(T value, boolean matchIfLarger) {
        super(value, matchIfLarger);
    }

    public boolean isMatchIfLarger() {
        return super.isMatchIfEqual();
    }

    @Override
    public boolean matches(T testValue) {
        if (this.getValue() == null) {
            return true;
        } else if (this.isMatchIfLarger()) {
            return testValue.doubleValue() >= this.getValue().doubleValue();
        } else {
            return testValue.doubleValue() < this.getValue().doubleValue();
        }
    }

    public static ComparisonCondition<Integer> ofInteger(String number) {
        if (StringUtils.isBlank(number) || !number.matches("!?[\\d.]+")) {
            return new ComparisonCondition<>(null, true);
        }
        boolean matchIfLarger = !number.startsWith("!");
        if (number.contains(".")) {
            throw new IllegalArgumentException("Given number '" + number + "' is not an integer");
        }
        Integer value = Integer.valueOf(number.replace("!", ""));
        return new ComparisonCondition<>(value, matchIfLarger);
    }

    public static ComparisonCondition<Double> ofDouble(String number) {
        boolean matchIfLarger = !number.startsWith("!");
        Double value = Double.valueOf(number.replace("!", ""));
        return new ComparisonCondition<>(value, matchIfLarger);

    }

}
