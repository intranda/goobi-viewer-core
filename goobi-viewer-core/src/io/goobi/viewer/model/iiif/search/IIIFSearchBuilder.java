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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import de.intranda.api.annotation.FieldListResource;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchResult;
import de.intranda.api.iiif.search.SearchResultLayer;
import de.intranda.api.iiif.search.SearchTerm;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
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
    private String min = null;
    private int page = 1;
    private int hitsPerPage = 20;

    private final String requestURI;

    /**
     * Initializes the builder with all required parameters
     * 
     * @param requestURI The request url, including all query parameters
     * @param query the query string
     * @param pi the pi of the manifest to search
     */
    public IIIFSearchBuilder(URI requestURI, String query, String pi) {
        this.requestURI = requestURI.toString().replaceAll("&page=\\d+", "");
        if (query != null) {
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
     * @return the min
     */
    public String getMin() {
        return min;
    }
    
    /**
     * @param min the min to set
     */
    public IIIFSearchBuilder setMin(String min) {
        this.min = min;
        return this;
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
     * Creates a {@link SearchResult} containing annotations matching {@link #getQuery()} within {@link #getPi()}. The answer may contain more than
     * {@link #getHitsPerPage()} hits if more than one motivation is searched, but no more than {@link #getHitsPerPage()} hits per motivation.
     * 
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public SearchResult build() throws PresentationException, IndexUnreachableException {

        AnnotationResultList resultList = new AnnotationResultList();

        long mostHits = 0;
        if (StringUtils.isNotBlank(query)) {
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
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("describing")) {
                AnnotationResultList metadata = searchMetadata(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(metadata);
                mostHits = Math.max(mostHits, metadata.numHits);

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
        if (getPage() < lastPageNo) {
            searchResult.setNext(getURI(getPage() + 1));
        }
        SearchResultLayer layer = new SearchResultLayer();
        layer.setTotal(resultList.numHits);
        layer.setIgnored(getIgnoredParameterList());
        layer.setFirst(getURI(1));
        layer.setLast(getURI(lastPageNo));
        searchResult.setWithin(layer);

        return searchResult;
    }

    public AutoSuggestResult buildAutoSuggest() throws PresentationException, IndexUnreachableException {

        SearchTermList terms = new SearchTermList();
        if (StringUtils.isNotBlank(query)) {
            if (motivation.isEmpty() || motivation.contains("painting")) {
                //add terms from fulltext?
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("describing")) {
                terms.addAll(autoSuggestAnnotations(query, getPi()));
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("describing")) {
                terms.addAll(autoSuggestMetadata(query, getPi()));
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("commenting")) {
                terms.addAll(autoSuggestComments(query, getPi()));
            }
        }

        AutoSuggestResult result = new AutoSuggestResult(presentationBuilder.getAutoSuggestURI(getPi(), getQuery(), getMotivation()));
        result.setIgnored(getIgnoredParameterList());
        result.setTerms(terms);
        return result;
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
        if (StringUtils.isNotBlank(getMin())) {
            ignored.add("min");
        }
        return ignored;
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

    private SearchTermList autoSuggestComments(String query, String pi) {

        SearchTermList terms = new SearchTermList();
        String queryRegex = getAutoSuggestRegex(query);

        try {
            List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork(pi, false);
            comments = comments.stream().filter(c -> c.getText().matches(getContainedWordRegex(queryRegex))).collect(Collectors.toList());
            for (Comment comment : comments) {
                terms.addAll(getSearchTerms(queryRegex, comment.getText()));
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    private AnnotationResultList searchMetadata(String query, String pi, int firstHitIndex, int hitsPerPage) {

        AnnotationResultList results = new AnnotationResultList();
        List<String> searchFields = getSearchFields();
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();

        if (searchFields.isEmpty()) {
            return results;
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:DOCSTRCT");
        queryBuilder.append(" +( ");
        for (String field : searchFields) {
            queryBuilder.append(field).append(":").append(query).append(" ");
        }
        queryBuilder.append(")");

        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getDocStructSortFields(), presentationBuilder.getSolrFieldList());
            for (SolrDocument doc : docList) {
                Map<String, List<String>> fieldNames = SolrSearchIndex.getFieldValueMap(doc);
                for (String fieldName : fieldNames.keySet()) {
                    if (fieldNameMatches(fieldName, displayFields)) {
                        String fieldValue = fieldNames.get(fieldName).stream().collect(Collectors.joining(" "));
                        if (fieldValue.matches(getContainedWordRegex(getQueryRegex(query)))) {
                            results.numHits += 1;
                            if (firstHitIndex < results.numHits && firstHitIndex + hitsPerPage >= results.numHits) {
                                OpenAnnotation anno = createMetadataAnnotation(fieldName, doc);
                                results.hits.add(anno);
                            }
                        }
                    }
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }
    
    private SearchTermList autoSuggestMetadata(String query, String pi) {

        SearchTermList terms = new SearchTermList();
        List<String> searchFields = getSearchFields();
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();

        if (searchFields.isEmpty()) {
            return terms;
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:DOCSTRCT");
        queryBuilder.append(" +( ");
        for (String field : searchFields) {
            queryBuilder.append(field).append(":").append(query).append("* ");
        }
        queryBuilder.append(")");

        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getDocStructSortFields(), presentationBuilder.getSolrFieldList());
            for (SolrDocument doc : docList) {
                Map<String, List<String>> fieldNames = SolrSearchIndex.getFieldValueMap(doc);
                for (String fieldName : fieldNames.keySet()) {
                    if (fieldNameMatches(fieldName, displayFields)) {
                        String fieldValue = fieldNames.get(fieldName).stream().collect(Collectors.joining(" "));
                        if (fieldValue.matches(getContainedWordRegex(getAutoSuggestRegex(query)))) {
                            terms.addAll(getSearchTerms(getAutoSuggestRegex(query), fieldValue));
                        }
                    }
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    private List<StringPair> getFieldList(SolrDocument doc) {
        List<StringPair> list = new ArrayList<>();
        Map<String, List<String>> fields = SolrSearchIndex.getFieldValueMap(doc);
        for (String fieldName : fields.keySet()) {
            String fieldValue = SolrSearchIndex.getMetadataValues(doc, fieldName).stream().collect(Collectors.joining(" "));
            StringPair pair = new StringPair(fieldName, fieldValue);
            list.add(pair);
        }
        return list;
    }

    /**
     * Test if the given fieldName is included in the configuredFields or matches any of the contained wildcard fieldNames
     * 
     * @param fieldName
     * @param iiifDescriptionFields
     * @return
     */
    private boolean fieldNameMatches(String fieldName, List<String> configuredFields) {
        for (String configuredField : configuredFields) {
            if (configuredField.contains("*")) {
                String fieldRegex = getQueryRegex(configuredField);
                if (fieldName.matches(fieldRegex)) {
                    return true;
                }
            } else if (fieldName.equalsIgnoreCase(configuredField)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param doc
     * @return
     */
    private OpenAnnotation createMetadataAnnotation(String metadataField, SolrDocument doc) {
        String iddoc = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
        String pi = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
        String logId = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.LOGID);
        Boolean isWork = SolrSearchIndex.getSingleFieldBooleanValue(doc, SolrConstants.ISWORK);
        OpenAnnotation anno = new OpenAnnotation(presentationBuilder.getAnnotationURI(pi, AnnotationType.METADATA, iddoc));
        anno.setMotivation(Motivation.DESCRIBING);
        if (Boolean.TRUE.equals(isWork)) {
            anno.setTarget(new SimpleResource(presentationBuilder.getManifestURI(pi)));
        } else {
            anno.setTarget(new SimpleResource(presentationBuilder.getRangeURI(pi, logId)));
        }
        IMetadataValue label = ViewerResourceBundle.getTranslations(metadataField);
        IMetadataValue value = SolrSearchIndex.getTranslations(metadataField, doc, (a, b) -> a + "; " + b)
                .orElse(new SimpleMetadataValue(SolrSearchIndex.getSingleFieldStringValue(doc, metadataField)));
        Metadata md = new Metadata(label, value);
        anno.setBody(new FieldListResource(Collections.singletonList(md)));

        return anno;
    }

    /**
     * @return
     */
    public List<StringPair> getPageSortFields() {
        StringPair sortField = new StringPair(SolrConstants.ORDER, "asc");
        return Collections.singletonList(sortField);
    }

    /**
     * @return
     */
    public List<StringPair> getDocStructSortFields() {
        StringPair sortField1 = new StringPair(SolrConstants.THUMBPAGENO, "asc");
        StringPair sortField2 = new StringPair(SolrConstants.ISANCHOR, "asc");
        StringPair sortField3 = new StringPair(SolrConstants.ISWORK, "asc");
        List<StringPair> pairs = new ArrayList<>();
        pairs.add(sortField1);
        pairs.add(sortField2);
        pairs.add(sortField3);
        return pairs;
    }

    /**
     * @return
     */
    public List<String> getSearchFields() {
        List<String> configuredFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
        if (configuredFields.stream().anyMatch(field -> field.contains("*"))) {
            configuredFields = configuredFields.stream().filter(field -> !field.contains("*")).collect(Collectors.toList());
            configuredFields.add(SolrConstants.DEFAULT);
        }
        return configuredFields;
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
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getPageSortFields(), Arrays.asList(AbstractBuilder.UGC_SOLR_FIELDS));
            results.numHits = docList.size();
            if (firstHitIndex < docList.size()) {
                List<SolrDocument> filteredDocList = docList.subList(firstHitIndex, Math.min(firstHitIndex + hitsPerPage, docList.size()));
                for (SolrDocument doc : filteredDocList) {
                    OpenAnnotation anno = presentationBuilder.createOpenAnnotation(pi, doc, true);
                    results.hits.add(anno);
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    private SearchTermList autoSuggestAnnotations(String query, String pi) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +UGCTERMS:").append(query).append("*");

        SearchTermList terms = new SearchTermList();
        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getPageSortFields(), Arrays.asList(AbstractBuilder.UGC_SOLR_FIELDS));
            for (SolrDocument doc : docList) {
                terms.addAll(getSearchTerms(getAutoSuggestRegex(query), doc, Collections.singletonList(SolrConstants.UGCTERMS)));
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    /**
     * @param autoSuggestRegex
     * @param doc
     * @param fieldsToSearch
     * @return
     */
    private SearchTermList getSearchTerms(String regex, SolrDocument doc, List<String> fieldsToSearch) {
        SearchTermList terms = new SearchTermList();
        for (String field : fieldsToSearch) {
            String value = SolrSearchIndex.getSingleFieldStringValue(doc, field);
            terms.addAll(getSearchTerms(regex, value));
        }
        return terms;
    }

    /**
     * @param regex
     * @param terms
     * @param value
     */
    public SearchTermList getSearchTerms(String regex, String value) {
        SearchTermList terms = new SearchTermList();
        String wordRegex = getSingleWordRegex(regex);
        if (StringUtils.isNotBlank(value)) {
            Matcher matcher = Pattern.compile(wordRegex).matcher(value);
            while (matcher.find()) {
                String match = matcher.group(1);
                SearchTerm term = new SearchTerm(presentationBuilder.getSearchURI(getPi(), match, getMotivation()), match, 1);
                terms.add(term);
            }
        }
        return terms;
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

        //        QueryResponse response = DataManager.getInstance().getSearchIndex().search(queryBuilder.toString(), (page-1)*getHitsPerPage(), getHitsPerPage(), Collections.singletonList(sortField), null, FULLTEXTFIELDLIST);
        SolrDocumentList docList = DataManager.getInstance()
                .getSearchIndex()
                .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getPageSortFields(), FULLTEXTFIELDLIST);
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
                    .filter(l -> l.getContent().matches(getContainedWordRegex(query)))
                    .map(w -> (GeometricData) w)
                    .collect(Collectors.toList());
        }
        return words;
    }

    /**
     * @param query
     * @return a regex matching a single word matching the given query regex (ignoring case)
     */
    private String getSingleWordRegex(String query) {
        query = query.replace("(?i)", ""); //remove any possible ignore case flags
        return "(?i)(?:^|\\s+|[.:,;!?\\(\\)])(" + query + ")(?=$|\\s+|[.:,;!?\\(\\)])";
    }

    /**
     * 
     * @param query
     * @return a regex matching any text containing the given query regex as single word
     */
    private String getContainedWordRegex(String query) {
        query = query.replace("(?i)", ""); //remove any possible ignore case flags
        return "(?i)([\\w\\W]*)(^|.*\\s|[.:,;!?\\(\\)])(" + query + ")($|\\s.*|[.:,;!?\\(\\)])([\\w\\W]*)";
    }

    /**
     * @return a regex matching any word of the given query with '*' matching any number of word characters and ignoring case
     */
    private String getQueryRegex(String query) {
        query = query.replace("(?i)", ""); //remove any possible ignore case flags
        String queryRegex = query.replace("*", "[\\w\\d-]*").replaceAll("\\s+", "|");
        return "(?i)" + queryRegex;
    }

    /**
     * Create a regular expression matching all anything starting with the given query followed by an arbitrary number of word characters and ignoring
     * case
     * 
     * @param query
     * @return the regular expression {@code (?i){query}[\w\d-]*}
     */
    private String getAutoSuggestRegex(String query) {
        query = query.replace("(?i)", ""); //remove any possible ignore case flags
        String queryRegex = query + "[\\w\\d-]*";
        return "(?i)" + queryRegex;
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

    private static class SearchTermList extends ArrayList<SearchTerm> {

        public SearchTermList() {
            super();
        }

        /**
         * Adds the given term to the list if no term with the same {@link SearchTerm#getMatch()} exists. Otherwise add the
         * {@link SearchTerm#getCount()} of the given term to the existing term
         * 
         * @param term
         * @return true, even if the term already exists and its count is added to an existing term
         */
        @Override
        public boolean add(SearchTerm term) {
            int index = this.indexOf(term);
            if (index > -1) {
                this.get(index).incrementCount(term.getCount());
                return true;
            } else {
                return super.add(term);
            }
        }

        /* (non-Javadoc)
         * @see java.util.ArrayList#addAll(java.util.Collection)
         */
        @Override
        public boolean addAll(Collection<? extends SearchTerm> c) {
            for (SearchTerm searchTerm : c) {
                this.add(searchTerm);
            }
            return true;
        }
    }
}
