package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.calendar.CalendarItemCentury;
import io.goobi.viewer.model.calendar.CalendarItemWeek;

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

    @Test
    void test_addDaysBeforeMonday() {
        {
            LocalDate date = LocalDate.of(1569, 6, 1);
            assertEquals(DayOfWeek.SUNDAY, date.getDayOfWeek());
            CalendarItemWeek week = new CalendarItemWeek("week", 0, 0);
            CalendarBean.addEmptyDays(week, date);
            assertEquals(6, week.getDaysOfWeek().size());
        }
        {
            LocalDate date = LocalDate.of(1569, 1, 1);
            assertEquals(DayOfWeek.WEDNESDAY, date.getDayOfWeek());
            CalendarItemWeek week = new CalendarItemWeek("week", 0, 0);
            CalendarBean.addEmptyDays(week, date);
            assertEquals(2, week.getDaysOfWeek().size());
        }
        {
            LocalDate date = LocalDate.of(1569, 12, 1);
            assertEquals(DayOfWeek.MONDAY, date.getDayOfWeek());
            CalendarItemWeek week = new CalendarItemWeek("week", 0, 0);
            CalendarBean.addEmptyDays(week, date);
            assertEquals(0, week.getDaysOfWeek().size());
        }
    }

}
