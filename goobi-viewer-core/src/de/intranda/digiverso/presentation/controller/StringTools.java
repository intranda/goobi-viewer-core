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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

public class StringTools {

    private static final Logger logger = LoggerFactory.getLogger(StringTools.class);

    public static String encodeUrl(String string) {
        try {
            //            return BeanUtils.escapeCriticalUrlChracters(string);
            return URLEncoder.encode(string, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to encode '" + string + "' with utf-8");
            return string;
        }
    }

    public static String decodeUrl(String string) {
        //    string = string.replace("%", "\\u");
        String encodedString = string;
        try {
            do {
                string = encodedString;
                encodedString = URLDecoder.decode(string, "utf-8");
            } while (!encodedString.equals(string));
            return BeanUtils.unescapeCriticalUrlChracters(string);
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }

    /**
     * Finds the first String matching a regex within another string and return it as an {@link Optional}
     * 
     * @param text The String in which to search
     * @param regex The regex to search for
     * @return An optional containing the first String within the {@code text} matched by {@code regex}, or an empty optional if no match was found
     */
    public static Optional<String> findFirstMatch(String text, String regex, int group) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group(group));
        }

        return Optional.empty();
    }

    /**
     * Escapes special HTML characters in the given string.
     *
     * @param str
     * @return
     * @should escape all characters correctly
     */
    public static String escapeHtmlChars(String str) {
        return StringUtils.replaceEach(str, new String[] { "&", "\"", "<", ">" }, new String[] { "&amp;", "&quot;", "&lt;", "&gt;" });
    }

    /**
     * Removed diacritical marks from each letter in the given String.
     *
     * @param s
     * @return String without diacritical marks
     * @should remove diacritical marks correctly
     */
    public static String removeDiacriticalMarks(String s) {
        if (s == null) {
            throw new IllegalArgumentException("s may not be null");
        }

        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+", "");
    }

    /**
     * Removes regular and HTML line breaks from the given String.
     * 
     * @param s
     * @return String without line breaks
     * @should remove line breaks correctly
     * @should remove html line breaks correctly
     */
    public static String removeLineBreaks(String s, String replaceWith) {
        if (s == null) {
            throw new IllegalArgumentException("s may not be null");
        }

        if (replaceWith == null) {
            replaceWith = "";
        }

        return s.replace("\r\n", replaceWith).replace("\n", replaceWith).replaceAll("\r", replaceWith).replaceAll("<br>", replaceWith).replaceAll(
                "<br\\s*/>", replaceWith);
    }

    /**
     * 
     * @param s String to clean
     * @return String sans any script-tag blocks
     * @should remove JS blocks correctly
     */
    public static String stripJS(String s) {
        if (StringUtils.isBlank(s)) {
            return s;
        }

        return s.replaceAll("(?i)<script[\\s\\S]*<\\/script>", "");
    }
}
