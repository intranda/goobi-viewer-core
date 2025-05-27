package io.goobi.viewer.model.viewer.record.views;

import org.apache.commons.lang3.StringUtils;

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
