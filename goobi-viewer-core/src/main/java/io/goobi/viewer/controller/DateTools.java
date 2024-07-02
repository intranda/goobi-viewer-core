/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Utility methods for date parsing, etc.
 */
public final class DateTools {

    private static final Logger logger = LogManager.getLogger(DateTools.class);

    /** Constant <code>formatterISO8601Full</code> */
    public static final DateTimeFormatter FORMATTERISO8601FULL = DateTimeFormatter.ISO_LOCAL_DATE_TIME; // yyyy-MM-dd'T'HH:mm:ss
    /** Constant <code>formatterISO8601DateTimeInstant</code> */
    public static final DateTimeFormatter FORMATTERISO8601DATETIMEINSTANT = DateTimeFormatter.ISO_INSTANT; // yyyy-MM-dd'T'HH:mm:ssZ
    /** Constant <code>formatterISO8601DateTimeWithOffset</code> */
    public static final DateTimeFormatter FORMATTERISO8601DATETIMEWITHOFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME; // yyyy-MM-dd'T'HH:mm:ss+01:00
    /** Constant <code>formatterISO8601Date</code> */
    public static final java.time.format.DateTimeFormatter FORMATTERISO8601DATE = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    /** Constant <code>formatterISO8601Date</code> */
    public static final java.time.format.DateTimeFormatter FORMATTERISO8601TIME = DateTimeFormatter.ISO_LOCAL_TIME; // HH:mm:ss
    /** Constant <code>formatterISO8601DateReverse</code> */
    public static final DateTimeFormatter FORMATTERISO8601DATEREVERSE = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // dd-MM-YYYY
    /** Constant <code>formatterISO8601YearMonth</code> */
    public static final DateTimeFormatter FORMATTERISO8601YEARMONTH = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();
    /** Constant <code>formatterISO8601DateTime</code> */
    public static final DateTimeFormatter FORMATTERISO8601DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** Constant <code>formatterISO8601DateTimeMS</code> */
    public static final DateTimeFormatter FORMATTERISO8601DATETIMEMS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    /** Constant <code>formatterDEDate</code> */
    public static final DateTimeFormatter FORMATTERDEDATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    /** Constant <code>formatterUSDate</code> */
    public static final DateTimeFormatter FORMATTERENDATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    /** Constant <code>formatterCNDate</code> */
    public static final DateTimeFormatter FORMATTERCNDATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    /** Constant <code>formatterJPDate</code> */
    public static final DateTimeFormatter FORMATTERJPDATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    /** Constant <code>formatterISO8601DateTimeNoSeconds</code> */
    public static final DateTimeFormatter FORMATTERISO8601DATETIMENOSECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** Constant <code>formatterDEDateTime</code> */
    public static final DateTimeFormatter FORMATTERDEDATETIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    /** Constant <code>formatterENDateTime</code> */
    public static final DateTimeFormatter FORMATTERENDATETIME = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a").withLocale(Locale.ENGLISH);
    /** Constant <code>formatterDEDateTimeNoSeconds</code> */
    public static final DateTimeFormatter FORMATTERDEDATETIMENOSECONDS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    /** Constant <code>formatterENDateTimeNoSeconds</code> */
    public static final DateTimeFormatter FORMATTERENDATETIMENOSECONDS = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a").withLocale(Locale.ENGLISH);
    /** Constant <code>formatterISO8601BasicDateNoYear</code> */
    public static final DateTimeFormatter FORMATTERISO8601BASICDATENOYEAR = new DateTimeFormatterBuilder()
            .appendPattern("MMdd")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();
    /** Constant <code>formatterISO8601BasicDate</code> */
    public static final DateTimeFormatter FORMATTERISO8601BASICDATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** Constant <code>formatterBasicDateTime</code> */
    public static final DateTimeFormatter FORMATTERISO8601BASICDATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** Constant <code>formatterJavaUtilDateToString</code> */
    public static final DateTimeFormatter FORMATTERJAVAUTILDATETOSTRING = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss zzz yyyy");

    public static final DateTimeFormatter FORMATTERYEARONLY = DateTimeFormatter.ofPattern("yyyy");

    public static final DateTimeFormatter FORMATTERMONTHDAYONLY = DateTimeFormatter.ofPattern("MMdd");

    public static final DateTimeFormatter FORMATTERFILENAME = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmssSSS");

    /**
     * 
     */
    private DateTools() {
        //
    }

    /**
     * Converts the given string to a list of Date objects created from the contents of this string (years or whole dates).
     *
     * @param dateString a {@link java.lang.String} object.
     * @should parse single date correctly
     * @should parse multiple dates correctly
     * @should parse dates in parentheses correctly
     * @return a {@link java.util.List} object.
     */
    public static List<LocalDateTime> parseMultipleDatesFromString(String dateString) {
        // logger.debug("Parsing date string : {}", dateString);
        if (StringUtils.isEmpty(dateString)) {
            return Collections.emptyList();
        }

        List<LocalDateTime> ret = new ArrayList<>();
        String splittingChar = "/";
        String[] dateStringSplit = dateString.split(splittingChar);
        for (final String s : dateStringSplit) {
            String ds = s.trim();

            // Check whether this is a well-formed date and not a range or anything
            LocalDateTime date = parseDateFromString(ds);
            if (date != null) {
                ret.add(date);
                continue;
            }

            // Try finding a complete date in the string (enclosed in parentheses)
            Pattern p = Pattern.compile(StringTools.REGEX_PARENTHESES);
            Matcher m = p.matcher(ds);
            if (m.find()) {
                ds = ds.substring(m.start() + 1, m.end() - 1);
                logger.trace("Extracted date: {}", ds);
                date = parseDateFromString(ds);
                if (date != null) {
                    ret.add(date);
                    continue;
                }
            }

            // If no complete date was found, just use the year
            if (ds.contains(" ")) {
                String[] sSplit = ds.split(" ");
                ds = sSplit[0];
            }
            try {
                int year = Integer.parseInt(ds);
                ret.add(LocalDateTime.of(year, 1, 1, 0, 0));
            } catch (NumberFormatException e) {
                logger.trace("Could not parse year: {}", ds);
            }
        }

        return ret;
    }

    /**
     *
     * @param millis
     * @param utc
     * @return {@link LocalDateTime} built from millis
     * @should create LocalDateTime correctly
     */
    public static LocalDateTime getLocalDateTimeFromMillis(long millis, boolean utc) {
        return Instant.ofEpochMilli(millis).atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     *
     * @param ldt
     * @param utc
     * @return {@link Long} built from ldt
     */
    public static Long getMillisFromLocalDateTime(LocalDateTime ldt, boolean utc) {
        if (ldt == null) {
            return null;
        }

        ZonedDateTime zdt = ldt.atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    /**
     *
     * @param dateString
     * @param fromUTC
     * @return {@link LocalDateTime} parsed from dateString
     * @should parse iso date formats correctly
     * @should parse german date formats correctly
     * @should parse english date formats correctly
     * @should parse chinese date formats correctly
     * @should parse japanese date formats correctly
     * @should return null if unsupported format
     * @should throw IllegalArgumentException if dateString is null
     */
    public static LocalDateTime parseDateTimeFromString(String dateString, boolean fromUTC) {
        return parseDateTimeFromString(dateString, fromUTC, null);
    }

    /**
     * <p>
     * parseDateTimeFromString.
     * </p>
     *
     * @param dateString a {@link java.lang.String} object.
     * @param fromUTC a boolean.
     * @param zoneOffset
     * @return a {@link java.time.LocalDateTime} object.
     * @should parse iso date as UTC correctly
     */
    public static LocalDateTime parseDateTimeFromString(String dateString, boolean fromUTC, Integer zoneOffset) {
        if (dateString == null) {
            throw new IllegalArgumentException("dateString may not be null");
        }

        try {
            if (fromUTC) {
                ZoneId zoneId = zoneOffset != null ? ZoneId.ofOffset("UTC", ZoneOffset.ofHours(zoneOffset)) : ZoneId.systemDefault();
                return LocalDateTime.parse(dateString, FORMATTERISO8601DATETIMEINSTANT)
                        .atZone(zoneId)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();
            }
            return LocalDateTime.parse(dateString, FORMATTERISO8601DATETIMEINSTANT);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, FORMATTERISO8601FULL);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            if (fromUTC) {
                ZoneId zoneId = zoneOffset != null ? ZoneId.ofOffset("UTC", ZoneOffset.ofHours(zoneOffset)) : ZoneId.systemDefault();
                return LocalDateTime.parse(dateString, FORMATTERISO8601DATETIMEWITHOFFSET)
                        .atZone(zoneId)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();
            }
            return LocalDateTime.parse(dateString, FORMATTERISO8601DATETIMEWITHOFFSET);

        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, FORMATTERISO8601DATETIMEMS);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, FORMATTERISO8601DATETIME);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, FORMATTERISO8601DATE).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, FORMATTERISO8601YEARMONTH).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, FORMATTERDEDATETIME);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDateTime.parse(dateString, FORMATTERENDATETIME);
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, FORMATTERDEDATE).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, FORMATTERENDATE).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, FORMATTERJPDATE).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }
        try {
            return LocalDate.parse(dateString, FORMATTERCNDATE).atStartOfDay();
        } catch (DateTimeParseException e) {
            //
        }

        return null;
    }

    /**
     * <p>
     * parseDateFromString.
     * </p>
     *
     * @param dateString a {@link java.lang.String} object.
     * @return a {@link java.util.Date} object.
     */
    public static LocalDateTime parseDateFromString(String dateString) {
        return parseDateTimeFromString(dateString, false);
    }

    /**
     *
     * @param dateToConvert
     * @param utc
     * @return {@link Date} converted from dateToConvert
     */
    public static Date convertLocalDateTimeToDateViaInstant(LocalDateTime dateToConvert, boolean utc) {
        if (dateToConvert == null) {
            return null;
        }
        return Date.from(dateToConvert.atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault()).toInstant());
    }

    /**
     *
     * @param dateToConvert
     * @return {@link LocalDateTime} converted form dateToConvert
     */
    public static LocalDateTime convertDateToLocalDateTimeViaInstant(Date dateToConvert) {
        if (dateToConvert == null) {
            return null;
        }
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     *
     * @param date java.util.Date
     * @param formatter
     * @param utc
     * @return Formatted {@link Date} as {@link String}
     */
    public static String format(Date date, DateTimeFormatter formatter, boolean utc) {
        if (date == null) {
            throw new IllegalArgumentException("date may not be null");
        }

        ZonedDateTime ld =
                convertDateToLocalDateTimeViaInstant(date).atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault());
        return ld.format(formatter);
    }

    /**
     *
     * @param localDateTime
     * @param formatter
     * @param utc
     * @return Formatted {@link LocalDateTime} as {@link String}
     */
    public static String format(LocalDateTime localDateTime, DateTimeFormatter formatter, boolean utc) {
        if (localDateTime == null) {
            throw new IllegalArgumentException("localDateTime may not be null");
        }

        ZonedDateTime ld =
                localDateTime.atZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault());
        return ld.format(formatter);
    }

    /**
     * Returns the string representation of the given <code>Date</code> based on the given ISO 639-1 language code.
     *
     * @param date LocalDateTime to format.
     * @param language ISO 639-1 (two-character) language code.
     * @should format date correctly for the given language
     * @should use English format for unknown languages
     * @return a {@link java.lang.String} object.
     */
    public static String getLocalDate(LocalDateTime date, String language) {
        if (language == null) {
            return format(date, FORMATTERENDATETIMENOSECONDS, false);
        }

        if ("de".equals(language)) {
            return format(date, FORMATTERDEDATETIMENOSECONDS, false);
        }

        return format(date, FORMATTERENDATETIMENOSECONDS, false);
    }

    /**
     * Converts the given <code>LocalDateTime</code> to a locale-based string format. This method is used by the crowdsourcing module.
     *
     * @param ldt a {@link java.time.LocalDateTime} object.
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
     */
    public static String formatDate(LocalDateTime ldt, Locale locale) {
        if (ldt == null) {
            return null;
        }
        if (locale == null) {
            return format(ldt, FORMATTERENDATE, false);
        }

        switch (locale.getLanguage()) {
            case "de":
                return format(ldt, FORMATTERDEDATE, false);
            case "en":
            default:
                return format(ldt, FORMATTERENDATE, false);
        }
    }
}
