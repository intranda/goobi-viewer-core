package io.goobi.viewer.model.viewer.record.views;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;

public class AnyMatchCondition<T> extends Condition<Collection<T>> {

    public static <T> AnyMatchCondition<T> of(Collection<T> value, boolean matchIfEqual) {
        if (value == null || value.isEmpty()) {
            return (AnyMatchCondition<T>) Condition.NONE;
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

}
