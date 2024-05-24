package io.goobi.viewer.model.viewer.record.views;

import java.util.Objects;

public class Condition<T> {

    @SuppressWarnings("rawtypes")
    public static final Condition NONE = new Condition(null, true);

    private final T value;
    private final boolean matchIfEqual;

    public Condition(T value, boolean matchIfEqual) {
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

}
