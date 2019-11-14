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
package io.goobi.viewer.model.iiif.search.parser;

import org.apache.commons.lang3.StringUtils;

/**
 * @author florian
 *
 */
public abstract class AbstractSearchParser {

    public static String getPrecedingText(String text, int hitStartIndex, int maxLength) {
        String before = "";
        int index = hitStartIndex-1;
        while(index > -1 && before.length() < maxLength) {
            String c = Character.toString(text.charAt(index));
            before = c + before;
            index--;
            while(index > -1 && !StringUtils.isWhitespace(c)) {
                c = Character.toString(text.charAt(index));
                before = c + before;
                index--;
            }
        }
        return before;
    }
    
    public static String getSucceedingText(String text, int hitEndIndex, int maxLength) {
        String after = "";
        int index = hitEndIndex+1;
        while(index < text.length() && after.length() < maxLength) {
            String c = Character.toString(text.charAt(index));
            after = after + c;
            index++;
            while(index < text.length() && !StringUtils.isWhitespace(c)) {
                c = Character.toString(text.charAt(index));
                after = after + c;
                index++;
            }
        }
        return after;
    }
    
    
    /**
     * @param query
     * @return a regex matching a single word matching the given query regex (ignoring case)
     */
    public static String getSingleWordRegex(String query) {
        query = query.replace("(?i)", ""); //remove any possible ignore case flags
        return "(?i)(?:^|\\s+|[.:,;!?\\(\\)])(" + query + ")(?=$|\\s+|[.:,;!?\\(\\)])";
    }

    /**
     * 
     * @param query
     * @return a regex matching any text containing the given query regex as single word
     */
    public static String getContainedWordRegex(String query) {
        query = query.replace("(?i)", ""); //remove any possible ignore case flags
        return "(?i)[\\w\\W]*(?:^|\\s+|[.:,;!?\\(\\)])(" + query + ")(?:$|\\s+|[.:,;!?\\(\\)])[\\w\\W]*";
    }
    
    /**
     * @return a regex matching any word or sequence of words of the given query with '*' matching any number of word characters and ignoring case
     */
    public static String getQueryRegex(String query) {
        query = query.replace("(?i)", ""); //remove any possible ignore case flags
        String queryRegex = query.replace("*", "[\\w\\d-]*").replaceAll("\\s+", "\\\\s*|\\\\s*");
        return "(?i)" + "((?:" + queryRegex + ")+)";
    }

    /**
     * Create a regular expression matching all anything starting with the given query followed by an arbitrary number of word characters and ignoring
     * case
     * 
     * @param query
     * @return the regular expression {@code (?i){query}[\w\d-]*}
     */
    public static String getAutoSuggestRegex(String query) {
        query = query.replace("(?i)", ""); //remove any possible ignore case flags
        String queryRegex = query + "[\\w\\d-]*";
        return "(?i)" + queryRegex;
    }
    
    
}
