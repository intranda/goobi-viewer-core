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
package io.goobi.viewer.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.MalformedInputException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.managedbeans.utils.BeanUtils;

public class StringTools {

    private static final Logger logger = LoggerFactory.getLogger(StringTools.class);

    public static String encodeUrl(String string) {
        try {
            //            return BeanUtils.escapeCriticalUrlChracters(string);
            return URLEncoder.encode(string, Helper.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to encode '{}' with {}", string, Helper.DEFAULT_ENCODING);
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

        return s.replace("\r\n", replaceWith)
                .replace("\n", replaceWith)
                .replaceAll("\r", replaceWith)
                .replaceAll("<br>", replaceWith)
                .replaceAll("<br\\s*/>", replaceWith);
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

    /**
     * Return the length of the given string, or 0 if the string is null
     * 
     * @param s
     * @return the length of the string if it exists, 0 otherwise
     */
    public static int getLength(String s) {
        if (StringUtils.isEmpty(s)) {
            return 0;
        }
        return s.length();
    }

    /**
     * Escapes the given string using {@link StringEscapeUtils#escapeHtml4(String)} and additionally converts all line breaks (\r\n, \r, \n) to html
     * line breaks ({@code <br/>
     * })
     * 
     * @param text the text to escape
     * @return the escaped string
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        text = StringEscapeUtils.escapeHtml4(text);
        text = text.replace("\r\n", "<br/>").replace("\r", "<br/>").replace("\n", "<br/>");
        return text;
    }

    /**
     * 
     * @param s
     * @return
     */
    public static String escapeQuotes(String s) {
        if (s != null) {
            s = s.replaceAll("(?<!\\\\)'", "\\\\'");
            s = s.replaceAll("(?<!\\\\)\"", "\\\\\"");
        }
        return s;
    }

    /**
     * Converts a <code>String</code> from one given encoding to the other.
     * 
     * @param string The string to convert.
     * @param from Source encoding.
     * @param to Destination encoding.
     * @return The converted string.
     */
    public static String convertStringEncoding(String string, String from, String to) {
        try {
            Charset charsetFrom = Charset.forName(from);
            Charset charsetTo = Charset.forName(to);
            CharsetEncoder encoder = charsetFrom.newEncoder();
            CharsetDecoder decoder = charsetTo.newDecoder();
            // decoder.onMalformedInput(CodingErrorAction.IGNORE);
            ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(string));
            CharBuffer cbuf = decoder.decode(bbuf);
            return cbuf.toString();
        } catch (MalformedInputException e) {
            logger.warn(e.getMessage());
        } catch (CharacterCodingException e) {
            logger.error(e.getMessage(), e);
        }

        return string;
    }

    /**
     * 
     * @param url
     * @return true if this is an image URL; false otherwise
     * @should return true for image urls
     */
    public static boolean isImageUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return false;
        }

        String extension = FilenameUtils.getExtension(url);
        if (StringUtils.isEmpty(extension)) {
            return false;
        }

        switch (extension.toLowerCase()) {
            case "tif":
            case "tiff":
            case "jpg":
            case "jpeg":
            case "gif":
            case "png":
            case "jp2":
                return true;
            default:
                return false;
        }
    }

    /**
     * Renames CSS classes that start with digits in the given html code due to Chrome ignoring such classes.
     * 
     * @param html The HTML to fix
     * @return Same HTML document but with Chrome-compatible CSS class names
     * @should rename classes correctly
     */
    public static String renameIncompatibleCSSClasses(String html) {
        if (html == null) {
            return null;
        }

        Pattern p = Pattern.compile("\\.([0-9]+[A-Za-z]+) \\{.*\\}");
        Matcher m = p.matcher(html);
        Map<String, String> replacements = new HashMap<>();
        // Collect bad class names
        while (m.find()) {
            if (m.groupCount() > 0) {
                String oldName = m.group(1);
                StringBuilder sbMain = new StringBuilder();
                StringBuilder sbNum = new StringBuilder();
                for (char c : oldName.toCharArray()) {
                    if (Character.isDigit(c)) {
                        sbNum.append(c);
                    } else {
                        sbMain.append(c);
                    }
                }
                replacements.put(oldName, sbMain.toString() + sbNum.toString());
            }
        }
        // Replace in HTML
        if (!replacements.isEmpty()) {
            for (String key : replacements.keySet()) {
                html = html.replace(key, replacements.get(key));
            }
        }

        return html;
    }

    /**
     * 
     * @param collection
     * @param split
     * @return List of string containing every (sub-)collection name
     * @should create list correctly
     * @should return single value correctly
     */
    public static List<String> getHierarchyForCollection(String collection, String split) {
        if (StringUtils.isEmpty(collection) || StringUtils.isEmpty(split)) {
            return Collections.emptyList();
        }

        String useSplit = '[' + split + ']';
        String[] hierarchy = collection.contains(split) ? collection.split(useSplit) : new String[] { collection };
        List<String> ret = new ArrayList<>(hierarchy.length);
        StringBuilder sb = new StringBuilder();
        for (String level : hierarchy) {
            if (sb.length() > 0) {
                sb.append(split);
            }
            sb.append(level);
            ret.add(sb.toString());
        }

        return ret;
    }
}
