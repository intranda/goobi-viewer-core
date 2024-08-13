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
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * <p>
 * StringTools class.
 * </p>
 */
public final class StringTools {

    private static final Logger logger = LogManager.getLogger(StringTools.class);

    /** Constant <code>REGEX_QUOTATION_MARKS="\"[^()]*?\""</code>. */
    public static final String REGEX_QUOTATION_MARKS = "\"[^()]*?\"";
    /** Constant <code>REGEX_PARENTHESES="\\([^()]*\\)"</code>. */
    public static final String REGEX_PARENTHESES = "\\([^()]*\\)";
    /** Constant <code>REGEX_PARENTESES_DATES="\\([\\w|\\s|\\-|\\.|\\?]+\\)"</code>. */
    public static final String REGEX_PARENTESES_DATES = "\\([\\w|\\s|\\-|\\.|\\?]+\\)";
    /** Constant <code>REGEX_BRACES="\\{(\\w+)\\}"</code>. */
    public static final String REGEX_BRACES = "\\{(\\w+)\\}";
    /** Constant <code>REGEX_WORDS="[a-zäáàâöóòôüúùûëéèêßñ0123456789]+"</code>. */
    public static final String REGEX_WORDS = "[\\wäáàâöóòôüúùûëéèêßñ]+";
    /** Constant <code>DEFAULT_ENCODING="UTF-8"</code>. */
    public static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();

    /** Constant <code>SLASH_REPLACEMENT="U002F"</code>. */
    public static final String SLASH_REPLACEMENT = "U002F";
    /** Constant <code>BACKSLASH_REPLACEMENT="U005C"</code>. */
    public static final String BACKSLASH_REPLACEMENT = "U005C";
    /** Constant <code>PIPE_REPLACEMENT="U007C"</code>. */
    public static final String PIPE_REPLACEMENT = "U007C";
    /** Constant <code>QUESTION_MARK_REPLACEMENT="U003F"</code>. */
    public static final String QUESTION_MARK_REPLACEMENT = "U003F";
    /** Constant <code>PERCENT_REPLACEMENT="U0025"</code>. */
    public static final String PERCENT_REPLACEMENT = "U0025";
    /** Constant <code>PLUS_REPLACEMENT="U0025"</code>. */
    public static final String PLUS_REPLACEMENT = "U002B";

    /**
     * Private construct to prevent instantiation.
     */
    private StringTools() {
        //
    }

    /**
     * Escape url for submitted form data. A space is encoded as '+'.
     *
     * @param string String to encode
     * @return URL-encoded string
     */
    public static String encodeUrl(String string) {
        return encodeUrl(string, false);
    }

    /**
     * <p>
     * encodeUrl.
     * </p>
     *
     * @param string String to encode
     * @param escapeCriticalUrlCharacters If true, slashes etc. will be manually escaped prior to URL encoding
     * @return URL-encoded string
     */
    public static String encodeUrl(final String string, boolean escapeCriticalUrlCharacters) {
        if (StringUtils.isEmpty(string)) {
            return string;
        }

        String ret = string;
        if (escapeCriticalUrlCharacters) {
            ret = BeanUtils.escapeCriticalUrlChracters(ret);
        }
        try {
            return URLEncoder.encode(ret, StringTools.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to encode '{}' with {}", ret, StringTools.DEFAULT_ENCODING);
            return ret;
        }
    }

    /**
     * <p>
     * decodeUrl.
     * </p>
     *
     * @param string a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String decodeUrl(final String string) {
        if (string == null) {
            return string;
        }

        String encodedString = string;
        String ret = null;
        try {
            do {
                ret = encodedString;
                encodedString = URLDecoder.decode(ret, "utf-8");
            } while (!encodedString.equals(ret));
            return unescapeCriticalUrlChracters(ret);
        } catch (NullPointerException | UnsupportedEncodingException e) {
            return ret;
        }
    }

    /**
     * Finds the first String matching a regex within another string and return it as an {@link java.util.Optional}.
     *
     * @param text The String in which to search
     * @param regex The regex to search for
     * @return An optional containing the first String within the {@code text} matched by {@code regex}, or an empty optional if no match was found
     * @param group a int.
     */
    public static Optional<String> findFirstMatch(String text, String regex, int group) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group(group));
        }

        return Optional.empty();
    }

    /**
     *
     * @param s {@link String} to match
     * @param candidates List of {@link String}s with possible matches
     * @param language Language for scoring; English will be used, if no {@link Locale} can be matched for given language
     * @return Best matching string; null if none found
     * @should throw IllegalArgumentException if s is null
     * @should throw IllegalArgumentException if candidates is null
     * @should return best match
     * @should return null if no matches found
     */
    public static String findBestMatch(String s, List<String> candidates, String language) {
        if (s == null) {
            throw new IllegalArgumentException("s may not be null");
        }
        if (candidates == null) {
            throw new IllegalArgumentException("candidates may not be null");
        }

        FuzzyScore fs = new FuzzyScore(StringUtils.isNotEmpty(language) ? Locale.forLanguageTag(language) : Locale.ENGLISH);
        String ret = null;
        int topScore = 0;
        for (String c : candidates) {
            int score = fs.fuzzyScore(s, c);
            if (score > topScore) {
                topScore = score;
                ret = c;
            }
        }
        logger.trace("best match for {}: {}", s, ret);

        return ret;
    }

    /**
     * Escapes special HTML characters in the given string.
     *
     * @param str a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @should escape all characters correctly
     */
    public static String escapeHtmlChars(String str) {
        return replaceCharacters(str, new String[] { "&", "\"", "<", ">" }, new String[] { "&amp;", "&quot;", "&lt;", "&gt;" });
    }

    /**
     * Escapes &lt;&gt; in the given string.
     *
     * @param str a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String escapeHtmlLtGt(String str) {
        return replaceCharacters(str, new String[] { "<", ">" }, new String[] { "&lt;", "&gt;" });
    }

    /**
     * Replaces characters in included in <code>replace</code> with characters in <code>replaceWith</code> in the given string.
     *
     * @param str
     * @param replace
     * @param replaceWith
     * @return String with replaced characters.
     * @should replace characters correctly
     */
    static String replaceCharacters(String str, String[] replace, String[] replaceWith) {
        if (str == null) {
            return null;
        }
        if (replace == null) {
            throw new IllegalArgumentException("replace may not be null");
        }
        if (replaceWith == null) {
            throw new IllegalArgumentException("replaceWith may not be null");
        }

        return StringUtils.replaceEach(str, replace, replaceWith);
    }

    /**
     * Removed diacritical marks from each letter in the given String.
     *
     * @param s a {@link java.lang.String} object.
     * @return String without diacritical marks
     * @should remove diacritical marks correctly
     */
    public static String removeDiacriticalMarks(String s) {
        if (s == null) {
            throw new IllegalArgumentException("s may not be null");
        }

        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+", "");

    }

    public static String replaceCharacterVariants(String text) {
        return text.replace("ſ", "s")
                .replace("ø", "o")
                .replace("Ø", "O")
                .replace("Ł", "L")
                .replace("ł", "l")
                .replace("Ð", "D")
                .replace("ð", "d");

    }

    /**
     * Removes regular and HTML line breaks from the given String.
     *
     * @param s a {@link java.lang.String} object.
     * @param replaceWith a {@link java.lang.String} object.
     * @return String without line breaks
     * @should remove line breaks correctly
     * @should remove html line breaks correctly
     */
    public static String removeLineBreaks(String s, final String replaceWith) {
        if (s == null) {
            throw new IllegalArgumentException("s may not be null");
        }

        String replacement = replaceWith;
        if (replacement == null) {
            replacement = "";
        }

        return s.replace("\r\n", replacement)
                .replace("\n", replacement)
                .replace("\r", replacement)
                .replace("<br>", replacement)
                .replaceAll("<br\\s*/>", replacement);
    }

    /**
     * <p>
     * stripJS.
     * </p>
     *
     * @param s
     * @return String sans any script-tag blocks
     * @should remove JS blocks correctly
     */
    public static String stripJS(String s) {
        if (StringUtils.isBlank(s)) {
            return s;
        }

        return s.replaceAll("(?i)<script[\\s\\S]*<\\/script>", "")
                .replaceAll("(?i)<script[\\s\\S]*\\/?>", "")
                .replaceAll("(?i)<svg[\\s\\w()\"=]*\\/?>", "")
                .replaceAll("(?i)<\\/svg>", "");
    }

    /**
     * Use this method to log user-controller variables that may contain pattern-breaking characters such as line breaks and tabs.
     *
     * @param s String to clean
     * @return String sans any logger pattern-breaking characters
     * @should remove chars correctly
     */
    public static String stripPatternBreakingChars(String s) {
        if (StringUtils.isBlank(s)) {
            return s;
        }

        return s.replaceAll("[\n\r\t]", "_");
    }

    /**
     * Return the length of the given string, or 0 if the string is null.
     *
     * @param s a {@link java.lang.String} object.
     * @return the length of the string if it exists, 0 otherwise
     */
    public static int getLength(String s) {
        if (StringUtils.isEmpty(s)) {
            return 0;
        }
        return s.length();
    }

    /**
     * Escapes the given string. Uses {@link org.apache.commons.lang3.StringEscapeUtils#escapeHtml4(String)} and additionally converts all line breaks
     * (\r\n, \r, \n) to html line breaks ({@code <br/>
     *  })
     *
     * @param text the text to escape
     * @return the escaped string
     */
    public static String escapeHtml(final String text) {
        if (text == null) {
            return null;
        }

        String ret = StringEscapeUtils.escapeHtml4(text);
        ret = ret.replace("\r\n", StringConstants.HTML_BR).replace("\r", StringConstants.HTML_BR).replace("\n", StringConstants.HTML_BR);
        return ret;
    }

    /**
     * <p>
     * escapeQuotes.
     * </p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String escapeQuotes(final String s) {
        String ret = s;
        if (ret != null) {
            ret = ret.replaceAll("(?<!\\\\)'", "\\\\'");
            ret = ret.replaceAll("(?<!\\\\)\"", "\\\\\"");
        }
        return ret;
    }

    /**
     *
     * @param input
     * @return Charset of the given input
     */
    public static String getCharset(String input) {
        CharsetDetector cd = new CharsetDetector();
        cd.setText(input.getBytes());
        CharsetMatch cm = cd.detect();
        if (cm != null) {
            return cm.getName();
        }

        return null;
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
     * Checks whether given string already contains URL-encoded characters.
     *
     * @param s String to check
     * @param charset Charset for URL decoding
     * @return true if decoded string differs from original; false otherwise
     * @throws UnsupportedEncodingException
     * @should return true if string contains url encoded characters
     * @should return false if string not encoded
     *
     */
    public static boolean isStringUrlEncoded(String s, String charset) throws UnsupportedEncodingException {
        if (StringUtils.isEmpty(s)) {
            return false;
        }

        String decoded = URLDecoder.decode(s, charset);
        return !s.equals(decoded);
    }

    /**
     * <p>
     * escapeCriticalUrlChracters.
     * </p>
     *
     * @param value a {@link java.lang.String} object.
     * @param escapePercentCharacters a boolean.
     * @return a {@link java.lang.String} object.
     * @should replace characters correctly
     */
    public static String escapeCriticalUrlChracters(final String value, boolean escapePercentCharacters) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        String ret = value.replace("/", SLASH_REPLACEMENT)
                .replace("\\", BACKSLASH_REPLACEMENT)
                .replace("|", PIPE_REPLACEMENT)
                .replace("%7C", PIPE_REPLACEMENT)
                .replace("?", QUESTION_MARK_REPLACEMENT)
                .replace("+", PLUS_REPLACEMENT);
        if (escapePercentCharacters) {
            ret = ret.replace("%", PERCENT_REPLACEMENT);
        }
        return ret;
    }

    /**
     * <p>
     * unescapeCriticalUrlChracters.
     * </p>
     *
     * @param value a {@link java.lang.String} object.
     * @should replace characters correctly
     * @return a {@link java.lang.String} object.
     */
    public static String unescapeCriticalUrlChracters(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        return value.replace(SLASH_REPLACEMENT, "/")
                .replace(BACKSLASH_REPLACEMENT, "\\")
                .replace(PIPE_REPLACEMENT, "|")
                .replace(QUESTION_MARK_REPLACEMENT, "?")
                .replace(PERCENT_REPLACEMENT, "%")
                .replace(PLUS_REPLACEMENT, "+");
    }

    /**
     * <p>
     * isImageUrl.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
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
    public static String renameIncompatibleCSSClasses(final String html) {
        if (html == null) {
            return null;
        }

        Pattern p = Pattern.compile("\\.(\\d+[A-Za-z]+) \\{.*\\}");
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
        String ret = html;
        if (!replacements.isEmpty()) {
            for (Entry<String, String> entry : replacements.entrySet()) {
                ret = ret.replace(entry.getKey(), entry.getValue());
            }
        }

        return ret;
    }

    /**
     * <p>
     * getHierarchyForCollection.
     * </p>
     *
     * @param collection a {@link java.lang.String} object.
     * @param split a {@link java.lang.String} object.
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

    /**
     * Normalizes WebAnnotation coordinates for rectangle rendering (x,y,w,h -&gt; minX,minY,maxX,maxY).
     *
     * @param coords a {@link java.lang.String} object.
     * @return Legacy format coordinates
     * @should normalize coordinates correctly
     * @should preserve legacy coordinates
     */
    public static String normalizeWebAnnotationCoordinates(String coords) {
        if (coords == null) {
            return null;
        }
        if (!coords.startsWith("xywh=")) {
            return coords;
        }

        // Normalize WebAnnotation coordinates (x,y,w,h -> minX,minY,maxX,maxY)
        String ret = coords.substring(5);
        String[] rectSplit = ret.split(",");
        if (rectSplit.length == 4) {
            ret = rectSplit[0].trim() + ", " + rectSplit[1].trim() + ", " + (Integer.parseInt(rectSplit[0].trim())
                    + Integer.parseInt(rectSplit[2].trim()) + ", " + (Integer.parseInt(rectSplit[1].trim()) + Integer.parseInt(rectSplit[3].trim())));
        }

        return ret;
    }

    /**
     * <p>
     * getMatch.
     * </p>
     *
     * @param s a {@link java.lang.String} object.
     * @param pattern a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMatch(String s, String pattern) {
        if (StringUtils.isBlank(s)) {
            return "";
        }
        Matcher matcher = Pattern.compile(pattern).matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * <p>
     * intern.
     * </p>
     *
     * @param string a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String intern(String string) {
        if (string == null) {
            return null;
        }
        return string.intern();
    }

    /**
     * Creates an hash of the given String using SHA-256.
     *
     * @param myString a {@link java.lang.String} object.
     * @return generated hash
     * @should hash string correctly
     */
    public static String generateHash(String myString) {
        String answer = "";
        try {
            byte[] defaultBytes = myString.getBytes(StandardCharsets.UTF_8.name());
            MessageDigest algorithm = MessageDigest.getInstance("SHA-256");
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte[] messageDigest = algorithm.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte element : messageDigest) {
                String hex = Integer.toHexString(0xFF & element);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            answer = hexString.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }

        return answer;
    }

    /**
     * @param path
     * @return Given path with a trailing slash, if not yet present
     */
    public static String appendTrailingSlash(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        } else if (path.endsWith("/") || path.endsWith("\\")) {
            return path;
        } else {
            return path + "/";
        }
    }
    
    public static String removeTrailingSlashes(String path) {
        if (path != null && (path.endsWith("/") || path.endsWith("\\"))) {
            return removeTrailingSlashes(path.substring(0, path.length() - 1));
        }
        return path;
    }

    /**
     *
     * @param value
     * @return true if value null, empty or starts with 0x1; false otherwise
     * @should return true if value null or empty
     * @should return true if value starts with 0x1
     * @should return true if value starts with #1;
     * @should return false otherwise
     */
    public static boolean checkValueEmptyOrInverted(String value) {
        if (StringUtils.isEmpty(value)) {
            return true;
        }

        return value.charAt(0) == 0x01 || value.startsWith("#1;");
    }

    /**
     *
     * @param values All values to check
     * @param regex
     * @return List of values that match <code>regex</code>
     * @should return all matching values
     */
    public static List<String> filterStringsViaRegex(List<String> values, String regex) {
        if (values == null || values.isEmpty() || StringUtils.isEmpty(regex)) {
            return Collections.emptyList();
        }

        List<String> ret = new ArrayList<>();
        Pattern p = Pattern.compile(regex);
        for (String key : values) {
            Matcher m = p.matcher(key);
            if (m.find()) {
                ret.add(key);
            }
        }

        return ret;
    }

    /**
     * Try to parse the given string as integer.
     *
     * @param s the string to parse
     * @return An Optional containing the parsed int. If the string is blank or cannot be parsed to an integer, an empty Optional is returned
     */
    public static Optional<Integer> parseInt(String s) {
        if (StringUtils.isBlank(s)) {
            return Optional.empty();
        }

        try {
            int i = Integer.parseInt(s);
            return Optional.of(i);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static int[] getIntegerRange(final String inRange) {
        int page;
        int page2 = Integer.MAX_VALUE;
        String range = inRange;
        if (range.contains("-")) {
            boolean firstMinus = false;
            boolean secondMinus = false;
            if (range.startsWith("-")) {
                firstMinus = true;
                range = range.substring(1);
            }
            if (range.contains("-")) {
                if (range.contains("--")) {
                    secondMinus = true;
                    range = range.replace("--", "-");
                }
                String[] split = range.split("-");
                page = Integer.valueOf(split[0]);
                page2 = Integer.valueOf(split[1]);
                if (firstMinus) {
                    page *= -1;
                }
                if (secondMinus) {
                    page2 *= -1;
                }
            } else {
                page = Integer.valueOf(range);
                if (firstMinus) {
                    page *= -1;
                }
            }
        } else {
            page = Integer.valueOf(range);
        }
        return new int[] { page, page2 };
    }

    /**
     * Clean a String from any malicious content like script tags, line breaks and backtracking filepaths.
     *
     * @param data
     * @return a cleaned up string which can be savely used
     */
    public static String cleanUserGeneratedData(String data) {
        String cleaned = stripJS(data);
        cleaned = stripPatternBreakingChars(cleaned);
        cleaned = Paths.get(cleaned).getFileName().toString();
        return cleaned;
    }

    public static int sortByList(String v1, String v2, List<String> sorting) {
        int i1 = sorting.indexOf(v1);
        int i2 = sorting.indexOf(v2);
        if (i1 > -1 && i2 > -1) {
            return i1 - i2;
        } else if (i1 > -1) {
            return -1;
        } else if (i2 > -1) {
            return 1;
        } else {
            return v1.compareTo(v2);
        }
    }

    public static String convertToSingleWord(String text, int maxLength, String whitespaceReplacement) {
        String replaced = Optional.ofNullable(text)
                .orElse("")
                .replaceAll("\\s", whitespaceReplacement)
                .replaceAll("[^a-zA-Z0-9" + whitespaceReplacement + "]", "");
        if (replaced.length() > maxLength) {
            return replaced.substring(0, maxLength);
        }
        return replaced;
    }

    public static String replaceAllMatches(String string, String matchRegex, Function<List<String>, String> replacer) {
        Matcher matcher = Pattern.compile(matchRegex).matcher(string);
        StringBuffer buffer = new StringBuffer(string);
        List<MatchResult> results = matcher.results().collect(Collectors.toList());
        Collections.reverse(results);
        results.forEach(result -> {
            String s = result.group();
            String s1 = result.group(0);
            int groupCount = result.groupCount();
            List<String> groups = IntStream.range(0, result.groupCount() + 1).mapToObj(i -> result.group(i)).collect(Collectors.toList());
            String replacement = replacer.apply(groups);
            buffer.replace(result.start(), result.end(), replacement);
        });
        return buffer.toString();
    }
}
