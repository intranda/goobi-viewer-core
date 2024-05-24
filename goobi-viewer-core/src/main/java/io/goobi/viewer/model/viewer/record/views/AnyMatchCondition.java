package io.goobi.viewer.model.viewer.record.views;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;

public class AnyMatchCondition<T> extends Condition<Collection<T>> {

    public AnyMatchCondition(Collection<T> value, boolean matchIfEqual) {
        super(value, matchIfEqual);
    }

    public boolean matches(Collection<T> testValue) {
        if (this.isMatchIfEqual()) {
            return !CollectionUtils.union(this.getValue(), testValue).isEmpty();
        } else {
            return CollectionUtils.union(this.getValue(), testValue).isEmpty();
        }
    }

}
