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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;

import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;

/**
 * IIIF Search API parser that searches for matches within ALTO full-text documents.
 *
 * @author Florian Alpers
 */
public class AltoSearchParser extends AbstractSearchParser {

    /**
     * findWordMatches.
     *
     * @param words candidate words to match against
     * @param regex regular expression to test each word's content
     * @return a list of matched word groups, where each group is a consecutive sequence of matching ALTO words
     */
    public List<List<Word>> findWordMatches(List<Word> words, String regex) {
        ListIterator<Word> iterator = words.listIterator();
        List<List<Word>> results = new ArrayList<>();
        while (iterator.hasNext()) {
            Word word = iterator.next();
            if (Pattern.matches(regex, word.getSubsContent())) {
                List<Word> phrase = new ArrayList<>();
                phrase.add(word);
                while (iterator.hasNext()) {
                    Word nextWord = iterator.next();
                    if (Pattern.matches(regex, nextWord.getSubsContent())) {
                        phrase.add(nextWord);
                    } else {
                        break;
                    }
                }
                results.add(phrase);
            }
        }
        return results;
    }

    /**
     * findLineMatches.
     *
     * @param lines ALTO lines to search through
     * @param regex regular expression applied to concatenated line text
     * @return a map of character-index ranges to the ALTO lines containing the match
     */
    public Map<Range<Integer>, List<Line>> findLineMatches(List<Line> lines, String regex) {
        String text = getText(lines);
        Map<Range<Integer>, List<Line>> map = new LinkedHashMap<>();
        String singleWordRegex = getSingleWordRegex(regex);
        Matcher matcher = Pattern.compile(singleWordRegex).matcher(text);
        while (matcher.find()) {
            int indexStart = matcher.start(1);
            int indexEnd = matcher.end(1);
            List<Line> containingLines = getContainingLines(lines, indexStart, indexEnd);
            Range<Integer> range = Range.of(indexStart, indexEnd);
            map.put(range, containingLines);
        }
        return map;
    }

    /**
     * getText.
     *
     * @param lines ALTO lines whose content to concatenate
     * @return the concatenated text content of the given ALTO lines, joined by spaces
     */
    public String getText(List<Line> lines) {
        return lines.stream().map(Line::getContent).collect(Collectors.joining(" "));
    }

    /**
     * getLines.
     *
     * @param doc ALTO document to extract lines from
     * @return a list of all ALTO text lines contained in the given document
     */
    public List<Line> getLines(AltoDocument doc) {
        return doc.getAllPagesAsList().stream().flatMap(p -> p.getAllLinesAsList().stream()).toList();
    }

    /**
     * getWords.
     *
     * @param doc ALTO document to extract words from
     * @return a list of all ALTO word elements contained in the given document
     */
    public List<Word> getWords(AltoDocument doc) {
        return doc.getAllPagesAsList()
                .stream()
                .flatMap(p -> p.getAllWordsAsList().stream().filter(Word.class::isInstance).map(w -> (Word) w))
                .toList();
    }

    /**
     * getContainingLines.
     *
     * @param allLines all ALTO lines with their content
     * @param indexStart start character index of the match
     * @param indexEnd end character index of the match
     * @return a list of ALTO lines whose character range overlaps with the given match positions
     */
    public List<Line> getContainingLines(List<Line> allLines, int indexStart, int indexEnd) {
        List<Line> containingLines = new ArrayList<>();
        int lineStartIndex = 0;
        for (Line line : allLines) {
            int lineEndIndex = lineStartIndex + line.getContent().length();
            if (indexStart <= lineEndIndex && indexEnd >= lineStartIndex) {
                containingLines.add(line);
            }
            lineStartIndex = lineEndIndex + 1;
        }
        return containingLines;
    }

    /**
     * getLineStartIndex.
     *
     * @param allLines all ALTO lines providing character offsets
     * @param line target line whose start index is sought
     * @return a int.
     */
    public int getLineStartIndex(List<Line> allLines, Line line) {
        int lineStartIndex = 0;
        for (Line l : allLines) {
            int lineEndIndex = lineStartIndex + line.getContent().length();
            if (l.getId().endsWith(line.getId())) {
                return lineStartIndex;
            }
            lineStartIndex = lineEndIndex + 1;
        }
        return -1;
    }

    /**
     * getLineEndIndex.
     *
     * @param allLines all ALTO lines providing character offsets
     * @param line target line whose end index is sought
     * @return a int.
     */
    public int getLineEndIndex(List<Line> allLines, Line line) {
        int lineStartIndex = 0;
        for (Line l : allLines) {
            int lineEndIndex = lineStartIndex + line.getContent().length();
            if (l.getId().endsWith(line.getId())) {
                return lineEndIndex;
            }
            lineStartIndex = lineEndIndex + 1;
        }
        return -1;
    }

    /**
     * getPrecedingText.
     *
     * @param w word whose preceding siblings to collect
     * @param maxLength maximum character count of returned text
     * @return the text content of sibling words preceding the given word, up to maxLength characters
     */
    public String getPrecedingText(Word w, int maxLength) {

        int wordIndex = w.getParent().getWords().indexOf(w);
        StringBuilder before = new StringBuilder();
        if (wordIndex > -1) {
            ListIterator<Word> lineIterator = w.getParent().getWords().listIterator(wordIndex);
            while (lineIterator.hasPrevious() && before.length() < maxLength) {
                if (before.isEmpty() || !Character.isWhitespace(before.charAt(0))) {
                    before.insert(0, " ");
                }
                before.insert(0, lineIterator.previous().getContent());
            }
        }
        return before.toString().trim();
    }

    /**
     * getSucceedingText.
     *
     * @param w word whose following siblings to collect
     * @param maxLength maximum character count of returned text
     * @return the text content of sibling words following the given word, up to maxLength characters
     */
    public String getSucceedingText(Word w, int maxLength) {

        int wordIndex = w.getParent().getWords().indexOf(w) + 1;
        StringBuilder after = new StringBuilder();
        if (wordIndex < w.getParent().getWords().size()) {
            ListIterator<Word> lineIterator = w.getParent().getWords().listIterator(wordIndex);
            while (lineIterator.hasNext() && after.length() < maxLength) {
                if (after.isEmpty() || !Character.isWhitespace(after.charAt(after.length() - 1))) {
                    after.append(" ");
                }
                after.append(lineIterator.next().getContent());
            }
        }
        return after.toString().trim();
    }

}
