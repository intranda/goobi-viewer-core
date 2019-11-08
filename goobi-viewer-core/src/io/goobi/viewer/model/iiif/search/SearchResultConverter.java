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
package io.goobi.viewer.model.iiif.search;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.oa.TextQuoteSelector;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.search.SearchHit;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.AbstractBuilder;
import io.goobi.viewer.model.iiif.search.parser.AltoSearchParser;

/**
 * @author florian
 *
 */
public class SearchResultConverter {

    private static final int MAX_TEXT_LENGTH = 20;
    
    private final AltoAnnotationBuilder altoBuilder = new AltoAnnotationBuilder();
    private final AbstractBuilder presentationBuilder;
    private final AltoSearchParser altoParser = new AltoSearchParser();
    private String pi;
    private Integer pageNo;
    
    public SearchResultConverter(URI requestURI, URI restApiURI, String pi, Integer pageNo) {
        this.presentationBuilder = new AbstractBuilder(requestURI, restApiURI) {};
        this.pi = pi;
        this.pageNo = pageNo;
    }

    public void setPi(String pi) {
        this.pi = pi;
    }

    public String getPi() {
        return pi;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }
    
    public Integer getPageNo() {
        return pageNo;
    }
    
    public IAnnotation convertToAnnotation(GeometricData altoElement) {
        return altoBuilder.createAnnotation(altoElement, 
                getCanvas(getPi(), getPageNo()), getAnnotationListURI(getPi(), getPageNo(), AnnotationType.ALTO).toString());
    }
    
    public SearchHit convertToHit(List<Word> altoElements) {
        SearchHit hit = new SearchHit();
        hit.setAnnotations(altoElements.stream().map(this::convertToAnnotation).collect(Collectors.toList()));
        hit.setMatch(altoElements.stream().map(Word::getSubsContent).collect(Collectors.joining(" ")));
        
        if(!altoElements.isEmpty()) {
            String before = altoParser.getPrecedingText(altoElements.get(0), MAX_TEXT_LENGTH);
            String after = new AltoSearchParser().getSucceedingText(altoElements.get(altoElements.size()-1), MAX_TEXT_LENGTH);
            if(StringUtils.isNotBlank(before)) {
                hit.setBefore(before);
            }
            if(StringUtils.isNotBlank(after)) {
                hit.setAfter(after);
            }
        }
        return hit;
    }
    
    public List<SearchHit> convertToHits(List<Line> altoElements, String matchQuery) {
        List<SearchHit> hits = new ArrayList<>();
        String wholeText = altoElements.stream().map(Line::getContent).collect(Collectors.joining(" "));
        Matcher m = Pattern.compile(matchQuery).matcher(wholeText);
        while(m.find()) {
            SearchHit hit = new SearchHit();
            String match = m.group(1);
            int indexStart = m.start(1);
            int indexEnd = m.end(1);
            
            hit.setMatch(match);
            String before = altoParser.getPrecedingText(wholeText, indexStart, MAX_TEXT_LENGTH);
            String after = altoParser.getSucceedingText(wholeText, indexEnd, MAX_TEXT_LENGTH);
            if(StringUtils.isNotBlank(before)) {
                hit.setBefore(before);
            }
            if(StringUtils.isNotBlank(after)) {
                hit.setAfter(after);
            }
            hit.setAnnotations(altoParser.getContainingLines(altoElements, indexStart, indexEnd).stream().map(this::convertToAnnotation).collect(Collectors.toList()));
            hits.add(hit);
        }
        return hits;
    }

    /**
     * @return the presentationBuilder
     */
    public AbstractBuilder getPresentationBuilder() {
        return presentationBuilder;
    };
    



    /**
     * @param pi2
     * @param pageNo2
     * @return
     */
    private URI getAnnotationListURI(String pi, Integer pageNo, AnnotationType type) {
        if(pageNo != null) {            
            return presentationBuilder.getAnnotationListURI(pi, pageNo, type);
        } else {
            return presentationBuilder.getAnnotationListURI(pi, type);
        }
    }

    /**
     * @param pi2
     * @param pageNo2
     * @return
     */
    private Canvas getCanvas(String pi, Integer pageNo) {
        if(StringUtils.isBlank(pi) || pageNo == null) {
            return null;
        }
        return new Canvas(presentationBuilder.getCanvasURI(pi, pageNo));
    }

    /**
     * @param lines
     * @param position
     * @param containingLines
     */
    public SearchHit convertToHit(List<Line> lines, Range<Integer> position, List<Line> containingLines) {
        SearchHit hit = new SearchHit();
        String wholeText = altoParser.getText(containingLines);
        int firstLineStartIndex = altoParser.getLineStartIndex(lines, containingLines.get(0));
        int lastLineEndIndex = altoParser.getLineEndIndex(lines, containingLines.get(containingLines.size()-1));
        
        String before = altoParser.getPrecedingText(wholeText, position.getMinimum(), Math.min( position.getMinimum()-firstLineStartIndex, MAX_TEXT_LENGTH));
        String after = altoParser.getSucceedingText(wholeText, position.getMaximum(), Math.min( lastLineEndIndex-position.getMaximum(), MAX_TEXT_LENGTH));
        String match = wholeText.substring(position.getMinimum(), position.getMaximum()+1);
        
        hit.setMatch(wholeText.substring(position.getMinimum(), position.getMaximum()+1));
        TextQuoteSelector selector = new TextQuoteSelector();
        selector.setFragment(match);
        if(StringUtils.isNotBlank(before)) {
            selector.setPrefix(before);
        }
        if(StringUtils.isNotBlank(after)) {
            selector.setSuffix(after);
        }
        hit.setSelectors(Collections.singletonList(selector));
        hit.setAnnotations(containingLines.stream().map(this::convertToAnnotation).collect(Collectors.toList()));
        return hit;
    }
}
