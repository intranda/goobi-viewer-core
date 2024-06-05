package io.goobi.viewer.model.viewer.record.views;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class Condition<T> {

    @SuppressWarnings("rawtypes")
    public static final Condition NONE = new Condition(null, true);

    private final T value;
    private final boolean matchIfEqual;

    public static <T> boolean isNone(Condition<T> condition) {
        return condition == null || condition == NONE;
    }

    public static <T> Condition<T> of(T value, boolean matchIfEqual) {
        if (value == null || StringUtils.isBlank(value.toString())) {
            return NONE;
        } else {
            return new Condition<T>(value, matchIfEqual);
        }
    }

    protected Condition(T value, boolean matchIfEqual) {
        this.value = value;
        this.matchIfEqual = matchIfEqual;
    }

    public boolean matches(T testValue) {
        if (this == NONE) {
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
        return String.valueOf(this.value) + ": " + this.matchIfEqual;
    }

}
