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

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Tools for parsing html
 *
 * @author florian
 *
 */
public final class HtmlParser {

    private static final String HTML_TAG_PATTERN_STRING = "(</\\w+>)|(<br\\s*/>)";
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(HTML_TAG_PATTERN_STRING);

    private static final Set<String> DISALLOWED_HTML_TAGS = Set.of("<script>");
    private static final Map<String, String> HTML_REPLACEMENTS = Map.ofEntries(new SimpleEntry<>("<br\\s?>", "<br />"));

    /**
     * Private constructor.
     */
    private HtmlParser() {
        //
    }

    /**
     * Guess if the given text should be interpreted as html based in the existence of closing html tags or empty line break tags
     *
     * @param text
     * @return true if text is HTML content; false otherwise
     */
    public static boolean isHtml(String text) {
        Matcher matcher = HTML_TAG_PATTERN.matcher(text);
        return matcher.find();
    }

    /**
     * Duplicate of {@link StringTools#stripJS(String)} because it seemed to fit here
     *
     * @param s
     * @return s stripped of any JavaScript
     */
    public static String stripJS(String s) {
        return StringTools.stripJS(s);
    }

    public static String getPlaintext(String htmlText) {
        Document doc = Jsoup.parse(htmlText);
        return doc.text();
    }

    public static Set<String> getDisallowedHtmlTags() {
        return DISALLOWED_HTML_TAGS;
    }

    public static String getDisallowedHtmlTagsForDisplay() {
        return StringUtils.join(DISALLOWED_HTML_TAGS, ", ");
    }

    public static Map<String, String> getHtmlReplacements() {
        return HTML_REPLACEMENTS;
    }

}
