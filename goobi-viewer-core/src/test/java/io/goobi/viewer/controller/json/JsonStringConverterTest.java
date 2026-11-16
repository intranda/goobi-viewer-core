package io.goobi.viewer.controller.json;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import io.goobi.viewer.model.viewer.record.views.VisibilityConditionInfo;

@Execution(ExecutionMode.CONCURRENT)
class JsonStringConverterTest {

    String json = "{contentType:[IMAGE],"
            + "accessCondition:PRIV_DOWNLOAD_PDF,"
            + "pageType:[viewObject,viewFullscreen],"
            + "docType:[!,group, anchor],\n"
            + "numPages:2}";

    /**
     * @verifies deserialize json string to visibility condition info
     */
    @Test
    void convert_shouldDeserializeJsonStringToVisibilityConditionInfo() throws IOException {
        VisibilityConditionInfo info = JsonStringConverter.of(VisibilityConditionInfo.class).convert(json);
        Assertions.assertEquals(1, info.getContentType().size());
        Assertions.assertEquals("IMAGE", info.getContentType().get(0));
    }

}
