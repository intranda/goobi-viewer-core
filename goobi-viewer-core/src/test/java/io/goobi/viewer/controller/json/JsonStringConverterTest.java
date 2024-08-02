package io.goobi.viewer.controller.json;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.viewer.record.views.VisibilityConditionInfo;

class JsonStringConverterTest {

    String json = "{requiredFileTypes:[IMAGE],"
            + "accessCondition:PRIV_DOWNLOAD_PDF,"
            + "pageTypes:[viewObject,viewFullscreen],"
            + "docTypes:[!,group, anchor],\n"
            + "numPages:2}";

    @Test
    void test() throws IOException {
        VisibilityConditionInfo info = JsonStringConverter.of(VisibilityConditionInfo.class).convert(json);
        Assertions.assertEquals(1, info.getRequiredFileTypes().size());
        Assertions.assertEquals("IMAGE", info.getRequiredFileTypes().get(0));
    }

}
