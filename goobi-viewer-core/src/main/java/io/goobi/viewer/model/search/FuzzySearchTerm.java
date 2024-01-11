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
package io.goobi.viewer.model.search;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DamerauLevenshtein;
import io.goobi.viewer.controller.StringTools;

/**
 * Class for extracting the actual search term from a fuzzy search. For creating a fuzzy search term see
 * {@link SearchHelper#addFuzzySearchToken(String, String, String)}
 *
 * @author florian
 *
 */
public class FuzzySearchTerm {

    /**
     * Don't use fuzzy search on search terms matching this pattern
     */
    private static final String IGNORE_FUZZY_PATTERN = "\\d+";
    /**
     * For search terms below this length, don't use fuzzy search
     */
    private static final int FUZZY_THRESHOLD_DISTANCE_1 = 4;
    /**
     * For search terms of at least this length, use fuzzy distance 2
     */
    private static final int FUZZY_THRESHOLD_DISTANCE_2 = 9;
    /**
     * Regex matching all characters within words, including umlauts etc.
     */
    public static final String WORD_PATTERN = "[\\p{L}=-_\\d⸗¬]+";
    /**
     * Regex matching anything not matching {@link #WORD_PATTERN}, including an empty string
     */
    public static final String NOT_WORD_PATTERN = "[^\\p{L}=-_\\d⸗¬]*";
    /**
     * Regex matching a word (according to {@link #WORD_PATTERN}, surrounded by other characters (according to {@link #NOT_WORD_PATTERN}. The word
     * itself is the first capture group
     */
    public static final String WORD_SURROUNDED_BY_OTHER_CHARACTERS = NOT_WORD_PATTERN + "(" + WORD_PATTERN + ")" + NOT_WORD_PATTERN;

    private final String fullTerm;
    private final String term;
    private final boolean wildcardFront;
    private final boolean wildcardBack;
    private final int maxDistance;

    public FuzzySearchTerm(String term) {
        this.fullTerm = term;
        if (isFuzzyTerm(term)) {
            this.term =
                    this.fullTerm.replaceAll("[*]{0,1}(" + WORD_PATTERN + ")[*]{0,1}~\\d", "$1")
                            .toLowerCase(); //NOSONAR no catastrophic backtracking detected
            this.maxDistance = Integer.parseInt(this.fullTerm
                    .replaceAll("[*]{0,1}" + WORD_PATTERN + "[*]{0,1}~(\\d)", "$1")); //NOSONAR no catastrophic backtracking detected
            wildcardBack = this.fullTerm.endsWith("*~" + this.maxDistance);
        } else {
            this.term = term;
            this.maxDistance = 0;
            wildcardBack = this.fullTerm.endsWith("*");
        }
        wildcardFront = this.fullTerm.startsWith("*");
    }

    public String getFullTerm() {
        return fullTerm;
    }

    public String getTerm() {
        return term;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public boolean isWildcardBack() {
        return wildcardBack;
    }

    public boolean isWildcardFront() {
        return wildcardFront;
    }

    public static boolean isFuzzyTerm(String term) {
        return term.matches("[*]{0,1}" + WORD_PATTERN + "[*]{0,1}~\\d");
    }

    /**
     * Test if a given text containing a single word
     * 
     * @param text
     * @return boolean
     */
    public boolean matches(final String text) {
        String t = cleanup(text);
        String termToMatch = cleanup(this.term);
        if ((wildcardFront || wildcardBack) && t.length() >= termToMatch.length() - this.maxDistance) {
            for (int pos = 0; pos < t.length() - (termToMatch.length() - this.maxDistance); pos++) {
                for (int length = termToMatch.length() - this.maxDistance; length <= Math.min(t.length() - pos,
                        termToMatch.length() + maxDistance); length++) {
                    String subString = t.substring(pos, pos + length);
                    int distance = new DamerauLevenshtein(subString, termToMatch).getSimilarity();
                    if (distance <= maxDistance) {
                        return true;
                    }
                }
            }
            return false;
        } else if (Math.abs(t.length() - termToMatch.length()) <= this.maxDistance) {
            int distance = new DamerauLevenshtein(t, termToMatch).getSimilarity();
            return distance <= maxDistance;
        } else {
            return false;
        }
    }

    /**
     * 
     * @param text
     * @return Cleaned-up text
     */
    private static String cleanup(final String text) {
        String ret = text;
        if (StringUtils.isNotBlank(ret)) {
            ret = cleanHyphenations(ret);
            ret = StringTools.removeDiacriticalMarks(ret);
            ret = StringTools.replaceCharacterVariants(ret);
            ret = ret.toLowerCase();
            ret =
                    ret.replaceAll(WORD_SURROUNDED_BY_OTHER_CHARACTERS, "$1"); //NOSONAR removes anything before and after the word, backtracking save
        }

        return ret;
    }

    /**
     * 
     * @param text
     * @return Cleaned-up text
     */
    private static String cleanHyphenations(String text) {
        return text.replaceAll("[⸗¬-]", "");
    }

    public static int calculateOptimalDistance(String term) {
        if (StringUtils.isBlank(term)) {
            return 0;
        } else if (term.matches(IGNORE_FUZZY_PATTERN)) {
            return 0;
        } else if (term.length() < FUZZY_THRESHOLD_DISTANCE_1) {
            return 0;
        } else if (term.length() < FUZZY_THRESHOLD_DISTANCE_2) {
            return 1;
        } else {
            return 2;
        }
    }
}
