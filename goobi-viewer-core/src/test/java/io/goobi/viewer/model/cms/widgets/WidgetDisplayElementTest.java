package io.goobi.viewer.model.cms.widgets;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;

class WidgetDisplayElementTest {

    private static final String LOREM_IPSUM =
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";
    private static final String LOREM_IPSUM_SHORT = "Lorem ipsum dolor sit amet,";

    @Test
    void test_descriptionFromType() {
        CustomSidebarWidget widget = new HtmlSidebarWidget();
        WidgetDisplayElement element = new WidgetDisplayElement(widget);
        Assertions.assertEquals(CustomWidgetType.WIDGET_HTML.getDescription(), element.getDescriptionOrTypeDescription().getText(Locale.GERMAN));
    }

    @Test
    void test_descriptionFromHtmlText() {
        HtmlSidebarWidget widget = new HtmlSidebarWidget();
        widget.getHtmlText().setText(LOREM_IPSUM, Locale.GERMAN);
        WidgetDisplayElement element = new WidgetDisplayElement(widget);
        Assertions.assertEquals(LOREM_IPSUM_SHORT + "...", element.getDescriptionOrTypeDescription().getText(Locale.GERMAN));
    }

    @Test
    void test_descriptionFromDescriptionText() {
        CustomSidebarWidget widget = new HtmlSidebarWidget();
        widget.getDescription().setText(LOREM_IPSUM, Locale.GERMAN);
        WidgetDisplayElement element = new WidgetDisplayElement(widget);
        Assertions.assertEquals(LOREM_IPSUM_SHORT + "...", element.getDescriptionOrTypeDescription().getText(Locale.GERMAN));
    }

}
