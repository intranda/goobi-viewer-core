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
package io.goobi.viewer.model.iiif.search;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_COMMENT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_METADATA;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_UGC;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.jdom2.JDOMException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.search.SearchHit;
import de.intranda.api.iiif.search.SearchTerm;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.iiif.presentation.v2.builder.AbstractBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.search.model.AnnotationResultList;
import io.goobi.viewer.model.iiif.search.model.SearchTermList;
import io.goobi.viewer.model.iiif.search.parser.AbstractSearchParser;
import io.goobi.viewer.model.iiif.search.parser.AltoSearchParser;
import io.goobi.viewer.model.iiif.search.parser.SolrSearchParser;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Converts resources found in a search to IIIF Search objects
 *
 * @author florian
 */
public class SearchResultConverter {

    private static final Logger logger = LogManager.getLogger(IIIFSearchBuilder.class);

    private static final int MAX_TEXT_LENGTH = 20;

    private final AltoAnnotationBuilder altoBuilder;
    private final AbstractBuilder presentationBuilder;
    private final AltoSearchParser altoParser = new AltoSearchParser();
    private final SolrSearchParser solrParser = new SolrSearchParser();

    private String pi;
    private Integer pageNo;
    private AbstractApiUrlManager urls;

    /**
     * Create a new converter; parameters are used to construct urls or result resources
     *
     * @param requestURI The URI of the search request
     * @param restApiURI The URI of the viewer rest api
     * @param pi The PI of the manifest to search
     * @param pageNo The page number of generated resources
     */
    public SearchResultConverter(AbstractApiUrlManager urls, String pi, Integer pageNo) {
        this.presentationBuilder = new AbstractBuilder(urls) {
        };
        this.altoBuilder = new AltoAnnotationBuilder(urls, "oa");
        this.urls = urls;
        this.pi = pi;
        this.pageNo = pageNo;
    }

    /**
     * <p>
     * Setter for the field <code>pi</code>.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>
     * Setter for the field <code>pageNo</code>.
     * </p>
     *
     * @param pageNo a {@link java.lang.Integer} object.
     */
    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    /**
     * <p>
     * Getter for the field <code>pageNo</code>.
     * </p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getPageNo() {
        return pageNo;
    }

    /**
     * <p>
     * Getter for the field <code>presentationBuilder</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.AbstractBuilder} object.
     */
    public AbstractBuilder getPresentationBuilder() {
        return presentationBuilder;
    };

    /**
     * Generates a search hit from a {@link io.goobi.viewer.model.annotation.comments.Comment}
     *
     * @param pi The PI of the work containing the comment
     * @param queryRegex The regex matching the search terms
     * @param comment The comment containing the search terms
     * @return a {@link de.intranda.api.iiif.search.SearchHit}
     */
    public SearchHit convertCommentToHit(String queryRegex, String pi, Comment comment) {
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
            hit.setMatch(match);
            IAnnotation anno = createAnnotation(pi, comment);
            hit.addAnnotation(anno);
        }

        return hit;
    }

    /**
     * Create a IIIF Search hit from a UGC solr document (usually a crowdsouring created comment/metadata)
     *
     * @param queryRegex a {@link java.lang.String} object.
     * @return A search hit matching the queryRegex within the given UGC SolrDocument
     * @param ugc a {@link org.apache.solr.common.SolrDocument} object.
     */
    public SearchHit convertUGCToHit(String queryRegex, SolrDocument ugc) {

        SearchHit hit = new SearchHit();
        String mdText = SolrTools.getMetadataValues(ugc, SolrConstants.UGCTERMS).stream().collect(Collectors.joining("; "));
        String type = SolrTools.getSingleFieldStringValue(ugc, SolrConstants.UGCTYPE);
        Matcher m = Pattern.compile(AbstractSearchParser.getSingleWordRegex(queryRegex)).matcher(mdText);
        while (m.find()) {
            String match = m.group(1);
            int indexStart = m.start(1);
            int indexEnd = m.end(1);
            if (!match.equals(type)) {
                String before = AbstractSearchParser.getPrecedingText(mdText, indexStart, Integer.MAX_VALUE);
                before = before.replace(type, "");
                String after = AbstractSearchParser.getSucceedingText(mdText, indexEnd, Integer.MAX_VALUE);
                after = after.replace(type, "");
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
            OpenAnnotation anno = new OpenAnnotationBuilder(urls).createUGCOpenAnnotation(ugc, true);
            hit.addAnnotation(anno);
        }
        return hit;
    }

    /**
     * Create a IIIF Search hit from the field fieldName within the SolrDocumnet doc
     *
     * @param queryRegex a {@link java.lang.String} object.
     * @param fieldName a {@link java.lang.String} object.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @return A search hit for a Solr field search
     */
    public SearchHit convertMetadataToHit(String queryRegex, String fieldName, SolrDocument doc) {
        SearchHit hit = new SearchHit();
        String mdText = SolrTools.getMetadataValues(doc, fieldName).stream().collect(Collectors.joining("; "));
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
        OpenAnnotation anno = createAnnotation(fieldName, doc);
        hit.addAnnotation(anno);
        return hit;
    }

    /**
     * Create annotations for all matches of the given query within the given alto file
     *
     * @param doc The {@link de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument}
     * @param query a regex; each match of the query within the alto document creates a {@link de.intranda.api.iiif.search.SearchHit} with one or more
     *            annotations referencing alto word or line elements
     * @return A result list containing hits for each mach of the query and annotations containing the hits
     * @throws JDOMException
     * @throws IOException
     */
    public AnnotationResultList getAnnotationsFromAlto(Path path, String query) throws IOException, JDOMException {
        AnnotationResultList results = new AnnotationResultList();
        AltoSearchParser parser = new AltoSearchParser();
        AltoDocument doc = AltoDocument.getDocumentFromFile(path.toFile());
        List<Word> words = parser.getWords(doc);
        if (!words.isEmpty()) {
            List<List<Word>> matches = parser.findWordMatches(words, query);
            for (List<Word> wordsHit : matches) {
                SearchHit hit = convertAltoToHit(wordsHit);
                results.add(hit);
            }
        } else {
            List<Line> lines = parser.getLines(doc);
            if (!lines.isEmpty()) {
                Map<Range<Integer>, List<Line>> hits = parser.findLineMatches(lines, query);
                for (Range<Integer> position : hits.keySet()) {
                    List<Line> containingLines = hits.get(position);
                    SearchHit hit = createAltoHit(lines, position, containingLines);
                    results.add(hit);
                }
            }
        }
        return results;
    }

    /**
     * Create annotations for all matches of the given query within the given text file Returns only a partial result if the firstIndex is larger than
     * 0 and numHits is smaller than the total number of hits
     *
     * @param text the text to search
     * @param pi the PI of the work containing the annotations
     * @param pageNo The page number of the canvas containing the annotations
     * @param query The regex matching all hits in the text file
     * @param previousHitCount The number of hits already found in previous pages
     * @param firstIndex The index of the first overal hit to be returned in the result itself. Larger than 0 only for later pages within a paged
     *            annotation collection
     * @param numHits The maximal number of hits to be returned in the result itself. This is the maximal size of the hit list within a single result
     *            page of a paged annotation collection
     * @return A result list containing all matching hits within the range set by previousHitCount, firstIndex and numHits
     */
    public AnnotationResultList getAnnotationsFromFulltext(String text, String pi, Integer pageNo, String query, long previousHitCount,
            int firstIndex, int numHits) {
        AnnotationResultList results = new AnnotationResultList();
        long firstPageHitIndex = previousHitCount;
        long lastPageHitIndex = firstPageHitIndex;
        if (firstIndex <= lastPageHitIndex && firstIndex + numHits - 1 >= firstPageHitIndex) {
            results.add(createFulltextHit(query, text, pi, pageNo));
        }
        return results;
    }

    /**
     * Get all matches to the given regex in the fieldsToSearch of the given doc as {@link SearchTerm SearchTerms}
     *
     * @param regex A regex matching all text wich should be returned as a searchTerm
     * @param doc The document within to search
     * @param fieldsToSearch The fields to search for the regex
     * @param searchMotivation The motivation to be set for the search url of the searchTerms
     * @return A list of search terms
     */
    public SearchTermList getSearchTerms(String regex, SolrDocument doc, List<String> fieldsToSearch, List<String> searchMotivation) {
        SearchTermList terms = new SearchTermList();
        for (String field : fieldsToSearch) {
            String value = SolrTools.getSingleFieldStringValue(doc, field);
            terms.addAll(getSearchTerms(regex, value, searchMotivation));
        }
        return terms;
    }

    /**
     * Get all matches to the given regex in the given value as {@link SearchTerm SearchTerms}
     *
     * @param regex A regex matching all text wich should be returned as a searchTerm
     * @param value The text to be searched with the regex
     * @param searchMotivation The motivation to be set for the search url of the searchTerms
     * @return A list of search terms
     */
    public SearchTermList getSearchTerms(String regex, String value, List<String> searchMotivation) {
        SearchTermList terms = new SearchTermList();
        String escapedRegex = Pattern.quote(regex);
        String wordRegex = AbstractSearchParser.getSingleWordRegex(escapedRegex);
        if (StringUtils.isNotBlank(value)) {
            Matcher matcher = Pattern.compile(wordRegex).matcher(value);
            while (matcher.find()) {
                String match = matcher.group(1);
                SearchTerm term = new SearchTerm(getPresentationBuilder().getSearchURI(getPi(), match, searchMotivation), match, 1);
                terms.add(term);
            }
        }
        return terms;
    }

    /**
     * Convert a list of also word elements to a search hit, containing an annotation for each word in the list
     *
     * @param altoElements A list of ALTO word elements
     * @return A hit of the combined words
     */
    public SearchHit convertAltoToHit(List<Word> altoElements) {
        SearchHit hit = new SearchHit();
        hit.setAnnotations(altoElements.stream().map(this::createAnnotation).collect(Collectors.toList()));
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

    /**
     * Creates a {@link de.intranda.api.iiif.search.SearchHit} of the text within the given position within the given lines.
     *
     * @param lines The lines containing the hit
     * @param position A range covering character positions of the matched text. the position is relative to the entire text of the given lines
     * @param containingLines The lines to be included as annotations in the hit
     * @return A search hit containing the match at the given position
     */
    public SearchHit createAltoHit(List<Line> lines, Range<Integer> position, List<Line> containingLines) {
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
        hit.setAnnotations(containingLines.stream().map(this::createAnnotation).collect(Collectors.toList()));
        return hit;
    }

    /**
     * Create a SearchHit for the given text. Each match of the given queryRegex in the text is included as a TextQuoteSelector in the hit
     *
     * @param queryRegex The regex matching the hit
     * @param text The text to search
     * @param pi The PI of the manifest containing the annotations
     * @param pageNo The order of the canvas containing the annotations
     * @return A search hit containing a TextQuoteSelector for each match of the regex and a single annotations covering the entire text
     */
    public SearchHit createFulltextHit(String queryRegex, String text, String pi, Integer pageNo) {

        SearchHit hit = new SearchHit();

        String regex = AbstractSearchParser.getSingleWordRegex(queryRegex);

        Matcher m = Pattern.compile(regex).matcher(text);
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
            hit.addSelector(selector);
        }

        IResource canvas = createSimpleCanvasResource(pi, pageNo);
        IAnnotation pageAnnotation = createAnnotation(text, canvas);
        hit.addAnnotation(pageAnnotation);

        return hit;
    }

    /**
     * Create an annotation for the given metadata field in the given doc
     *
     * @param metadataField
     * @param doc
     * @return
     */
    private OpenAnnotation createAnnotation(String metadataField, SolrDocument doc) {
        String pi = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
        String logId = SolrTools.getSingleFieldStringValue(doc, SolrConstants.LOGID);
        boolean isWork = SolrTools.getSingleFieldBooleanValue(doc, SolrConstants.ISWORK);
        Integer thumbPageNo = SolrTools.getSingleFieldIntegerValue(doc, SolrConstants.THUMBPAGENO);
        OpenAnnotation anno = new OpenAnnotation(getMetadataAnnotationURI(pi, logId, metadataField));
        anno.setMotivation(Motivation.DESCRIBING);
        if (thumbPageNo != null) {
            anno.setTarget(createSimpleCanvasResource(pi, thumbPageNo));
        } else {
            if (Boolean.TRUE.equals(isWork)) {
                anno.setTarget(new SimpleResource(getPresentationBuilder().getManifestURI(pi)));
            } else {
                anno.setTarget(new SimpleResource(getPresentationBuilder().getRangeURI(pi, logId)));
            }
        }
        IMetadataValue label = ViewerResourceBundle.getTranslations(metadataField);
        IMetadataValue value = SolrTools.getTranslations(metadataField, doc, (a, b) -> a + "; " + b)
                .orElse(new SimpleMetadataValue(SolrTools.getSingleFieldStringValue(doc, metadataField)));
        Metadata md = new Metadata(label, value);
        anno.setBody(new FieldListResource(Collections.singletonList(md)));

        return anno;
    }

    /**
     * Create an annotation from an ALTO element
     *
     * @param altoElement The alto xml element
     * @return An annotation representing the element
     */
    private IAnnotation createAnnotation(GeometricData altoElement) {
        return altoBuilder.createAnnotation(altoElement, getPi(), getPageNo(), createSimpleCanvasResource(getPi(), getPageNo()), true);
    }

    /**
     * create a text annotation with the given text in the given canvas
     */
    private IAnnotation createAnnotation(String text, IResource canvas) {
        AbstractAnnotation anno = new OpenAnnotation(getPlaintextAnnotationURI(pi, pageNo));
        anno.setMotivation(Motivation.PAINTING);
        anno.setTarget(canvas);
        TextualResource body = new TextualResource(text);
        anno.setBody(body);
        return anno;
    }

    private IAnnotation createAnnotation(String pi, Comment comment) {
        OpenAnnotation anno = new OpenAnnotation(getCommentAnnotationURI(comment.getId().toString()));
        anno.setMotivation(Motivation.COMMENTING);
        IResource canvas = createSimpleCanvasResource(pi, comment.getTargetPageOrder());
        anno.setTarget(canvas);
        TextualResource body = new TextualResource(comment.getContentString());
        anno.setBody(body);
        return anno;
    }

    /**
     * Create a URI-only resource for a page. Either as a {@link SimpleResource} or a {@link SpecificResourceURI} if the page has a width and height
     *
     * @param pi PI of the work containing the page
     * @param pageNo page number (ORDER) of the page
     * @return A URI to a canvas resource
     */
    private IResource createSimpleCanvasResource(String pi, int pageNo) {
        Dimension pageSize = solrParser.getPageSize(pi, pageNo);
        if (pageSize.getWidth() * pageSize.getHeight() == 0) {
            return new SimpleResource(getPresentationBuilder().getCanvasURI(pi, pageNo));
        }
        FragmentSelector selector = new FragmentSelector(new Rectangle(0, 0, pageSize.width, pageSize.height));
        return new SpecificResourceURI(getPresentationBuilder().getCanvasURI(pi, pageNo), selector);
    }

    /**
     * Return a URI to an annotation list for the given pi and pageNo
     */
    private URI getAnnotationListURI(String pi, Integer pageNo, AnnotationType type) {
        if (pageNo != null) {
            return presentationBuilder.getAnnotationListURI(pi, pageNo, type, true);
        }
        return presentationBuilder.getAnnotationListURI(pi, type);
    }

    private URI getMetadataAnnotationURI(String pi, String logId, String metadataField) {
        return URI.create(urls.path(ANNOTATIONS, ANNOTATIONS_METADATA).params(pi, logId, metadataField).query("format", "oa").build());
    }

    private URI getPlaintextAnnotationURI(String pi, Integer pageNo) {
        return URI.create(urls.path(ANNOTATIONS, ANNOTATIONS_PLAINTEXT).params(pi, pageNo).query("format", "oa").build());
    }

    private URI getAltoAnnotationURI(String pi, Integer pageNo, String elementId) {
        return URI.create(urls.path(ANNOTATIONS, ANNOTATIONS_ALTO).params(pi, pageNo, elementId).query("format", "oa").build());
    }

    private URI getUGCAnnotationURI(String id) {
        return URI.create(urls.path(ANNOTATIONS, ANNOTATIONS_UGC).params(id).query("format", "oa").build());
    }

    private URI getCommentAnnotationURI(String id) {
        return URI.create(urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(id).query("format", "oa").build());
    }

}
