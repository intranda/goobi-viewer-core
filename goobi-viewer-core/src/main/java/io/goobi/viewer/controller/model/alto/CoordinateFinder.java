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

package io.goobi.viewer.controller.model.alto;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;

import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.utils.HyphenationLinker;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.StringTools;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Detect word coordinates in alto document
 */
public class CoordinateFinder {

    private static final Logger logger = LogManager.getLogger(CoordinateFinder.class);

    private static final String CONTENT = "CONTENT";

    private final AltoDocument document;

    public CoordinateFinder(String altoString, String charset) throws IOException, JDOMException {
        if (altoString == null) {
            throw new IllegalArgumentException("altoDoc may not be null");
        }
        this.document = AltoDocument.getDocumentFromString(altoString, StringUtils.isBlank(charset) ? StringTools.DEFAULT_ENCODING : charset);
        HyphenationLinker linker = new HyphenationLinker();
        linker.linkWords(this.document);
    }

    public List<String> getWordCoords(Set<String> searchTerms, int proximitySearchDistance, int rotation) {
        return getWordCoordinates(searchTerms, proximitySearchDistance, rotation, getAllWords(), getPageSize());
    }

    Dimension getPageSize() {
        Dimension pageSize = new Dimension(0, 0);
        try {
            Page page = document.getFirstPage();
            pageSize = new Dimension((int) page.getWidth(), (int) page.getHeight());
        } catch (NullPointerException e) {
            logger.error("Could not parse ALTO: No width or height specified in 'page' element.");
        } catch (NumberFormatException e) {
            logger.error("Could not parse ALTO: Could not parse page width or height.");
        }
        return pageSize;
    }

    List<Word> getAllWords() {
        List<Word> words = new ArrayList<>();
        Page page = document.getFirstPage();
        List<Line> lines = page.getAllLinesAsList();
        for (Line line : lines) {
            words.addAll(line.getWords());
        }
        logger.trace("{} ALTO words found for this page.", words.size());
        return words;
    }

    private List<String> getWordCoordinates(
            Set<String> searchTerms,
            int proximitySearchDistance,
            int rotation,
            List<Word> words,
            Dimension pageSize) {

        List<String> coordList = new ArrayList<>();

        for (String term : searchTerms) {
            String[] searchWords = normalizeSearchTerm(term);
            if (isInvalidSearch(searchWords)) {
                continue;
            }

            coordList.addAll(
                    findMatchesForSearchWords(
                            searchWords,
                            proximitySearchDistance,
                            rotation,
                            words,
                            pageSize));
        }

        return coordList;
    }

    private String[] normalizeSearchTerm(String term) {
        String cleaned = StringTools.removeQuotations(term);
        return cleaned.split("\\s+");
    }

    private boolean isInvalidSearch(String[] searchWords) {
        return searchWords == null
                || searchWords.length == 0
                || StringUtils.isBlank(searchWords[0]);
    }

    private List<String> findMatchesForSearchWords(
            String[] searchWords,
            int proximitySearchDistance,
            int rotation,
            List<Word> words,
            Dimension pageSize) {

        List<String> results = new ArrayList<>();

        for (int index = 0; index < words.size(); index++) {
            MatchResult matchResult = tryMatchFromIndex(
                    words,
                    index,
                    searchWords,
                    proximitySearchDistance,
                    rotation,
                    pageSize);

            if (matchResult.isMatch()) {
                results.addAll(matchResult.getCoordinates());
                index = matchResult.getLastIndex(); // skip consumed words
            }
        }

        return results;
    }

    private MatchResult tryMatchFromIndex(
            List<Word> words,
            int startIndex,
            String[] searchWords,
            int proximitySearchDistance,
            int rotation,
            Dimension pageSize) {

        List<String> coords = new ArrayList<>();
        int index = startIndex;

        Word firstWord = words.get(index);
        int totalHits = ALTOTools.getMatchALTOWord(firstWord, searchWords);

        if (totalHits == 0) {
            return MatchResult.noMatch(startIndex);
        }

        index = addWordAndHyphenation(firstWord, index, rotation, pageSize, coords, words);

        if (totalHits == searchWords.length) {
            return MatchResult.match(coords, index);
        }

        return continueProximityMatch(
                words,
                index,
                totalHits,
                searchWords,
                proximitySearchDistance,
                rotation,
                pageSize,
                coords);
    }

    private MatchResult continueProximityMatch(
            List<Word> words,
            int index,
            int totalHits,
            String[] searchWords,
            int proximitySearchDistance,
            int rotation,
            Dimension pageSize,
            List<String> coords) {

        int remainingReach = proximitySearchDistance;

        while (totalHits < searchWords.length && index + 1 < words.size()) {
            index++;
            Word nextWord = words.get(index);

            int hits = ALTOTools.getMatchALTOWord(
                    nextWord,
                    Arrays.copyOfRange(searchWords, totalHits, searchWords.length));

            if (hits == 0) {
                if (remainingReach-- < 1) {
                    return MatchResult.noMatch(index - 1);
                }
            } else {
                remainingReach = proximitySearchDistance;
            }

            totalHits += hits;
            index = addWordAndHyphenation(nextWord, index, rotation, pageSize, coords, words);
        }

        return totalHits == searchWords.length
                ? MatchResult.match(coords, index)
                : MatchResult.noMatch(index);
    }

    private int addWordAndHyphenation(
            Word word,
            int index,
            int rotation,
            Dimension pageSize,
            List<String> coords,
            List<Word> words) {

        addWordCoords(rotation, pageSize, word, coords);

        if (word.getHyphenationPartNext() != null
                && word.getHyphenationPartNext().getContent().matches("\\S+")) {

            addWordCoords(rotation, pageSize, word.getHyphenationPartNext(), coords);
            return index + 1;
        }

        return index;
    }

    /**
     * 
     * @param rotation
     * @param pageSize
     * @param eleWord
     * @param tempList
     * @return ALTO word coordinates as a {@link String}
     */
    private String addWordCoords(int rotation, Dimension pageSize, Word eleWord, List<String> tempList) {
        String coords = ALTOTools.getALTOCoords(eleWord);
        if (coords != null && rotation != 0) {
            try {
                coords = ALTOTools.getRotatedCoordinates(coords, rotation, pageSize);
            } catch (NumberFormatException e) {
                logger.error("Cannot rotate coords {}: {}", coords, e.getMessage());
            }
        }
        if (coords != null) {
            tempList.add(coords);
            if (logger.isTraceEnabled()) {
                logger.trace("ALTO word found: {} ({})", eleWord.getAttributeValue(CONTENT), coords);
            }
        }

        return coords;
    }

    private static class MatchResult {
        private final boolean match;
        private final List<String> coordinates;
        private final int lastIndex;

        private MatchResult(boolean match, List<String> coordinates, int lastIndex) {
            this.match = match;
            this.coordinates = coordinates;
            this.lastIndex = lastIndex;
        }

        static MatchResult match(List<String> coords, int lastIndex) {
            return new MatchResult(true, coords, lastIndex);
        }

        static MatchResult noMatch(int lastIndex) {
            return new MatchResult(false, Collections.emptyList(), lastIndex);
        }

        boolean isMatch() {
            return match;
        }

        List<String> getCoordinates() {
            return coordinates;
        }

        int getLastIndex() {
            return lastIndex;
        }
    }
}
