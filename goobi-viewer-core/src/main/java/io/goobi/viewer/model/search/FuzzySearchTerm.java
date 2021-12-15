package io.goobi.viewer.model.search;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DamerauLevenshtein;

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
    public static final String WORD_PATTERN = "[\\p{L}=-_\\d]+";

    
    private final String fullTerm;
    private final String term;
    private final boolean wildcardFront;
    private final boolean wildcardBack;
    private final int maxDistance;
    
    public FuzzySearchTerm(String term) {
        this.fullTerm = term;
        if(isFuzzyTerm(term)) {            
            this.term = this.fullTerm.replaceAll("\\*?("+WORD_PATTERN+")\\*?~\\d", "$1").toLowerCase();
            this.maxDistance = Integer.parseInt(this.fullTerm.replaceAll("\\*?"+WORD_PATTERN+"\\*?~(\\d)", "$1"));
            wildcardBack = this.fullTerm.endsWith("*~"+this.maxDistance);
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
        return term.matches("\\*?"+WORD_PATTERN+"+\\*?~\\d");
    }

    public boolean matches(String text) {
        if((wildcardFront || wildcardBack) && text.length() >= this.term.length()-this.maxDistance) {
            for(int pos = 0; pos < text.length()-(this.term.length()-this.maxDistance); pos++) {
                for(int length=this.term.length()-this.maxDistance; length <= Math.min(text.length()-pos, this.term.length()+maxDistance); length++) {
                    String subString = text.substring(pos, pos+length);
                    int distance = new DamerauLevenshtein(subString.toLowerCase(), this.term.toLowerCase()).getSimilarity();
                    if(distance <= maxDistance) {
                        return true;
                    }
                }
            }
            return false;
        } else if( Math.abs(text.length() - this.term.length()) <= this.maxDistance) {
            int distance = new DamerauLevenshtein(text.toLowerCase(), this.term.toLowerCase()).getSimilarity();
            return distance <= maxDistance;
        } else {            
            return false;
        }
    }

    public static int calculateOptimalDistance(String term) {
        if(StringUtils.isBlank(term)) {
            return 0;
        } else if(term.matches(IGNORE_FUZZY_PATTERN)) {
            return 0;
        } else if(term.length() < FUZZY_THRESHOLD_DISTANCE_1) {
            return 0;
        } else if(term.length() < FUZZY_THRESHOLD_DISTANCE_2) {
            return 1;
        } else {
            return 2;
        }
    }
}
