package io.goobi.viewer.model.cms.widgets;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CustomSidebarWidgetTest {

    private static final String LOREM_IPSUM =
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";
    private static final String LOREM_IPSUM_SHORT = "Lorem ipsum dolor sit amet,";

    @Test
    void test_shortDescription() {
        CustomSidebarWidget widget = new CustomSidebarWidget();
        Assertions.assertFalse(widget.isHasShortDescription());
        widget.getDescription().setText(LOREM_IPSUM, Locale.GERMAN);
        Assertions.assertTrue(widget.isHasShortDescription());
        Assertions.assertEquals(LOREM_IPSUM_SHORT + "...", widget.getShortDescription(35).getValueOrFallback(Locale.GERMAN));
    }

}
