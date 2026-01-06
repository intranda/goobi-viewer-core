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
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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

/**
 * Detect word coordinates in alto document Alternative to {@link CoordinateFinder} which only finds the exact matches for proximity distance > 0
 * without the words in between
 * 
 * Unused alternative to {@link CoordinateFinder}
 */
public class WordCoordinateService {

    private static final Logger logger = LogManager.getLogger(WordCoordinateService.class);

    private static final String CONTENT = "CONTENT";

    private final AltoDocument document;

    public WordCoordinateService(String altoString, String charset) throws IOException, JDOMException {
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

    /**
     * Main entry point: Now follows a clear, linear flow.
     */
    private List<String> getWordCoordinates(
            Set<String> searchTerms,
            int proximitySearchDistance,
            int rotation,
            List<Word> words,
            Dimension pageSize) {

        List<String> coordList = new ArrayList<>();

        for (String term : searchTerms) {
            String[] searchWords = prepareSearchWords(term);
            if (searchWords.length == 0)
                continue;

            coordList.addAll(findCoordinatesForTerm(searchWords, proximitySearchDistance, rotation, words, pageSize));
        }

        return coordList;
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

    private static String[] prepareSearchWords(String term) {
        String cleaned = StringTools.removeQuotations(term);
        if (StringUtils.isBlank(cleaned))
            return new String[0];
        return cleaned.split("\\s+");
    }

    private static List<String> findCoordinatesForTerm(
            String[] searchWords, int proximity, int rotation, List<Word> words, Dimension pageSize) {

        List<String> results = new ArrayList<>();

        for (int i = 0; i < words.size(); i++) {
            Word startWord = words.get(i);
            int matchedInStart = ALTOTools.getMatchALTOWord(startWord, searchWords);

            if (matchedInStart > 0) {
                MatchSession session = new MatchSession(words, i, searchWords, proximity, rotation, pageSize);
                if (session.attemptMatch(matchedInStart)) {
                    results.addAll(session.getFoundCoordinates());
                    // Update index to the last word consumed to avoid redundant processing
                    i = session.getCurrentIndex();
                }
            }
        }
        return results;
    }

    /**
     * Helper class to manage the state of a single phrase/proximity search. This encapsulates the complexity of "looking ahead" in the word list.
     */
    private static class MatchSession {
        private final List<Word> words;
        private final String[] searchWords;
        private final int proximityDistance;
        private final int rotation;
        private final Dimension pageSize;

        private int currentIndex;
        private int totalHits;
        private final List<String> tempCoords = new ArrayList<>();

        public MatchSession(List<Word> words, int startIndex, String[] searchWords, int proximity, int rotation, Dimension pageSize) {
            this.words = words;
            this.currentIndex = startIndex;
            this.searchWords = searchWords;
            this.proximityDistance = proximity;
            this.rotation = rotation;
            this.pageSize = pageSize;
        }

        public boolean attemptMatch(int initialHits) {
            this.totalHits = initialHits;
            Word currentWord = words.get(currentIndex);

            collectWordCoords(currentWord);

            while (totalHits < searchWords.length && hasNextWord()) {
                if (!processNextWordStep()) {
                    return false;
                }
            }
            return totalHits >= searchWords.length;
        }

        private boolean processNextWordStep() {
            int remainingProximity = proximityDistance;

            while (hasNextWord()) {
                currentIndex++;
                Word nextWord = words.get(currentIndex);
                String[] remainingSearch = Arrays.copyOfRange(searchWords, totalHits, searchWords.length);
                int hits = ALTOTools.getMatchALTOWord(nextWord, remainingSearch);

                if (hits > 0) {
                    totalHits += hits;
                    collectWordCoords(nextWord);
                    return true; // Found a match within proximity
                } else {
                    if (remainingProximity <= 0)
                        return false; // Exhausted proximity range
                    remainingProximity--;
                }
            }
            return false;
        }

        private void collectWordCoords(Word word) {
            addWordCoords(rotation, pageSize, word, tempCoords);
            // Handle hyphenated segments automatically
            if (word.getHyphenationPartNext() != null) {
                currentIndex++;
                addWordCoords(rotation, pageSize, word.getHyphenationPartNext(), tempCoords);
            }
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

        private boolean hasNextWord() {
            return currentIndex + 1 < words.size();
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public List<String> getFoundCoordinates() {
            return tempCoords;
        }
    }
}