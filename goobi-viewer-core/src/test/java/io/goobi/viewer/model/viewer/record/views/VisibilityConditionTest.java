package io.goobi.viewer.model.viewer.record.views;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.viewer.BaseMimeType;

class VisibilityConditionTest {

    @Test
    void testReadCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setBaseMimeType("image");
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getMimeType().matches(BaseMimeType.IMAGE));
    }

    @Test
    void testReadUnknownCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setBaseMimeType("images");
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertFalse(cond.getMimeType().matches(BaseMimeType.IMAGE));
    }

}
