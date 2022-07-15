package io.goobi.viewer.api.rest.v1.statistics.usage;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;

public class UsageStatisticsResourceTest {

    @Test
    public void testParseDate() {
        String input = "2022-07-13";
        LocalDate date = new UsageStatisticsResource().getLocalDate(input);
        assertEquals(2022, date.getYear());
        assertEquals(Month.JULY, date.getMonth());
        assertEquals(13, date.getDayOfMonth());
    }

}
