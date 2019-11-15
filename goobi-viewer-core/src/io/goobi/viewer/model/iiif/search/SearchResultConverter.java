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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.FieldListResource;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.oa.FragmentSelector;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.SpecificResourceURI;
import de.intranda.api.annotation.oa.TextQuoteSelector;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.search.SearchHit;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.iiif.presentation.builder.AbstractBuilder;
import io.goobi.viewer.model.iiif.search.parser.AbstractSearchParser;
import io.goobi.viewer.model.iiif.search.parser.AltoSearchParser;
import io.goobi.viewer.model.iiif.search.parser.SolrSearchParser;

/**
 * @author florian
 *
 */
public class SearchResultConverter {

    private static final int MAX_TEXT_LENGTH = 20;

    private final AltoAnnotationBuilder altoBuilder = new AltoAnnotationBuilder();
    private final AbstractBuilder presentationBuilder;
    private final AltoSearchParser altoParser = new AltoSearchParser();
    private final SolrSearchParser solrParser = new SolrSearchParser();

    private String pi;
    private Integer pageNo;

    public SearchResultConverter(URI requestURI, URI restApiURI, String pi, Integer pageNo) {
        this.presentationBuilder = new AbstractBuilder(requestURI, restApiURI) {
        };
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
        return altoBuilder.createAnnotation(altoElement, getCanvas(getPi(), getPageNo()),
                getAnnotationListURI(getPi(), getPageNo(), AnnotationType.ALTO).toString(), true);
    }

    public SearchHit convertToHit(List<Word> altoElements) {
        SearchHit hit = new SearchHit();
        hit.setAnnotations(altoElements.stream().map(this::convertToAnnotation).collect(Collectors.toList()));
        hit.setMatch(altoElements.stream().map(Word::getSubsContent).collect(Collectors.joining(" ")));

        if (!altoElements.isEmpty()) {
            String before = altoParser.getPrecedingText(altoElements.get(0), MAX_TEXT_LENGTH);
            String after = new AltoSearchParser().getSucceedingText(altoElements.get(altoElements.size() - 1), MAX_TEXT_LENGTH);
            if (StringUtils.isNotBlank(before)) {
                hit.setBefore(before);
            }
            if (StringUtils.isNotBlank(after)) {
                hit.setAfter(after);
            }
        }
        return hit;
    }

    public List<SearchHit> convertToHits(List<Line> altoElements, String matchQuery) {
        List<SearchHit> hits = new ArrayList<>();
        String wholeText = altoElements.stream().map(Line::getContent).collect(Collectors.joining(" "));
        Matcher m = Pattern.compile(matchQuery).matcher(wholeText);
        while (m.find()) {
            SearchHit hit = new SearchHit();
            String match = m.group(1);
            int indexStart = m.start(1);
            int indexEnd = m.end(1);

            hit.setMatch(match);
            String before = AbstractSearchParser.getPrecedingText(wholeText, indexStart, MAX_TEXT_LENGTH);
            String after = AbstractSearchParser.getSucceedingText(wholeText, indexEnd, MAX_TEXT_LENGTH);
            if (StringUtils.isNotBlank(before)) {
                hit.setBefore(before);
            }
            if (StringUtils.isNotBlank(after)) {
                hit.setAfter(after);
            }
            hit.setAnnotations(altoParser.getContainingLines(altoElements, indexStart, indexEnd)
                    .stream()
                    .map(this::convertToAnnotation)
                    .collect(Collectors.toList()));
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
        if (pageNo != null) {
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
        if (StringUtils.isBlank(pi) || pageNo == null) {
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
        int lastLineEndIndex = altoParser.getLineEndIndex(lines, containingLines.get(containingLines.size() - 1));

        String before = AbstractSearchParser.getPrecedingText(wholeText, position.getMinimum(),
                Math.min(position.getMinimum() - firstLineStartIndex, MAX_TEXT_LENGTH));
        String after = AbstractSearchParser.getSucceedingText(wholeText, position.getMaximum(),
                Math.min(lastLineEndIndex - position.getMaximum(), MAX_TEXT_LENGTH));
        String match = wholeText.substring(position.getMinimum(), position.getMaximum() + 1);

        hit.setMatch(match);
        TextQuoteSelector selector = new TextQuoteSelector();
        selector.setFragment(match);
        if (StringUtils.isNotBlank(before)) {
            selector.setPrefix(before);
        }
        if (StringUtils.isNotBlank(after)) {
            selector.setSuffix(after);
        }
        hit.setSelectors(Collections.singletonList(selector));
        hit.setAnnotations(containingLines.stream().map(this::convertToAnnotation).collect(Collectors.toList()));
        return hit;
    }
    
    
    /**
     * @param pi
     * @param pageNo
     * @param results
     * @param text
     * @throws IOException 
     * @throws UnsupportedEncodingException 
     */
    public SearchHit convertToHit(String queryRegex, Path textFile, String pi, Integer pageNo, String encoding) throws UnsupportedEncodingException, IOException {
        
        String text = new String(Files.readAllBytes(textFile), encoding);
        SearchHit hit = new SearchHit();

        Matcher m = Pattern.compile(AbstractSearchParser.getSingleWordRegex(queryRegex)).matcher(text);
        while (m.find()) {
        
            String match = m.group(1);
            int indexStart = m.start(1);
            int indexEnd = m.end(1);
            
            String before = AbstractSearchParser.getPrecedingText(text, indexStart, MAX_TEXT_LENGTH);
            String after = AbstractSearchParser.getSucceedingText(text, indexEnd, MAX_TEXT_LENGTH);
    
            hit.setMatch(match);
            TextQuoteSelector selector = new TextQuoteSelector();
            selector.setFragment(match);
            if (StringUtils.isNotBlank(before)) {
                selector.setPrefix(before);
            }
            if (StringUtils.isNotBlank(after)) {
                selector.setSuffix(after);
            }
            hit.setSelectors(Collections.singletonList(selector));
        }
        
        IResource canvas = createSimpleCanvasResource(pi, pageNo);
        URI baseURI = getPresentationBuilder().getAnnotationListURI(pi, pageNo, AnnotationType.FULLTEXT);
        IAnnotation pageAnnotation = createAnnotation(text, canvas, baseURI.toString());
        hit.addAnnotation(pageAnnotation);
        
        return hit;
    }


    private IAnnotation createAnnotation(String text, IResource canvas, String baseUrl) {
        AbstractAnnotation anno = new OpenAnnotation(createAnnotationId(baseUrl, "plaintext"));
        anno.setMotivation(Motivation.PAINTING);
        anno.setTarget(canvas);
        TextualResource body = new TextualResource(text);
        anno.setBody(body);
        return anno;
    }

    private URI createAnnotationId(String baseUrl, String id) {
        if (baseUrl.endsWith("/")) {
            return URI.create(baseUrl + id);
        } else {
            return URI.create(baseUrl + "/" + id);
        }
    }
    
    /**
     * @param doc
     * @return
     */
    public OpenAnnotation createMetadataAnnotation(String metadataField, SolrDocument doc) {
        String iddoc = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
        String pi = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
        String logId = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.LOGID);
        Boolean isWork = SolrSearchIndex.getSingleFieldBooleanValue(doc, SolrConstants.ISWORK);
        Integer thumbPageNo = SolrSearchIndex.getSingleFieldIntegerValue(doc, SolrConstants.THUMBPAGENO);
        String id = pi + "/" + (StringUtils.isNotBlank(logId) ? (logId + "/") : "") + metadataField;
        OpenAnnotation anno = new OpenAnnotation(getPresentationBuilder().getAnnotationURI(pi, AnnotationType.METADATA, id));
        anno.setMotivation(Motivation.DESCRIBING);
        if(thumbPageNo != null) {
            anno.setTarget(createSimpleCanvasResource(pi, thumbPageNo));
        } else {            
            if (Boolean.TRUE.equals(isWork)) {
                anno.setTarget(new SimpleResource(getPresentationBuilder().getManifestURI(pi)));
            } else {
                anno.setTarget(new SimpleResource(getPresentationBuilder().getRangeURI(pi, logId)));
            }
        }
        IMetadataValue label = ViewerResourceBundle.getTranslations(metadataField);
        IMetadataValue value = SolrSearchIndex.getTranslations(metadataField, doc, (a, b) -> a + "; " + b)
                .orElse(new SimpleMetadataValue(SolrSearchIndex.getSingleFieldStringValue(doc, metadataField)));
        Metadata md = new Metadata(label, value);
        anno.setBody(new FieldListResource(Collections.singletonList(md)));

        return anno;
    }

    /**
     * Create a IIIF Search hit from the field fieldName within the SolrDocumnet doc
     * 
     * @param queryRegex
     * @param fieldName
     * @param doc
     * @return A search hit for a Solr field search
     */
    public SearchHit convertToHit(String queryRegex, String fieldName, SolrDocument doc) {
        SearchHit hit = new SearchHit();
        String mdText = SolrSearchIndex.getMetadataValues(doc, fieldName).stream().collect(Collectors.joining("; "));
        Matcher m = Pattern.compile(AbstractSearchParser.getSingleWordRegex(queryRegex)).matcher(mdText);
        while (m.find()) {
            String match = m.group(1);
            int indexStart = m.start(1);
            int indexEnd = m.end(1);

            String before = AbstractSearchParser.getPrecedingText(mdText, indexStart, Integer.MAX_VALUE);
            String after = AbstractSearchParser.getSucceedingText(mdText, indexEnd, Integer.MAX_VALUE);
            if (!StringUtils.isAllBlank(before, after)) {
                TextQuoteSelector textSelector = new TextQuoteSelector();
                textSelector.setFragment(match);
                if (StringUtils.isNotBlank(before)) {
                    textSelector.setPrefix(before);
                }
                if (StringUtils.isNotBlank(after)) {
                    textSelector.setSuffix(after);
                }
                hit.addSelector(textSelector);
            }
            hit.setMatch(match);
        }
        OpenAnnotation anno = createMetadataAnnotation(fieldName, doc);
        hit.addAnnotation(anno);
        return hit;
    }

    /**
     * Create a IIIF Search hit from a UGC solr document (usually a crowdsouring created comment/metadata
     * 
     * @param queryRegex
     * @param doc
     * @return
     */
    public SearchHit convertToHit(String queryRegex, SolrDocument ugc) {

        SearchHit hit = new SearchHit();
        String mdText = SolrSearchIndex.getMetadataValues(ugc, SolrConstants.UGCTERMS).stream().collect(Collectors.joining("; "));
        String type = SolrSearchIndex.getSingleFieldStringValue(ugc, SolrConstants.UGCTYPE);
        Matcher m = Pattern.compile(AbstractSearchParser.getSingleWordRegex(queryRegex)).matcher(mdText);
        while (m.find()) {
            String match = m.group(1);
            int indexStart = m.start(1);
            int indexEnd = m.end(1);
            if (!match.equals(type)) {
                String before = AbstractSearchParser.getPrecedingText(mdText, indexStart, Integer.MAX_VALUE);
                before = before.replace(type, "").trim();
                String after = AbstractSearchParser.getSucceedingText(mdText, indexEnd, Integer.MAX_VALUE);
                after = after.replace(type, "").trim();
                if (!StringUtils.isAllBlank(before, after)) {
                    TextQuoteSelector textSelector = new TextQuoteSelector();
                    textSelector.setFragment(match);
                    if (StringUtils.isNotBlank(before)) {
                        textSelector.setPrefix(before);
                    }
                    if (StringUtils.isNotBlank(after)) {
                        textSelector.setSuffix(after);
                    }
                    hit.addSelector(textSelector);
                }
            }
            hit.setMatch(match);
            OpenAnnotation anno = getPresentationBuilder().createOpenAnnotation(ugc, true);
            hit.addAnnotation(anno);
        }
        return hit;
    }

    /**
     * @param pi
     * @param results
     * @param comment
     */
    public SearchHit convertToHit(String queryRegex, String pi, Comment comment) {
        SearchHit hit = new SearchHit();

        String text = comment.getDisplayText();
        Matcher m = Pattern.compile(AbstractSearchParser.getSingleWordRegex(queryRegex)).matcher(text);
        while (m.find()) {
            String match = m.group(1);
            int indexStart = m.start(1);
            int indexEnd = m.end(1);

            String before = AbstractSearchParser.getPrecedingText(text, indexStart, Integer.MAX_VALUE);
            String after = AbstractSearchParser.getSucceedingText(text, indexEnd, Integer.MAX_VALUE);
            if (!StringUtils.isAllBlank(before, after)) {
                TextQuoteSelector textSelector = new TextQuoteSelector();
                textSelector.setFragment(match);
                if (StringUtils.isNotBlank(before)) {
                    textSelector.setPrefix(before);
                }
                if (StringUtils.isNotBlank(after)) {
                    textSelector.setSuffix(after);
                }
                hit.addSelector(textSelector);
            }
            IAnnotation anno = createAnnotation(pi, comment);
            hit.addAnnotation(anno);
        }

        return hit;
    }



    /**
     * @param pi
     * @param comment
     */
    public IAnnotation createAnnotation(String pi, Comment comment) {
        OpenAnnotation anno = new OpenAnnotation(getPresentationBuilder().getCommentAnnotationURI(pi, comment.getPage(), comment.getId()));
        anno.setMotivation(Motivation.COMMENTING);
        IResource canvas = createSimpleCanvasResource(pi, comment.getPage());
        anno.setTarget(canvas);
        TextualResource body = new TextualResource(comment.getText());
        anno.setBody(body);
        return anno;
    }

    /**
     * Create a URI-only resource for a page. Either as a {@link SimpleResource} or a {@link SpecificResourceURI} if the page has a width
     * and height
     * 
     * @param pi        PI of the work containing the page
     * @param pageNo    page number (ORDER) of the page
     * @return      A URI to a canvas resource
     */
    private IResource createSimpleCanvasResource(String pi, int pageNo) {
        Dimension pageSize = solrParser.getPageSize(pi, pageNo);
        if (pageSize.getWidth() * pageSize.getHeight() == 0) {
            return new SimpleResource(getPresentationBuilder().getCanvasURI(pi, pageNo));
        } else {
            FragmentSelector selector = new FragmentSelector(new Rectangle(0, 0, pageSize.width, pageSize.height));
            return new SpecificResourceURI(getPresentationBuilder().getCanvasURI(pi, pageNo), selector);
        }
    }

}
