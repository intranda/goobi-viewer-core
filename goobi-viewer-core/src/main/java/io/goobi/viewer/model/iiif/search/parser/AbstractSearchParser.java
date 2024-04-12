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
package io.goobi.viewer.model.iiif.search.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Abstract AbstractSearchParser class.
 * </p>
 *
 * @author florian
 */
public abstract class AbstractSearchParser {

    /**
     * <p>
     * getPrecedingText.
     * </p>
     *
     * @param text a {@link java.lang.String} object.
     * @param hitStartIndex a int.
     * @param maxLength a int.
     * @return a {@link java.lang.String} object.
     */
    public static String getPrecedingText(String text, int hitStartIndex, int maxLength) {
        String before = "";
        int index = hitStartIndex - 1;
        while (index > -1 && before.length() < maxLength) {
            String c = Character.toString(text.charAt(index));
            before = c + before;
            index--;
            while (index > -1 && !StringUtils.isWhitespace(c)) {
                c = Character.toString(text.charAt(index));
                before = c + before;
                index--;
            }
        }
        return before;
    }

    /**
     * <p>
     * getSucceedingText.
     * </p>
     *
     * @param text a {@link java.lang.String} object.
     * @param hitEndIndex a int.
     * @param maxLength a int.
     * @return a {@link java.lang.String} object.
     */
    public static String getSucceedingText(String text, int hitEndIndex, int maxLength) {
        String after = "";
        int index = hitEndIndex;
        while (index < text.length() && after.length() < maxLength) {
            String c = Character.toString(text.charAt(index));
            after = after + c;
            index++;
            while (index < text.length() && !StringUtils.isWhitespace(c)) {
                c = Character.toString(text.charAt(index));
                after = after + c;
                index++;
            }
        }
        return after;
    }

    /**
     * <p>
     * getSingleWordRegex.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @return a regex matching a single word matching the given query regex (ignoring case)
     */
    public static String getSingleWordRegex(final String query) {
        // remove any possible ignore case flags from query
        return "(?i)(?:^|\\s+|[.:,;!?\\(\\)])(" + query.replace("(?i)", "") + ")(?=$|\\s+|[.:,;!?\\(\\)])";
    }

    /**
     * <p>
     * getContainedWordRegex.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @return a regex matching any text containing the given query regex as single word
     */
    public static String getContainedWordRegex(final String query) {
        // remove any possible ignore case flags from query
        return "(?i)[\\w\\W]*(?:^|\\s+|[.:,;!?\\(\\)])(" + query.replace("(?i)", "") + ")(?:$|\\s+|[.:,;!?\\(\\)])[\\w\\W]*";
    }

    /**
     * <p>
     * getQueryRegex.
     * </p>
     *
     * @return a regex matching any word or sequence of words of the given query with '*' matching any number of word characters and ignoring case
     * @param query a {@link java.lang.String} object.
     */
    public static String getQueryRegex(final String query) {
        String useQuery = query.replace("(?i)", ""); //remove any possible ignore case flags

        Matcher literalPartsMatcher = Pattern.compile("[^\\s*]+").matcher(useQuery);
        List<MatchGroup> matchGroups = new ArrayList<>();
        while (literalPartsMatcher.find()) {
            int start = literalPartsMatcher.start();
            int end = literalPartsMatcher.end();
            String s = Pattern.quote(literalPartsMatcher.group());
            matchGroups.add(new MatchGroup(start, end, s));
        }
        Collections.reverse(matchGroups);
        String queryRegex = useQuery;
        for (MatchGroup matchGroup : matchGroups) {
            queryRegex = queryRegex.substring(0, matchGroup.getStart()) + matchGroup.getText() + queryRegex.substring(matchGroup.getEnd());
        }
        queryRegex = queryRegex.replace("*", "[\\w\\d-]*").replaceAll("\\s+", "\\\\s*|\\\\s*");

        return "(?i)" + "(?:[.:,;!?\\(\\)]?)((?:" + queryRegex + ")+)(?:[.:,;!?\\(\\)]?)";
    }

    /**
     * Create a regular expression matching all anything starting with the given query followed by an arbitrary number of word characters and ignoring
     * case
     *
     * @param query a {@link java.lang.String} object.
     * @return the regular expression {@code (?i){query}[\w\d-]*}
     */
    public static String getAutoSuggestRegex(final String query) {
        // remove any possible ignore case flags from query
        String queryRegex = query.replace("(?i)", "") + "[\\w\\d-]*";
        return "(?i)" + queryRegex;
    }
}
