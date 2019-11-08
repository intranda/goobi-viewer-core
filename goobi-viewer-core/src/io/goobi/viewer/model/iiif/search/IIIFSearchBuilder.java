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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.search.SearchResult;
import de.intranda.api.iiif.search.SearchResultLayer;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.iiif.presentation.builder.AbstractBuilder;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * Creates a IIIF Search API v1.0 response as {@link SearchResult}
 * 
 * @author florian
 *
 */
public class IIIFSearchBuilder {

    private static final Logger logger = LoggerFactory.getLogger(IIIFSearchBuilder.class);

    private static final List<String> FULLTEXTFIELDLIST =
            Arrays.asList(new String[] { SolrConstants.FILENAME_ALTO, SolrConstants.FILENAME_FULLTEXT, SolrConstants.ORDER });

    private final String query;
    private final String pi;
    private final AbstractBuilder presentationBuilder;
    private List<String> motivation = new ArrayList<>();
    private String user = null;
    private String date = null;
    private int page = 1;
    private int hitsPerPage = 20;

    private final String requestURI;

    /**
     * Initializes the builder with all required parameters
     * 
     * @param requestURI    The request url, including all query parameters
     * @param query the query string
     * @param pi    the pi of the manifest to search
     */
    public IIIFSearchBuilder(URI requestURI, String query, String pi) {
        this.requestURI = requestURI.toString().replaceAll("&page=\\d+", "");
        if(query != null) {            
            query = query.replace("+", " ");
        }
        this.query = query;
        this.pi = pi;
        this.presentationBuilder =
                new AbstractBuilder(URI.create(this.requestURI), URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl())) {

                };
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param motivation the motivation to set
     */
    public IIIFSearchBuilder setMotivation(String motivation) {
        if (StringUtils.isNotBlank(motivation)) {
            motivation = motivation.replace("+", " ");
            this.motivation = Arrays.asList(StringUtils.split(motivation, " "));
        }
        return this;
    }

    /**
     * @return the motivation
     */
    public List<String> getMotivation() {
        return motivation;
    }

    /**
     * @param user the user to set
     */
    public IIIFSearchBuilder setUser(String user) {
        this.user = user;
        return this;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param date the date to set
     */
    public IIIFSearchBuilder setDate(String date) {
        this.date = date;
        return this;
    }

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * @param page the page to set
     */
    public IIIFSearchBuilder setPage(Integer page) {
        if (page != null) {
            this.page = page;
        }
        return this;
    }

    /**
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * @return the hitsPerPage
     */
    public int getHitsPerPage() {
        return hitsPerPage;
    }

    /**
     * @param hitsPerPage the hitsPerPage to set
     */
    public IIIFSearchBuilder setHitsPerPage(int hitsPerPage) {
        this.hitsPerPage = hitsPerPage;
        return this;
    }

    /**
     * Creates a {@link SearchResult} containing annotations matching {@link #getQuery()} within {@link #getPi()}.
     * The answer may contain more than {@link #getHitsPerPage()} hits if more than one motivation is searched, but no more than
     * {@link #getHitsPerPage()} hits per motivation.
     * 
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public SearchResult build() throws PresentationException, IndexUnreachableException {

        AnnotationResultList resultList = new AnnotationResultList();

        long mostHits = 0;
        if(StringUtils.isNotBlank(query)) {
            if (motivation.isEmpty() || motivation.contains("painting")) {
                AnnotationResultList fulltextAnnotations = searchFulltext(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(fulltextAnnotations);
                mostHits = Math.max(mostHits, fulltextAnnotations.numHits);
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("describing")) {
                AnnotationResultList annotations = searchAnnotations(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(annotations);
                mostHits = Math.max(mostHits, annotations.numHits);
    
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("commenting")) {
                AnnotationResultList annotations = searchComments(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(annotations);
                mostHits = Math.max(mostHits, annotations.numHits);
            }
        }
        
        int lastPageNo = 1 + (int) mostHits / getHitsPerPage();

        SearchResult searchResult = new SearchResult(getURI(getPage()));
        searchResult.setResources(resultList.hits);
        searchResult.setStartIndex(getFirstHitIndex(getPage()));
        
        if(getPage() > 1) {
            searchResult.setPrev(getURI(getPage()-1));
        }
        if(getPage() < lastPageNo) {
            searchResult.setNext(getURI(getPage()+1));
        }
        SearchResultLayer layer = new SearchResultLayer();
        layer.setTotal(resultList.numHits);
        layer.setIgnored(getIgnoredParameterList());
        layer.setFirst(getURI(1));
        layer.setLast(getURI(lastPageNo));
        searchResult.setWithin(layer);

        return searchResult;
    }

    /**
     * @return
     */
    private List<String> getIgnoredParameterList() {
        List<String> ignored = new ArrayList<>();
        if (StringUtils.isNotBlank(getUser())) {
            ignored.add("user");
        }
        if (StringUtils.isNotBlank(getDate())) {
            ignored.add("date");
        }
        return ignored;
    }

    /**
     * @return
     */
    private String getQueryRegex(String query) {
        String queryRegex = query.replace("*", "[\\w\\d-]*").replaceAll("\\s+", "|");
        return "(?i)" + queryRegex;
    }

    /**
     * @param queryRegex
     * @param pi2
     * @param firstHitIndex
     * @param hitsPerPage2
     * @return
     */
    private AnnotationResultList searchComments(String query, String pi, int firstHitIndex, int hitsPerPage) {

        AnnotationResultList results = new AnnotationResultList();
        String queryRegex = getQueryRegex(query);

        try {
            List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork(pi, false);
            comments = comments.stream().filter(c -> c.getText().matches(getContainedWordRegex(queryRegex))).collect(Collectors.toList());
            results.numHits = comments.size();
            if (firstHitIndex < comments.size()) {
                comments = comments.subList(firstHitIndex, Math.min(firstHitIndex + hitsPerPage, comments.size()));
                for (Comment comment : comments) {
                    OpenAnnotation anno = new OpenAnnotation(presentationBuilder.getCommentAnnotationURI(pi, comment.getPage(), comment.getId()));
                    anno.setMotivation(Motivation.COMMENTING);
                    Canvas canvas = new Canvas(presentationBuilder.getCanvasURI(pi, comment.getPage()));
                    anno.setTarget(canvas);
                    TextualResource body = new TextualResource(comment.getText());
                    anno.setBody(body);
                    results.hits.add(anno);
                }
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    /**
     * @param queryRegex
     * @param pi2
     * @param firstHitIndex
     * @param hitsPerPage2
     * @return
     */
    private AnnotationResultList searchAnnotations(String query, String pi, int firstHitIndex, int hitsPerPage) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +UGCTERMS:").append(query);

        AnnotationResultList results = new AnnotationResultList();
        try {
            StringPair sortField = new StringPair(SolrConstants.ORDER, "asc");
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, Collections.singletonList(sortField),
                            Arrays.asList(AbstractBuilder.UGC_SOLR_FIELDS));
            results.numHits = docList.size();
            if (firstHitIndex < docList.size()) {
                List<SolrDocument> filteredDocList = docList.subList(firstHitIndex, Math.min(firstHitIndex + hitsPerPage, docList.size()));
                for (SolrDocument doc : filteredDocList) {
                    OpenAnnotation anno = presentationBuilder.createOpenAnnotation(pi, doc);
                    results.hits.add(anno);
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    /**
     * @param page2
     * @return
     */
    private int getFirstHitIndex(int pageNo) {
        return (pageNo - 1) * getHitsPerPage();
    }

    /**
     * @param query2
     * @param pi2
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private AnnotationResultList searchFulltext(String query, String pi, int firstIndex, int numHits)
            throws PresentationException, IndexUnreachableException {

        //replace search wildcards with word character regex and replace whitespaces with '|' to facilitate OR search
        String queryRegex = getQueryRegex(query);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:PAGE");
        queryBuilder.append(" +FULLTEXTAVAILABLE:true");
        queryBuilder.append(" +FULLTEXT:").append(query);

        AnnotationResultList results = new AnnotationResultList();

        StringPair sortField = new StringPair(SolrConstants.ORDER, "asc");
        //        QueryResponse response = DataManager.getInstance().getSearchIndex().search(queryBuilder.toString(), (page-1)*getHitsPerPage(), getHitsPerPage(), Collections.singletonList(sortField), null, FULLTEXTFIELDLIST);
        SolrDocumentList docList = DataManager.getInstance()
                .getSearchIndex()
                .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, Collections.singletonList(sortField), FULLTEXTFIELDLIST);
        for (SolrDocument doc : docList) {
            Path altoFile = getPath(pi, SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.FILENAME_ALTO));
            Path fulltextFile = getPath(pi, SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.FILENAME_FULLTEXT));
            Integer pageNo = SolrSearchIndex.getAsInt(doc.getFieldValue(SolrConstants.ORDER));
            if (altoFile != null && Files.exists(altoFile)) {
                results.add(getAnnotationsFromAlto(altoFile, pi, pageNo, queryRegex, results.numHits, firstIndex, numHits));
            } else if (fulltextFile != null && Files.exists(fulltextFile)) {
                results.add(getAnnotationsFromFulltext(fulltextFile, "utf-8", pi, pageNo, queryRegex, results.numHits, firstIndex, numHits));
            }
        }
        return results;
    }

    /**
     * @param query2
     * @return
     */
    private List<String> getQueryTerms(String query) {
        String[] tokens = StringUtils.split(query, " ");
        return Arrays.asList(tokens);
    }

    /**
     * @param altoFile
     * @param pi
     * @param pageNo
     * @param query
     * @param firstIndex
     * @param numHits
     * @param results
     */
    private AnnotationResultList getAnnotationsFromAlto(Path altoFile, String pi, Integer pageNo, String query, long previousHitCount, int firstIndex,
            int numHits) {
        AnnotationResultList results = new AnnotationResultList();
        try {
            List<GeometricData> words = getMatchingAltoElements(query, altoFile);
            long firstPageHitIndex = previousHitCount;
            long lastPageHitIndex = firstPageHitIndex + words.size() - 1;
            results.numHits = words.size();
            if (firstIndex <= lastPageHitIndex && firstIndex + numHits - 1 >= firstPageHitIndex) {
                words = words.stream().skip(Math.max(0, firstIndex - firstPageHitIndex)).limit(numHits).collect(Collectors.toList());
                Canvas canvas = new Canvas(presentationBuilder.getCanvasURI(pi, pageNo));
                URI baseURI = presentationBuilder.getAnnotationListURI(pi, pageNo, AnnotationType.ALTO);
                List<IAnnotation> pageAnnotations = new AltoAnnotationBuilder().createAnnotations(words, canvas, baseURI.toString(), true);
                results.hits.addAll(pageAnnotations);
            }
        } catch (JDOMException | IOException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    /**
     * @param query
     * @param altoFile
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    private List<GeometricData> getMatchingAltoElements(String query, Path altoFile) throws IOException, JDOMException {
        AltoDocument altoDoc = AltoDocument.getDocumentFromFile(altoFile.toFile());
        List<GeometricData> words = new ArrayList<>();
        words = altoDoc.getFirstPage()
                .getAllWordsAsList()
                .stream()
                .filter(w -> getActualContent(w).matches(query))
                .map(w -> (GeometricData) w)
                .collect(Collectors.toList());
        if (words.isEmpty()) {
            words = altoDoc.getFirstPage()
                    .getAllLinesAsList()
                    .stream()
                    .filter(l -> l.getContent().matches(getSingleWordRegex(query)))
                    .map(w -> (GeometricData) w)
                    .collect(Collectors.toList());
        }
        return words;
    }

    /**
     * @param query
     * @return a regex matching a single word matching the given query regex
     */
    private String getSingleWordRegex(String query) {
        return "(^|.*\\s)(" + query + ")($|\\s.*|[.:,;])";
    }

    /**
     * 
     * @param query
     * @return a regex matching any text containing the given query regex as single word
     */
    private String getContainedWordRegex(String query) {
        return "(?i)([\\w\\W]*)(^|.*\\s)(" + query + ")($|\\s.*|[.:,;])([\\w\\W]*)";
    }

    private AnnotationResultList getAnnotationsFromFulltext(Path textFile, String textFileEncoding, String pi, Integer pageNo, String query,
            long previousHitCount, int firstIndex, int numHits) {
        AnnotationResultList results = new AnnotationResultList();
        try {
            String text = new String(Files.readAllBytes(textFile), textFileEncoding);
            long firstPageHitIndex = previousHitCount;
            long lastPageHitIndex = firstPageHitIndex;
            results.numHits = 1;
            if (firstIndex <= lastPageHitIndex && firstIndex + numHits - 1 >= firstPageHitIndex) {
                Canvas canvas = new Canvas(presentationBuilder.getCanvasURI(pi, pageNo));
                URI baseURI = presentationBuilder.getAnnotationListURI(pi, pageNo, AnnotationType.ALTO);
                IAnnotation pageAnnotation = createAnnotation(text, canvas, baseURI.toString());
                results.hits.add(pageAnnotation);
            }
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    private IAnnotation createAnnotation(String text, Canvas canvas, String baseUrl) {
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
     * @param query2
     * @param text
     * @return
     */
    private List<String> getMatchingWords(String query, String text) {
        Matcher matcher = Pattern.compile(getSingleWordRegex(query)).matcher(text);
        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            results.add(matcher.group().trim());
        }
        return results;
    }

    /**
     * @param w
     * @return
     */
    private String getActualContent(GeometricData w) {
        if (w instanceof Word) {
            return ((Word) w).getSubsContent();
        } else {
            return w.getContent();
        }
    }

    public Path getPath(String pi, String filename) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(filename)) {
            return null;
        }
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        Path filePath = Paths.get(Helper.getRepositoryPath(dataRepository), filename);

        return filePath;
    }

    /**
     * @return
     */
    private URI getURI(int page) {
        return URI.create(requestURI + "&page=" + page);
    }

    /**
     * Utility class 
     * 
     * @author florian
     *
     */
    private static class AnnotationResultList {
        public long numHits;
        public final List<IAnnotation> hits;

        public AnnotationResultList() {
            this.hits = new ArrayList<>();
        }

        /**
         * @param annotationsFromAlto
         */
        public void add(AnnotationResultList partialResults) {
            numHits += partialResults.numHits;
            hits.addAll(partialResults.hits);

        }

        public AnnotationResultList(long numHits, List<IAnnotation> hits) {
            this.numHits = numHits;
            this.hits = hits;
        }

    }

}
