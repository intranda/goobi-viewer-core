package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.calendar.CalendarItemCentury;

class CalendarBeanTest extends AbstractSolrEnabledTest {

    @Test
    void test_centuryFromYear() {

        CalendarBean bean = new CalendarBean();

        assertEquals(16, bean.getCentury(1535));
        assertEquals(10, bean.getCentury(900));
        assertEquals(1, bean.getCentury(34));
        assertEquals(-1, bean.getCentury(-35));
        assertEquals(-4, bean.getCentury(-333));
        assertEquals(-301, bean.getCentury(-30000));

    }

    @Test
    void test_getAllCenturies() throws PresentationException, IndexUnreachableException {
        CalendarBean bean = new CalendarBean();
        List<CalendarItemCentury> centuries = bean.getAllActiveCenturies();
        Assertions.assertTrue(centuries.size() > 0);
    }

}
