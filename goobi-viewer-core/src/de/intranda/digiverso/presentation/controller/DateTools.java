/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for date parsing, etc.
 */
public class DateTools {

    private static final Logger logger = LoggerFactory.getLogger(DateTools.class);

    // "DateTimeFormat is thread-safe and immutable, and the formatters it returns are as well." - JodaTime Javadoc
    public static DateTimeFormatter formatterISO8601BasicDateTime = ISODateTimeFormat.basicDateTime(); // yyyyMMddHHmmss
    public static DateTimeFormatter formatterISO8601BasicDate = ISODateTimeFormat.basicDate(); // yyyyMMdd
    public static DateTimeFormatter formatterISO8601BasicDateNoYear = DateTimeFormat.forPattern("MMdd");
    public static DateTimeFormatter formatterISO8601YearMonth = DateTimeFormat.forPattern("yyyy-MM"); // yyyy-MM
    public static DateTimeFormatter formatterISO8601Date = ISODateTimeFormat.date(); // yyyy-MM-dd
    public static DateTimeFormatter formatterISO8601DateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    public static DateTimeFormatter formatterISO8601DateTimeMS = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static DateTimeFormatter formatterISO8601DateTimeFull = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static DateTimeFormatter formatterISO8601DateTimeFullWithTimeZone = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    public static DateTimeFormatter formatterISO8601Time = DateTimeFormat.forPattern("HH:mm:ss");
    public static DateTimeFormatter formatterDEDateTime = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss");
    public static DateTimeFormatter formatterENDateTime = DateTimeFormat.forPattern("MM/dd/yyyy h:mm:ss a");
    public static DateTimeFormatter formatterDEDateTimeNoSeconds = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm");
    public static DateTimeFormatter formatterENDateTimeNoSeconds = DateTimeFormat.forPattern("MM/dd/yyyy h:mm a");
    public static DateTimeFormatter formatterDEDate = DateTimeFormat.forPattern("dd.MM.yyyy");
    public static DateTimeFormatter formatterENDate = DateTimeFormat.forPattern("MM/dd/yyyy");
    public static DateTimeFormatter formatterCNDate = DateTimeFormat.forPattern("yyyy.MM.dd");
    public static DateTimeFormatter formatterJPDate = DateTimeFormat.forPattern("yyyy/MM/dd");;
    public static DateTimeFormatter formatterFilename = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    /**
     * Converts the given string to a list of Date objects created from the contents of this string (years or whole dates).
     *
     * @param dateString
     * @return
     * @should parse single date correctly
     * @should parse multiple dates correctly
     * @should parse dates in parentheses correctly
     */
    public static List<Date> parseMultipleDatesFromString(String dateString) {
        List<Date> ret = new ArrayList<>();

        // logger.debug("Parsing date string : {}", dateString);
        if (StringUtils.isNotEmpty(dateString)) {
            String splittingChar = "/";
            String[] dateStringSplit = dateString.split(splittingChar);
            for (String s : dateStringSplit) {
                s = s.trim();

                // Check whether this is a well-formed date and not a range or anything
                {
                    Date date = parseDateFromString(s);
                    if (date != null) {
                        ret.add(date);
                        continue;
                    }
                }

                // Try finding a complete date in the string (enclosed in parentheses)
                Pattern p = Pattern.compile(Helper.REGEX_PARENTHESES);
                Matcher m = p.matcher(s);
                if (m.find()) {
                    s = s.substring(m.start() + 1, m.end() - 1);
                    logger.trace("Extracted date: {}", s);
                    Date date = parseDateFromString(s);
                    if (date != null) {
                        ret.add(date);
                        continue;
                    }
                }

                // If no complete date was found, just use the year
                if (s.contains(" ")) {
                    String[] sSplit = s.split(" ");
                    s = sSplit[0];
                }
                try {
                    int year = Integer.valueOf(s);
                    Calendar cal = Calendar.getInstance();
                    cal.set(year, 0, 1, 0, 0, 0);
                    ret.add(cal.getTime());
                } catch (NumberFormatException e) {
                    logger.error("Could not parse year: {}", s);
                }
            }
        }

        return ret;
    }

    /**
     *
     * @param dateString
     * @return
     * @should parse iso date formats correctly
     * @should parse iso date as UTC correctly
     * @should parse german date formats correctly
     * @should parse english date formats correctly
     * @should parse chinese date formats correctly
     * @should parse japanese date formats correctly
     * @should return null if unsupported format
     * @should throw IllegalArgumentException if dateString is null
     */
    public static DateTime parseDateTimeFromString(String dateString, boolean fromUTC) {
        if (dateString == null) {
            throw new IllegalArgumentException("dateString may not be null");
        }

        try {
            if (fromUTC) {
                return formatterISO8601DateTimeFullWithTimeZone.withZoneUTC().parseDateTime(dateString);
            }
            return formatterISO8601DateTimeFullWithTimeZone.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterISO8601DateTimeFull.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterISO8601DateTimeMS.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterISO8601DateTime.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterISO8601Date.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterISO8601YearMonth.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterDEDateTime.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterENDateTime.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterDEDate.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterENDate.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterJPDate.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }
        try {
            return formatterCNDate.parseDateTime(dateString);
        } catch (IllegalArgumentException e) {
        }

        return null;
    }

    /**
     *
     * @param dateString
     * @return
     */
    public static Date parseDateFromString(String dateString) {
        DateTime dateTime = parseDateTimeFromString(dateString, false);
        if (dateTime != null) {
            return dateTime.toDate();
        }

        return null;
    }

    /**
     * Returns the string representation of the given <code>Date</code> based on the given ISO 639-1 language code.
     *
     * @param date Date to format.
     * @param language ISO 639-1 (two-character) language code.
     * @return
     * @should format date correctly for the given language
     * @should use English format for unknown languages
     */
    public static String getLocalDate(Date date, String language) {
        switch (language) {
            case "de":
                return formatterDEDateTimeNoSeconds.print(date.getTime());
            default:
                return formatterENDateTimeNoSeconds.print(date.getTime());
        }
    }

    /**
     * FIXME add some more documentation This method is used by the crowdsourcing module
     *
     * @param dateEnd
     * @param locale
     * @return
     */
    public static String formatDate(Date date, Locale locale) {
        if (locale != null) {
            switch (locale.getLanguage()) {
                case "de":
                    return formatterDEDate.print(date.getTime());
                case "en":
                default:
                    return formatterENDate.print(date.getTime());
            }
        }

        return formatterENDate.print(date.getTime());
    }
}
