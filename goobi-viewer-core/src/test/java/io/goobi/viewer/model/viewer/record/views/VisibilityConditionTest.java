package io.goobi.viewer.model.viewer.record.views;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.viewer.BaseMimeType;

class VisibilityConditionTest {

    @Test
    void testReadCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setBaseMimeType(List.of("image"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getMimeType().matches(List.of(BaseMimeType.IMAGE)));
    }

    @Test
    void testFileTypeCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setRequiredFileTypes(List.of("IMAGE"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getFileTypes().matches(List.of(FileType.IMAGE)));
        Assertions.assertFalse(cond.getFileTypes().matches(List.of(FileType.EPUB)));
    }

    @Test
    void testReadUnknownCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setBaseMimeType(List.of("images"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertFalse(cond.getMimeType().matches(List.of(BaseMimeType.IMAGE)));
    }

    @Test
    void testOtherCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setNumPages("2");
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getMimeType().matches(List.of(BaseMimeType.IMAGE)));
        Assertions.assertTrue(cond.getMimeType().matches(List.of(BaseMimeType.APPLICATION)));
        Assertions.assertTrue(cond.getNumPages().matches(312));
    }

}
