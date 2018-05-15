
package de.intranda.digiverso.presentation.controller;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.junit.Assert;
import org.junit.Test;

public class DateToolsTest {

    /**
     * @see DateTools#getLocalDate(Date,String)
     * @verifies format date correctly for the given language
     */
    @Test
    public void getLocalDate_shouldFormatDateCorrectlyForTheGivenLanguage() throws Exception {
        MutableDateTime date = new MutableDateTime();
        date.setYear(1980);
        date.setMonthOfYear(7);
        date.setDayOfMonth(10);
        date.setHourOfDay(13);
        date.setMinuteOfHour(15);
        date.setSecondOfMinute(30);
        Assert.assertEquals("10.07.1980 13:15", DateTools.getLocalDate(date.toDate(), "de"));
        Assert.assertEquals("07/10/1980 1:15 PM", DateTools.getLocalDate(date.toDate(), "en"));
    }

    /**
     * @see DateTools#getLocalDate(Date,String)
     * @verifies use English format for unknown languages
     */
    @Test
    public void getLocalDate_shouldUseEnglishFormatForUnknownLanguages() throws Exception {
        MutableDateTime date = new MutableDateTime();
        date.setYear(1980);
        date.setMonthOfYear(7);
        date.setDayOfMonth(10);
        date.setHourOfDay(13);
        date.setMinuteOfHour(15);
        date.setSecondOfMinute(30);
        Assert.assertEquals("07/10/1980 1:15 PM", DateTools.getLocalDate(date.toDate(), "eu"));
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse iso date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseIsoDateFormatsCorrectly() throws Exception {
        {
            DateTime date = DateTools.parseDateTimeFromString("2017-12-19 00:00:00", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            DateTime date = DateTools.parseDateTimeFromString("2017-12-19 00:00:00.000", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            DateTime date = DateTools.parseDateTimeFromString("2017-12-19T00:00:00", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            DateTime date = DateTools.parseDateTimeFromString("2017-12-19T00:00:00Z", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            DateTime date = DateTools.parseDateTimeFromString("2017-12-19", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            DateTime date = DateTools.parseDateTimeFromString("2017-12", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(1, date.getDayOfMonth());
        }
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse german date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseGermanDateFormatsCorrectly() throws Exception {
        {
            DateTime date = DateTools.parseDateTimeFromString("19.12.2017", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(19, date.getDayOfMonth());
        }
        {
            DateTime date = DateTools.parseDateTimeFromString("20.12.2017 01:02:03", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(20, date.getDayOfMonth());
            Assert.assertEquals(1, date.getHourOfDay());
            Assert.assertEquals(2, date.getMinuteOfHour());
            Assert.assertEquals(3, date.getSecondOfMinute());
        }
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse english date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseEnglishDateFormatsCorrectly() throws Exception {
        {
            DateTime date = DateTools.parseDateTimeFromString("12/20/2017", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(20, date.getDayOfMonth());
        }
        {
            DateTime date = DateTools.parseDateTimeFromString("12/19/2017 1:02:03 AM", false);
            Assert.assertNotNull(date);
            Assert.assertEquals(2017, date.getYear());
            Assert.assertEquals(12, date.getMonthOfYear());
            Assert.assertEquals(19, date.getDayOfMonth());
            Assert.assertEquals(1, date.getHourOfDay());
            Assert.assertEquals(2, date.getMinuteOfHour());
            Assert.assertEquals(3, date.getSecondOfMinute());
        }
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse chinese date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseChineseDateFormatsCorrectly() throws Exception {
        DateTime date = DateTools.parseDateTimeFromString("2017.12.19", false);
        Assert.assertNotNull(date);
        Assert.assertEquals(2017, date.getYear());
        Assert.assertEquals(12, date.getMonthOfYear());
        Assert.assertEquals(19, date.getDayOfMonth());
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse japanese date formats correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseJapaneseDateFormatsCorrectly() throws Exception {
        DateTime date = DateTools.parseDateTimeFromString("2017/12/19", false);
        Assert.assertNotNull(date);
        Assert.assertEquals(2017, date.getYear());
        Assert.assertEquals(12, date.getMonthOfYear());
        Assert.assertEquals(19, date.getDayOfMonth());
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies parse iso date as UTC correctly
     */
    @Test
    public void parseDateTimeFromString_shouldParseIsoDateAsUTCCorrectly() throws Exception {
        DateTime date = DateTools.parseDateTimeFromString("2017-12-19T01:01:00+01:00", true);
        Assert.assertNotNull(date);
        Assert.assertEquals(2017, date.getYear());
        Assert.assertEquals(12, date.getMonthOfYear());
        Assert.assertEquals(19, date.getDayOfMonth());
        Assert.assertEquals(0, date.getHourOfDay());
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies return null if unsupported format
     */
    @Test
    public void parseDateTimeFromString_shouldReturnNullIfUnsupportedFormat() throws Exception {
        Assert.assertNull(DateTools.parseDateTimeFromString("2017_12_20", false));
    }

    /**
     * @see DateTools#parseDateTimeFromString(String,boolean)
     * @verifies throw IllegalArgumentException if dateString is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseDateTimeFromString_shouldThrowIllegalArgumentExceptionIfDateStringIsNull() throws Exception {
        DateTools.parseDateTimeFromString(null, false);
    }
}