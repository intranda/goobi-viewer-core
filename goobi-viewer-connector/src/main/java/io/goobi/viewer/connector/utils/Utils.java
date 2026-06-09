/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.utils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import io.goobi.viewer.connector.Version;
import io.goobi.viewer.connector.oai.RequestHandler;

/**
 * <p>
 * Utils class.
 * </p>
 *
 */
public final class Utils {

    private static final Logger logger = LogManager.getLogger(Utils.class);

    private static final int HTTP_TIMEOUT = 10000;

    /** Constant <code>formatterISO8601DateTimeWithOffset</code> */
    public static final DateTimeFormatter FORMATTER_ISO8601_DATETIME_WITH_OFFSET =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME; // yyyy-MM-dd'T'HH:mm:ss+01:00
    /** Constant <code>formatterISO8601Date</code> */
    public static final DateTimeFormatter FORMATTER_ISO8601_DATE = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    /** Constant <code>formatterISO8601Date</code> */
    public static final DateTimeFormatter FORMATTER_ISO8601_TIME = DateTimeFormatter.ISO_LOCAL_TIME; // HH:mm:ss
    /** Constant <code>formatterBasicDateTime</code> */
    public static final DateTimeFormatter FORMATTER_ISO8601_BASIC_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** Constant <code>formatterISO8601DateTimeNoSeconds</code> */
    public static final DateTimeFormatter FORMATTER_ISO8601_DATETIME_NO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Utils() {
    }

    /**
     * Insert some chars in the time string.
     *
     * @param ldt LocalDateTime to use
     * @return the time in the format YYYY-MM-DDThh:mm:ssZ
     * @should format time correctly
     * @should truncate to seconds
     */
    public static String getCurrentUTCTime(LocalDateTime ldt) {
        if (ldt == null) {
            throw new IllegalArgumentException("ldt may not be null");
        }
        return ldt.atOffset(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS)
                .format(FORMATTER_ISO8601_DATETIME_WITH_OFFSET);
    }

    /**
     * <p>
     * convertDate.
     * </p>
     *
     * @param milliSeconds a long.
     * @return a {@link java.lang.String} object.
     * @should convert time correctly
     */
    public static String convertDate(long milliSeconds) {
        return Instant.ofEpochMilli(milliSeconds)
                .atOffset(ZoneOffset.UTC)
                .format(FORMATTER_ISO8601_DATETIME_WITH_OFFSET);
    }

    /**
     * <p>
     * getHttpResponseStatus.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @return a int.
     * @throws java.lang.UnsupportedOperationException if any.
     * @throws java.io.IOException if any.
     */
    public static int getHttpResponseStatus(String url) throws UnsupportedOperationException, IOException {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
            HttpGet get = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                return response.getStatusLine().getStatusCode();
            }
        }
    }

    /**
     * Returns the application version number.
     *
     * @return a {@link java.lang.String} object.
     */
    public static String getVersion() {
        return Version.asJSON();
    }

    /**
     * <p>
     * splitIdentifierAndLanguageCode.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @param languageCodeLength a int.
     * @return an array of {@link java.lang.String} objects.
     * @should split identifier correctly
     */
    public static String[] splitIdentifierAndLanguageCode(String identifier, int languageCodeLength) {
        if (identifier == null) {
            throw new IllegalArgumentException("identifer may not be null");
        }
        if (languageCodeLength < 1) {
            throw new IllegalArgumentException("splitIndex must be 1 or larger");
        }

        String[] ret = new String[2];

        if (identifier.contains("_")) {
            int splitIndex = identifier.length() - languageCodeLength;
            ret[0] = identifier.substring(0, splitIndex - 1);
            ret[1] = identifier.substring(splitIndex);
        } else {
            ret[0] = identifier;
        }

        return ret;
    }

    /**
     * <p>
     * parseDate.
     * </p>
     *
     * @param datestring a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
     * @should parse dates correctly
     */
    public static String parseDate(Object datestring) {
        if (datestring instanceof Long l) {
            return convertDate(l);
        }
        return "";
    }

    /**
     * Returns a parameter map built from {@link RequestHandler} values.
     *
     * @param requestHandler The request that was send to the server(servlet)
     * @return a HashMap with the values from, until and set as string
     * @should contain from timestamp
     * @should contain until timestamp
     * @should contain set
     * @should contain metadataPrefix
     * @should contain verb
     */
    public static Map<String, String> filterDatestampFromRequest(RequestHandler requestHandler) {
        Map<String, String> datestamp = new HashMap<>();

        String from = null;
        if (requestHandler.getFrom() != null) {
            from = requestHandler.getFrom();
            from = cleanUpTimestamp(from);
            datestamp.put("from", from);
        }
        String until = null;
        if (requestHandler.getUntil() != null) {
            until = requestHandler.getUntil();
            until = cleanUpTimestamp(until);
            datestamp.put("until", until);
        }

        String set = null;
        if (requestHandler.getSet() != null) {
            set = requestHandler.getSet();
            datestamp.put("set", set);
        }

        if (requestHandler.getMetadataPrefix() != null) {
            datestamp.put("metadataPrefix", requestHandler.getMetadataPrefix().getMetadataPrefix());
        }

        if (requestHandler.getVerb() != null) {
            datestamp.put("verb", requestHandler.getVerb().getTitle());
        }

        return datestamp;
    }

    /**
     * 
     * @param timestamp
     * @return {@link String}
     * @should clean up timestamp correctly
     */
    public static String cleanUpTimestamp(final String timestamp) {
        if (StringUtils.isEmpty(timestamp)) {
            return timestamp;
        }

        // Remove milliseconds
        String useTimeStamp = timestamp;
        if (useTimeStamp.contains(".")) {
            useTimeStamp = useTimeStamp.substring(0, useTimeStamp.indexOf("."));
        }

        return useTimeStamp.replace("-", "").replace("T", "").replace(":", "").replace("Z", "");
    }

    /**
     * 
     * @param json JSON string
     * @return Version information as a single line string
     * @should format string correctly
     */
    public static String formatVersionString(String json) {
        if (StringUtils.isEmpty(json)) {
            return "NOT AVAILABLE";
        }

        JSONObject jsonObj = new JSONObject(json);
        try {
            return jsonObj.getString("application") + " " + jsonObj.getString("version")
                    + " " + jsonObj.getString("build-date")
                    + " " + jsonObj.getString("git-revision");
        } catch (JSONException e) {
            logger.error(e.getMessage());
            return "NOT AVAILABLE";
        }
    }
}
