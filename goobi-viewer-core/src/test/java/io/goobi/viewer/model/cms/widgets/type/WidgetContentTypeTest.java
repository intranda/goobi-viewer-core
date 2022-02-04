package io.goobi.viewer.model.cms.widgets.type;

import static org.junit.Assert.*;

import org.junit.Test;

public class WidgetContentTypeTest {

    @Test
    public void testGetByName() {
        assertEquals(DefaultWidgetType.WIDGET_BROWSING, WidgetContentType.valueOf("WIDGET_BROWSING"));
        assertEquals(AutomaticWidgetType.WIDGET_CMSGEOMAP, WidgetContentType.valueOf("WIDGET_CMSGEOMAP"));
        assertEquals(CustomWidgetType.WIDGET_FIELDFACETS, WidgetContentType.valueOf("WIDGET_FIELDFACETS"));
    }
    
    @Test
    public void testGetGenerationType() {
        assertEquals(WidgetGenerationType.DEFAULT, WidgetContentType.getGenerationType(DefaultWidgetType.WIDGET_BROWSING));
        assertEquals(WidgetGenerationType.AUTOMATIC, WidgetContentType.getGenerationType(AutomaticWidgetType.WIDGET_CMSGEOMAP));
        assertEquals(WidgetGenerationType.CUSTOM, WidgetContentType.getGenerationType(CustomWidgetType.WIDGET_FIELDFACETS));
    }

}
